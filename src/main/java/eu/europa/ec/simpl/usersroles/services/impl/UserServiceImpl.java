package eu.europa.ec.simpl.usersroles.services.impl;

import eu.europa.ec.simpl.common.security.JwtService;
import eu.europa.ec.simpl.usersroles.adapters.UserAdapter;
import eu.europa.ec.simpl.usersroles.exceptions.KeycloakException;
import eu.europa.ec.simpl.usersroles.models.Role;
import eu.europa.ec.simpl.usersroles.models.User;
import eu.europa.ec.simpl.usersroles.models.UserFilter;
import eu.europa.ec.simpl.usersroles.services.RoleService;
import eu.europa.ec.simpl.usersroles.services.UserService;
import eu.europa.ec.simpl.validation.CreateOperation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.ClientErrorException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final JwtService jwtService;
    private final UserAdapter userAdapter;
    private final RoleService roleService;

    @Override
    public DeleteUserOutcome deleteByEmail(@Email @NotBlank String email) {
        log.info("Delete user by email {}", email);
        var user = getUserByEmail(email);
        return switch (user) {
            case GetUserOutcome.Success outcome -> {
                log.info("Get user by email {} return user {}", email, outcome.user());
                userAdapter.deleteUser(outcome.user().getId());
                yield new DeleteUserOutcome.Success(outcome.user().getId());
            }
            case GetUserOutcome.NotFoundByEmail ignored -> {
                log.warn("User by email {} does not exist", email);
                yield new DeleteUserOutcome.NotFoundByEmail(email);
            }
            case GetUserOutcome.NotFoundById notFoundById -> throw new IllegalStateException(
                    "Used email instead of id");
        };
    }

    @Override
    @Validated(CreateOperation.class)
    public CreateUserOutcome createUser(@Valid @NotNull User user) {
        log.info("Creating user {}", user);
        var createdUser = userAdapter.createUser(new UserAdapter.CreateUserArgs.Default(user));
        return switch (createdUser) {
            case UserAdapter.CreateUserOutcome.Success outcome -> new CreateUserOutcome.Success(outcome.user());
            case UserAdapter.CreateUserOutcome.AlreadyExists alreadyExists -> new CreateUserOutcome.AlreadyExists(
                    user.getEmail());
        };
    }

    @Override
    public GetUserOutcome getUserByEmail(@Email @NotBlank String email) {
        log.info("Finding user with email {}", email);
        var user = userAdapter.getUserByEmail(email);
        if (user.isEmpty()) {
            log.warn("User with email {} not found.", email);
            return new GetUserOutcome.NotFoundByEmail(email);
        }

        user.get()
                .setRoles(getPersistedUserRoles(user.get()).stream()
                        .map(Role::getCode)
                        .toList());

        log.info("Found user with email {}", email);
        return new GetUserOutcome.Success(user.get());
    }

    @Override
    public Collection<CreateUserOutcome> importUsers(@Valid @NotNull List<User> users) {
        Collection<CreateUserOutcome> results = new ArrayList<>(users.size());
        for (var user : users) {
            try {
                var createUserOutcome = this.importUser(user);
                results.add(createUserOutcome);
            } catch (KeycloakException e) {
                log.error("User import failure {}", user.getEmail(), e);
                return results;
            }
        }
        log.info("Imported all users {}", users);
        return results;
    }

    @Override
    public CreateUserOutcome importUser(@Valid @NotNull User user) {
        log.info("importUser: importing user [{}]", user.getUsername());
        var userOutcome = createUser(user);
        return switch (userOutcome) {
            case CreateUserOutcome.Success outcome -> {
                log.info("Imported user {}", user);
                yield outcome;
            }
            case CreateUserOutcome.AlreadyExists alreadyExists -> {
                log.warn("user already exists with username {}", user.getUsername());
                yield alreadyExists;
            }
        };
    }

    @Override
    public GetUserRolesOutcome getUserRoles(@NotBlank UUID uuid) {
        log.info("Getting user roles for user {}", uuid);
        var user = userAdapter.getUser(uuid);
        if (user.isEmpty()) {
            log.warn("User not found with id {}", uuid);
            return new GetUserRolesOutcome.UserNotFound(uuid);
        }

        var getRolesOutcome = roleService.findRolesBy(
                new RoleService.FindRolesByArgs.FindByCodes(user.get().getRoles()));

        switch (getRolesOutcome) {
            case RoleService.FindRolesByOutcome.Success success -> {
                log.info("Found user with roles {}", success.roles());
                return new GetUserRolesOutcome.Success(success.roles());
            }
            case RoleService.FindRolesByOutcome.NotFound notFound -> {
                return new GetUserRolesOutcome.RolesNotFound(notFound.notFoundRoles());
            }
        }
    }

    @Override
    public GetUserOutcome getUserById(@NotBlank UUID userId) {
        log.info("Getting user by id {}", userId);
        var user = userAdapter.getUser(userId);
        if (user.isEmpty()) {
            logUserNotFound(userId);
            return new GetUserOutcome.NotFoundById(userId);
        }

        user.get()
                .setRoles(getPersistedUserRoles(user.get()).stream()
                        .map(Role::getCode)
                        .toList());

        log.info("Found user with id {}", userId);
        return new GetUserOutcome.Success(user.get());
    }

    private static void logUserNotFound(UUID userId) {
        log.warn("User with id {} not found", userId);
    }

    @Override
    public UpdateUserOutcome updateUser(@NotBlank UUID userId, @Valid @NotNull User userDTO) {
        var user = userAdapter.getUser(userId);
        if (user.isEmpty()) {
            logUserNotFound(userId);
            return new UpdateUserOutcome.NotFound(userId);
        }
        var updateRolesOutcome = updateUserRoles(new UpdateUserRolesArgs.WithRoleNames(userId, userDTO.getRoles()));

        if (updateRolesOutcome instanceof UpdateUserOutcome.Success) {
            userAdapter.updateUser(userId, userDTO);
        }

        return updateRolesOutcome;
    }

    @Override
    public DeleteUserOutcome deleteUser(@NotNull UUID userId) {
        log.info("Deleting user {}", userId);
        var user = userAdapter.getUser(userId);
        if (user.isEmpty()) {
            logUserNotFound(userId);
            return new DeleteUserOutcome.NotFoundById(userId);
        }

        userAdapter.deleteUser(userId);

        return new DeleteUserOutcome.Success(userId);
    }

    @Override
    public UpdateUserOutcome updateUserRoles(UpdateUserRolesArgs args) {

        log.info("Updating roles for user with id {}", args.userId());

        RoleService.RolesCheckResult checkResult;

        var adapterArgs =
                switch (args) {
                    case UpdateUserRolesArgs.WithRoleIds withRoleIds -> {
                        checkResult = checkMissingDisabledRoles(
                                new RoleService.FindRolesByArgs.FindByIds(withRoleIds.roles()));

                        yield new UserAdapter.ReplaceUserRolesArgs.WithRoleIds(
                                withRoleIds.userId(), withRoleIds.roles());
                    }
                    case UpdateUserRolesArgs.WithRoleNames withRoleNames -> {
                        checkResult = checkMissingDisabledRoles(
                                new RoleService.FindRolesByArgs.FindByCodes(withRoleNames.roles()));

                        yield new UserAdapter.ReplaceUserRolesArgs.WithRoleNames(
                                withRoleNames.userId(), withRoleNames.roles());
                    }
                };

        if (!checkResult.notFound().isEmpty()) {
            log.warn(
                    "Error assigning roles to user {} - some roles are not found: {}",
                    args.userId(),
                    checkResult.notFound());
            return new UpdateUserOutcome.RolesNotFound(checkResult.notFound());
        }

        if (!checkResult.disabled().isEmpty()) {
            log.warn(
                    "Error assigning roles to user {} - some roles are disabled: {}",
                    args.userId(),
                    checkResult.disabled());
            return new UpdateUserOutcome.RolesDisabled(checkResult.disabled());
        }

        return switch (userAdapter.replaceUserRoles(adapterArgs)) {
            case UserAdapter.ReplaceUserRolesOutcome.Success success -> {
                log.info("Updated roles for user with id {}", args.userId());
                yield new UpdateUserOutcome.Success();
            }
        };
    }

    @Override
    public SearchUserOutcome search(@Valid @NotNull UserFilter filter, Pageable pageable) {
        log.info("Searching users with filter: {}", filter);

        try {
            var users = userAdapter.getUsers(filter, pageable);
            log.info("Search users result is {}", users);
            return new SearchUserOutcome.Success(users);
        } catch (ClientErrorException e) {
            log.error("search: error searching user with filter [{}] from realm", filter);
            throw new KeycloakException(e.getResponse(), e);
        }
    }

    @Override
    public LogoutOutcome logout() {
        var sid = jwtService.getClaim("sid");
        log.debug("Logout for sid {}", sid);
        userAdapter.logout(new UserAdapter.LogoutArgs.Default(sid));
        log.debug("Logout successful for sid {}", sid);
        return new LogoutOutcome.Success();
    }

    private RoleService.RolesCheckResult checkMissingDisabledRoles(RoleService.FindRolesByArgs findArgs) {
        switch (roleService.findRolesBy(findArgs)) {
            case RoleService.FindRolesByOutcome.NotFound notFound -> {
                return new RoleService.RolesCheckResult(notFound.notFoundRoles(), Collections.emptyList());
            }
            case RoleService.FindRolesByOutcome.Success success -> {
                var disabled = success.roles().stream()
                        .filter(r -> Boolean.FALSE.equals(r.getEnabled()))
                        .toList();
                return new RoleService.RolesCheckResult(Collections.emptyList(), disabled);
            }
        }
    }

    private List<Role> getPersistedUserRoles(User user) {

        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return Collections.emptyList();
        }

        var getRolesOutcome = roleService.findRolesBy(new RoleService.FindRolesByArgs.FindByCodes(user.getRoles()));

        switch (getRolesOutcome) {
            case RoleService.FindRolesByOutcome.Success success -> {
                return success.roles();
            }
            case RoleService.FindRolesByOutcome.NotFound notFound -> {
                log.warn("Error finding roles of user %s, some roles were not found in persistence: %s"
                        .formatted(user.getId(), notFound.notFoundRoles()));
                return notFound.foundRoles();
            }
        }
    }
}
