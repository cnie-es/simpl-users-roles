package eu.europa.ec.simpl.usersroles.adapters.impl;

import eu.europa.ec.simpl.usersroles.adapters.RoleAdapter;
import eu.europa.ec.simpl.usersroles.adapters.UserAdapter;
import eu.europa.ec.simpl.usersroles.adapters.mappers.KeycloakMapper;
import eu.europa.ec.simpl.usersroles.exceptions.KeycloakException;
import eu.europa.ec.simpl.usersroles.models.Role;
import eu.europa.ec.simpl.usersroles.models.User;
import eu.europa.ec.simpl.usersroles.models.UserFilter;
import eu.europa.ec.simpl.usersroles.models.UserPage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * An outbound adapter for Keycloak http operations
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Log4j2
public class UserAdapterImpl implements UserAdapter {

    private static final String USER_ID_GROUP = "userId";
    private static final Pattern LAST_PATH_LOCATION_PATTERN =
            Pattern.compile("\\/(?<%s>[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})"
                    .formatted(USER_ID_GROUP));
    private static final String SERVICE_ACCOUNT_PREFIX = "service-account-";

    RealmResource realm;
    KeycloakMapper keycloakMapper;
    RoleAdapter roleAdapter;

    @Override
    public Optional<User> getUser(UUID userId) {
        try {
            log.info("Getting user with id {}", userId);
            var representation = realm.users().get(userId.toString()).toRepresentation();
            var roleList = realm.users().get(representation.getId()).roles().realmLevel().listAll().stream()
                    .map(RoleRepresentation::getName)
                    .toList();
            log.info("User found with id: {}", userId);
            return Optional.of(keycloakMapper.toDto(representation, roleList));
        } catch (ClientErrorException e) {
            log.warn("User not found with id {}", userId, e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<User> getUserByEmail(String email) {
        var userRepresentations = realm.users().searchByEmail(email, Boolean.TRUE);

        if (userRepresentations.isEmpty()) {
            log.warn("User does not exist searching by email {}", email);
            return Optional.empty();
        }

        var userRepresentation = userRepresentations.getFirst();
        var roleList = realm.users().get(userRepresentation.getId()).roles().realmLevel().listAll().stream()
                .map(RoleRepresentation::getName)
                .toList();
        return Optional.of(keycloakMapper.toDto(userRepresentation, roleList));
    }

    @Override
    public CreateUserOutcome createUser(CreateUserArgs args) {
        var user = args.user();
        log.info("Creating user {}", user.getUsername());
        var userRepresentation = keycloakMapper.toRepresentation(user);

        try (var response = realm.users().create(userRepresentation)) {
            return switch (HttpStatus.valueOf(response.getStatus())) {
                case CONFLICT -> {
                    log.warn("User already exists {}", userRepresentation);
                    yield new CreateUserOutcome.AlreadyExists();
                }
                case CREATED -> {
                    var headerString = response.getHeaderString(HttpHeaders.LOCATION);
                    log.debug("User attributes [{}]", userRepresentation.getRawAttributes());
                    log.debug("Getting user id from location {} ", headerString);
                    var userId = getUserIdFromLocation(headerString);
                    if (userId.isPresent()) {
                        yield onCreatedUser(userId.get(), user);
                    } else {
                        log.error("User id can't be parsed from location header {}", headerString);
                        throw new KeycloakException(response);
                    }
                }
                default -> {
                    log.error("Error creating user with username {}", user.getUsername());
                    throw new KeycloakException(response);
                }
            };
        }
    }

    private CreateUserOutcome onCreatedUser(UUID userId, User user) {
        log.debug("User {} created with id {}", user.getUsername(), userId);
        if (CollectionUtils.isNotEmpty(user.getRoles())) {
            log.debug("Assigning roles {} to user {}", user.getRoles(), userId);
            replaceUserRoles(new ReplaceUserRolesArgs.WithRoleNames(userId, user.getRoles()));
        }
        if (user.getPassword() == null) {
            log.debug("No password set for user {}, sending UPDATE_PASSWORD action email", userId);
            realm.users().get(userId.toString()).executeActionsEmail(List.of("UPDATE_PASSWORD"));
        }
        var createdUser = getUser(userId).orElseThrow(() -> new IllegalStateException("User not found after creation"));
        return new CreateUserOutcome.Success(createdUser);
    }

    private static Optional<UUID> getUserIdFromLocation(@NotBlank String location) {
        log.info("Get userId from location {} ", location);
        var matcher = LAST_PATH_LOCATION_PATTERN.matcher(location);
        if (!matcher.find()) {
            log.error("getUserIdFromLocation: error getting user id from location user with location [{}]", location);
            return Optional.empty();
        }
        var match = matcher.group(USER_ID_GROUP);
        log.info("Got userId from location {} [{}]", location, match);
        return Optional.of(UUID.fromString(match));
    }

    @Override
    public void deleteUser(UUID userId) {
        log.info("Deleting user with id {}", userId);
        try (var response = realm.users().delete(userId.toString())) {
            if (response.getStatus() != Response.Status.NO_CONTENT.getStatusCode()) {
                log.error("error deleting user {}, http status {}", userId, response.getStatus());
                throw new KeycloakException(response);
            }
        } catch (ClientErrorException e) {
            log.error("deleteUser: error deleting user {}", userId);
            throw new KeycloakException(e.getResponse(), e);
        }
    }

    @Override
    public UserPage getUsers(UserFilter filter, Pageable pageable) {
        return getUsersBatch(filter, pageable);
    }

    private UserPage getUsersBatch(UserFilter filter, Pageable pageable) {

        if (pageable.isUnpaged()) {
            return getUsersUnpaged(filter, pageable);
        }

        final int pageSize = pageable.getPageSize();
        final int targetOffset = Math.toIntExact(pageable.getOffset());
        final int batchSize = Math.max(pageSize * 5, 200);

        int kcFirst = 0;

        var state = new BatchState(targetOffset, pageSize);

        while (!state.isDone()) {

            var batch = realm.users()
                    .search(
                            filter.getUsername(),
                            filter.getFirstName(),
                            filter.getLastName(),
                            filter.getEmail(),
                            kcFirst,
                            batchSize,
                            filter.getEnabled(),
                            Boolean.FALSE,
                            Boolean.FALSE);

            state = consumeBatch(batch, state);
            kcFirst += batchSize;
        }

        return new UserPage(
                pageSize,
                pageable.getPageNumber(),
                countTotalUsers(filter),
                keycloakMapper.toUserList(state.pageItems()),
                pageable);
    }

    private BatchState consumeBatch(List<UserRepresentation> batch, BatchState state) {
        if (batch == null || batch.isEmpty()) {
            return new BatchState(state.targetOffset(), state.pageSize, state.skippedValid(), state.pageItems(), true);
        }

        int skippedValid = state.skippedValid();
        List<UserRepresentation> pageItems = state.pageItems();
        int targetOffset = state.targetOffset();
        int pageSize = state.pageSize();

        for (UserRepresentation u : batch) {
            if (u.getServiceAccountClientId() == null && !u.getUsername().startsWith(SERVICE_ACCOUNT_PREFIX)) {
                if (skippedValid < targetOffset) {
                    skippedValid++;
                } else {
                    pageItems.add(u);
                }
            }

            if (pageItems.size() >= pageSize) {
                return new BatchState(targetOffset, pageSize, skippedValid, pageItems, true);
            }
        }

        return new BatchState(targetOffset, pageSize, skippedValid, pageItems, false);
    }

    private UserPage getUsersUnpaged(UserFilter filter, Pageable pageable) {

        var unpagedFilteredUsers = realm
                .users()
                .search(
                        filter.getUsername(),
                        filter.getFirstName(),
                        filter.getLastName(),
                        filter.getEmail(),
                        null,
                        null,
                        filter.getEnabled(),
                        Boolean.FALSE,
                        Boolean.FALSE)
                .stream()
                .filter(u -> u.getServiceAccountClientId() == null
                        && !u.getUsername().startsWith(SERVICE_ACCOUNT_PREFIX))
                .toList();

        return new UserPage(
                null, null, countTotalUsers(filter), keycloakMapper.toUserList(unpagedFilteredUsers), pageable);
    }

    private long countTotalUsers(UserFilter filter) {

        return realm
                .users()
                .search(
                        filter.getUsername(),
                        filter.getFirstName(),
                        filter.getLastName(),
                        filter.getEmail(),
                        null,
                        null,
                        filter.getEnabled(),
                        Boolean.FALSE,
                        Boolean.FALSE)
                .stream()
                .filter(u -> u.getServiceAccountClientId() == null
                        && !u.getUsername().startsWith(SERVICE_ACCOUNT_PREFIX))
                .count();
    }

    @Override
    public ReplaceUserRolesOutcome replaceUserRoles(ReplaceUserRolesArgs args) {
        var userId = args.userId();

        try {
            var user = realm.users().get(userId.toString());
            Collection<RoleRepresentation> validRoles = new ArrayList<>();

            switch (args) {
                case ReplaceUserRolesArgs.WithRoleIds withRoleIds -> {
                    for (var roleId : withRoleIds.roles()) {
                        var getRoleOpt = roleAdapter.getRoleById(roleId);
                        getRoleOpt.ifPresent(value -> validRoles.add(keycloakMapper.toRepresentation(value)));
                    }
                }
                case ReplaceUserRolesArgs.WithRoleNames withRoleNames -> {
                    for (var roleName : withRoleNames.roles()) {
                        var getRoleOpt = roleAdapter.getRoleByName(roleName);
                        getRoleOpt.ifPresent(value -> validRoles.add(keycloakMapper.toRepresentation(value)));
                    }
                }
            }

            var allUserRoles = user.roles().realmLevel().listAll();
            var rolesToAdd = validRoles.stream()
                    .filter(newRole -> !allUserRoles.contains(newRole))
                    .toList();
            var rolesToRemove = allUserRoles.stream()
                    .filter(existingRole -> !validRoles.contains(existingRole))
                    .toList();
            log.info("addRolesToUser: current roles {} of user {}", allUserRoles, userId);
            log.info("addRolesToUser: adding roles {} to user {}", rolesToAdd, userId);
            log.info("addRolesToUser: removing roles {} to user {}", rolesToRemove, userId);
            user.roles().realmLevel().add(rolesToAdd);
            user.roles().realmLevel().remove(rolesToRemove);
            return new ReplaceUserRolesOutcome.Success();
        } catch (ClientErrorException e) {
            log.error("addRolesToUser: error updating roles of user {}", userId, e);
            throw new KeycloakException(e.getResponse(), e);
        }
    }

    @Override
    public LogoutOutcome logout(LogoutArgs args) {
        try {
            realm.deleteSession(args.sid(), false);
            log.info("Delete session {}", args.sid());
            return new LogoutOutcome.Success();
        } catch (ClientErrorException e) {
            log.error("Logout receive a key cloak failure ", e);
            throw new KeycloakException(e.getResponse(), e);
        }
    }

    @Override
    public List<Role> getUserRoles(UUID userId) {
        try {
            var userRoles =
                    realm.users().get(userId.toString()).roles().realmLevel().listAll();
            return keycloakMapper.toRoleList(userRoles);
        } catch (ClientErrorException e) {
            log.error(
                    "User not found with id {} from realm {}",
                    userId,
                    realm.toRepresentation().getDisplayName());
            throw new KeycloakException(e.getResponse(), e);
        }
    }

    @Override
    public void updateUser(@NotBlank UUID uuid, @Valid @NotNull User user) {
        log.info("Updating user {}, {}", uuid, user);
        try {
            var userResource = realm.users().get(uuid.toString());
            var representation = userResource.toRepresentation();
            keycloakMapper.updateEntity(user, representation);
            userResource.update(representation);
            log.info("Updater user {}", representation);
        } catch (ClientErrorException e) {
            log.error("updateUser: error updating user [{}, {}] from realm", uuid, user);
            throw new KeycloakException(e.getResponse(), e);
        }
    }

    private record BatchState(
            int targetOffset, int pageSize, int skippedValid, List<UserRepresentation> pageItems, boolean done) {
        BatchState(int targetOffset, int pageSize) {
            this(targetOffset, pageSize, 0, new ArrayList<>(pageSize), false);
        }

        boolean isDone() {
            return done;
        }
    }
}
