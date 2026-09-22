package eu.europa.ec.simpl.usersroles.controllers;

import static eu.europa.ec.simpl.common.exceptions.http.HttpExceptionBuilder.conflict;
import static eu.europa.ec.simpl.common.exceptions.http.HttpExceptionBuilder.notFound;

import eu.europa.ec.simpl.api.usersroles.v1.exchanges.UsersApi;
import eu.europa.ec.simpl.api.usersroles.v1.model.RoleDTO;
import eu.europa.ec.simpl.api.usersroles.v1.model.UserDTO;
import eu.europa.ec.simpl.usersroles.controllers.mappers.UserMapperV1;
import eu.europa.ec.simpl.usersroles.services.UserService;
import eu.europa.ec.simpl.usersroles.utils.JsonUtils;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequestMapping("v1")
@RequiredArgsConstructor
public class UserControllerV1 implements UsersApi {

    public static final String USER_WITH_ID_NOT_FOUND_MESSAGE = "User with id %s not found ";
    private final UserService userService;
    private final UserMapperV1 mapperV1;

    @Override
    public String createUser(UserDTO user) {
        log.info("Received internal POST request for user with email [{}]", user);
        var userOutcome = userService.createUser(mapperV1.toUser(user));

        return switch (userOutcome) {
            case UserService.CreateUserOutcome.Success outcome -> {
                log.info("Create user {}.", outcome.createdUser().getId());
                yield JsonUtils.toJsonString(outcome.createdUser().getId().toString());
            }
            case UserService.CreateUserOutcome.AlreadyExists alreadyExists -> {
                log.warn("User already exists with email {}", user.getEmail());
                throw conflict()
                        .withDetail("User already exists with email %s".formatted(user.getEmail()))
                        .build();
            }
        };
    }

    @Override
    public void deleteUser(String uuid) {
        log.info("Received DELETE request for user with uuid [{}]", uuid);
        switch (userService.deleteUser(UUID.fromString(uuid))) {
            case UserService.DeleteUserOutcome.Success outcome -> log.info("Delete user {}.", outcome.userId());
            case UserService.DeleteUserOutcome.NotFoundById ignored -> throw notFound()
                    .withDetail("User with uuid %s not found ".formatted(uuid))
                    .build();
            case UserService.DeleteUserOutcome.NotFoundByEmail ignored -> throw new IllegalStateException(
                    "Used id instead of email");
        }
    }

    @Override
    public List<RoleDTO> getRoles(String uuid) {
        log.info("Received GET request for roles of user with uuid [{}]", uuid);
        return switch (userService.getUserRoles(UUID.fromString(uuid))) {
            case UserService.GetUserRolesOutcome.Success outcome -> {
                log.info("Find roles {}.", outcome.roles());
                yield mapperV1.toRoleDTOList(outcome.roles());
            }
            case UserService.GetUserRolesOutcome.RolesNotFound rolesNotFound -> {
                log.warn(
                        "Error getting user's roles: some user roles were not found in persistence: {}",
                        rolesNotFound.rolesNotFond());
                throw notFound("Error getting user's roles: some user roles were not found in persistence: %s"
                        .formatted(rolesNotFound.rolesNotFond()));
            }
            case UserService.GetUserRolesOutcome.UserNotFound userNotFound -> throw notFound()
                    .withDetail(USER_WITH_ID_NOT_FOUND_MESSAGE.formatted(uuid))
                    .build();
        };
    }

    @Override
    public UserDTO getUserByUuid(String uuid) {
        log.info("Received GET request for user with uuid [{}]", uuid);
        UserService.GetUserOutcome getUserOutcome = userService.getUserById(UUID.fromString(uuid));
        return switch (getUserOutcome) {
            case UserService.GetUserOutcome.Success outcome -> {
                log.info("Get user {}.", outcome.user());
                yield mapperV1.toUserDTO(outcome.user());
            }
            case UserService.GetUserOutcome.NotFoundById ignored -> throw notFound()
                    .withDetail("User with uuid %s not found ".formatted(uuid))
                    .build();
            case UserService.GetUserOutcome.NotFoundByEmail notFoundByEmail -> throw new IllegalArgumentException(
                    "Used id instead of email");
        };
    }

    @Override
    public void importUsers(List<UserDTO> users) {
        log.info("Received POST request for importing [{}] users and relative roles", users.size());

        var outcomes = userService.importUsers(mapperV1.toUsers(users));
        // TODO handle failed imports
        log.info("Import users {}.", outcomes.size());
    }

    @Override
    public List<UserDTO> search(
            String username, String firstName, String lastName, String email, Integer first, Integer max) {
        var firstRes = first != null ? first : 0;
        var lastRes = max != null ? max : Long.MAX_VALUE;

        var userOutcome = userService.search(
                mapperV1.toKeycloakUserFilterNotNull(username, firstName, lastName, email, null), Pageable.unpaged());

        return switch (userOutcome) {
            case UserService.SearchUserOutcome.Success outcome -> {
                log.info("Search users {}.", outcome.users());
                yield mapperV1.toUserList(outcome.users().items().stream()
                        .skip(firstRes)
                        .limit(lastRes)
                        .toList());
            }
        };
    }

    @Override
    public void updateUser(String uuid, UserDTO user) {
        var userId = UUID.fromString(uuid);
        log.info("Received PUT request for user with id {}", uuid);
        switch (userService.updateUser(userId, mapperV1.toUser(user))) {
            case UserService.UpdateUserOutcome.Success ignored -> log.info("Update user {} with id {}.", user, userId);
            case UserService.UpdateUserOutcome.NotFound notFound -> throw notFound()
                    .withDetail(USER_WITH_ID_NOT_FOUND_MESSAGE.formatted(uuid))
                    .build();
            case UserService.UpdateUserOutcome.RolesDisabled rolesDisabled -> throw conflict()
                    .withDetail("Error: Some roles are disabled: %s".formatted(rolesDisabled.rolesDisabled()))
                    .build();
            case UserService.UpdateUserOutcome.RolesNotFound rolesNotFound -> throw notFound()
                    .withDetail("Error: Some roles are not found: %s".formatted(rolesNotFound.rolesNotFond()))
                    .build();
        }
    }

    @Override
    public void updateUserRoles(String uuid, List<String> roles) {
        var userId = UUID.fromString(uuid);
        log.info("Received PUT request for user-roles of user with id {}", userId);
        switch (userService.updateUserRoles(new UserService.UpdateUserRolesArgs.WithRoleNames(userId, roles))) {
            case UserService.UpdateUserOutcome.Success ignored -> log.info(
                    "Update user {} with roles {}.", userId, roles);
            case UserService.UpdateUserOutcome.NotFound notFound -> throw notFound()
                    .withDetail(USER_WITH_ID_NOT_FOUND_MESSAGE.formatted(uuid))
                    .build();
            case UserService.UpdateUserOutcome.RolesDisabled rolesDisabled -> throw conflict()
                    .withDetail("Error: Some roles are disabled: %s".formatted(rolesDisabled.rolesDisabled()))
                    .build();
            case UserService.UpdateUserOutcome.RolesNotFound rolesNotFound -> throw notFound()
                    .withDetail("Error: Some roles are not found: %s".formatted(rolesNotFound.rolesNotFond()))
                    .build();
        }
    }
}
