package eu.europa.ec.simpl.usersroles.controllers;

import static eu.europa.ec.simpl.common.exceptions.http.HttpExceptionBuilder.conflict;
import static eu.europa.ec.simpl.common.exceptions.http.HttpExceptionBuilder.notFound;
import static eu.europa.ec.simpl.common.exceptions.http.HttpExceptionBuilder.unauthorized;

import eu.europa.ec.simpl.api.usersroles.t1.v2.exchanges.UserSessionApi;
import eu.europa.ec.simpl.api.usersroles.t1.v2.exchanges.UsersApi;
import eu.europa.ec.simpl.api.usersroles.t1.v2.model.RoleAssignmentRequestDTO;
import eu.europa.ec.simpl.api.usersroles.t1.v2.model.RoleDTO;
import eu.europa.ec.simpl.api.usersroles.t1.v2.model.UpdateUserRequestDTO;
import eu.europa.ec.simpl.api.usersroles.t1.v2.model.UserDTO;
import eu.europa.ec.simpl.api.usersroles.t1.v2.model.UserSessionDataDTO;
import eu.europa.ec.simpl.api.usersroles.t1.v2.model.UsersPagedResponseDTO;
import eu.europa.ec.simpl.common.security.AuthService;
import eu.europa.ec.simpl.usersroles.controllers.mappers.RoleMapperTier1V2;
import eu.europa.ec.simpl.usersroles.controllers.mappers.UserMapperTier1V2;
import eu.europa.ec.simpl.usersroles.models.User;
import eu.europa.ec.simpl.usersroles.services.UserService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequestMapping("tier1/v2")
@RequiredArgsConstructor
public class UserControllerTier1V2 implements UsersApi, UserSessionApi {

    private static final String USER_NOT_FOUND_WITH_ID_MESSAGE = "User not found with id %s";
    private final UserService userService;
    private final UserMapperTier1V2 userMapper;
    private final RoleMapperTier1V2 roleMapper;
    private final PagedResourcesAssembler<User> pagedResourcesAssembler;
    private final AuthService authService;

    @Override
    public UserDTO createNewUser(UserDTO userDTO) {
        log.info("Received internal POST request for user with email [{}]", userDTO);
        var userOutcome = userService.createUser(userMapper.toUser(userDTO));

        return switch (userOutcome) {
            case UserService.CreateUserOutcome.Success outcome -> {
                log.info("Create user {}.", outcome.createdUser().getId());
                yield userMapper.toUserDTO(outcome.createdUser());
            }
            case UserService.CreateUserOutcome.AlreadyExists alreadyExists -> {
                logUserAlreadyExists(alreadyExists);
                throw conflict()
                        .withDetail("User already exists with email %s".formatted(alreadyExists.email()))
                        .build();
            }
        };
    }

    private static void logUserAlreadyExists(UserService.CreateUserOutcome.AlreadyExists alreadyExists) {
        log.warn("User already exists with email {}", alreadyExists.email());
    }

    @Override
    public void deleteUserById(UUID userId) {
        switch (userService.deleteUser(userId)) {
            case UserService.DeleteUserOutcome.NotFoundByEmail notFoundByEmail -> throw new IllegalStateException(
                    "Used id instead of email");
            case UserService.DeleteUserOutcome.NotFoundById notFoundById -> {
                log.warn(USER_NOT_FOUND_WITH_ID_MESSAGE.formatted(userId));
                throw notFound(USER_NOT_FOUND_WITH_ID_MESSAGE.formatted(userId));
            }
            case UserService.DeleteUserOutcome.Success success -> log.info(
                    "Deleted user {} successfully", success.userId());
        }
    }

    @Override
    public UserDTO getUserById(UUID userId) {
        return switch (userService.getUserById(userId)) {
            case UserService.GetUserOutcome.NotFoundByEmail notFoundByEmail -> throw new IllegalStateException(
                    "Used id instead of email");
            case UserService.GetUserOutcome.NotFoundById notFoundById -> {
                log.warn(USER_NOT_FOUND_WITH_ID_MESSAGE.formatted(userId));
                throw notFound(USER_NOT_FOUND_WITH_ID_MESSAGE.formatted(userId));
            }
            case UserService.GetUserOutcome.Success success -> userMapper.toUserDTO(success.user());
        };
    }

    @Override
    public List<RoleDTO> getUserRoles(UUID userId) {
        return switch (userService.getUserRoles(userId)) {
            case UserService.GetUserRolesOutcome.UserNotFound notFound -> {
                log.warn(USER_NOT_FOUND_WITH_ID_MESSAGE.formatted(userId));
                throw notFound(USER_NOT_FOUND_WITH_ID_MESSAGE.formatted(userId));
            }
            case UserService.GetUserRolesOutcome.RolesNotFound rolesNotFound -> {
                log.warn(
                        "Error getting user's roles: some user roles were not found in persistence: {}",
                        rolesNotFound.rolesNotFond());
                throw notFound("Error getting user's roles: some user roles were not found in persistence: %s"
                        .formatted(rolesNotFound.rolesNotFond()));
            }
            case UserService.GetUserRolesOutcome.Success success -> roleMapper.toRoleDTOList(success.roles());
        };
    }

    @Override
    public void importUsers(List<UserDTO> userList) {
        var outcomes = userService.importUsers(userMapper.toUserList(userList));
        log.info("Import users {}", outcomes.size());
        for (var outcome : outcomes) {
            switch (outcome) {
                case UserService.CreateUserOutcome.AlreadyExists alreadyExists -> logUserAlreadyExists(alreadyExists);
                case UserService.CreateUserOutcome.Success success -> log.info(
                        "Imported user {}", success.createdUser().getId());
            }
        }
    }

    @Override
    public UsersPagedResponseDTO searchUsers(
            Integer page,
            Integer pageSize,
            String firstName,
            String lastName,
            String username,
            String email,
            Boolean enabled,
            Boolean federated,
            Boolean unpaged) {
        Pageable pageable = null;
        if (BooleanUtils.isTrue(unpaged)) {
            pageable = Pageable.unpaged();
        } else {
            pageable = Pageable.ofSize(pageSize).withPage(page);
        }
        var filter = userMapper.toKeycloakUserFilter(firstName, lastName, username, email, enabled);
        return switch (userService.search(filter, pageable)) {
            case UserService.SearchUserOutcome.Success success -> {
                var pageModel = pagedResourcesAssembler.toModel(new PageImpl<>(
                        success.users().items(),
                        success.users().pageable(),
                        success.users().total()));
                var userPagedResponseDTO = userMapper.toUsersPagedResponseDTO(pageModel);
                userMapper.toUsersPagedResponseDTO(userPagedResponseDTO, success.users());
                yield userPagedResponseDTO;
            }
        };
    }

    @Override
    public void updateUserById(UUID userId, UpdateUserRequestDTO updateUserRequestDTO) {
        switch (userService.updateUser(userId, userMapper.toUpdateUserRequestDTO(updateUserRequestDTO))) {
            case UserService.UpdateUserOutcome.NotFound notFound -> {
                log.warn(USER_NOT_FOUND_WITH_ID_MESSAGE.formatted(userId));
                throw notFound(USER_NOT_FOUND_WITH_ID_MESSAGE.formatted(userId));
            }
            case UserService.UpdateUserOutcome.RolesDisabled rolesDisabled -> throw conflict()
                    .withDetail("Error: Some roles are disabled: %s".formatted(rolesDisabled.rolesDisabled()))
                    .build();
            case UserService.UpdateUserOutcome.RolesNotFound rolesNotFound -> throw notFound()
                    .withDetail("Error: Some roles are not found: %s".formatted(rolesNotFound.rolesNotFond()))
                    .build();
            case UserService.UpdateUserOutcome.Success success -> log.info("User {} updated successfully", userId);
        }
    }

    @Override
    public void updateUserRoles(UUID userId, RoleAssignmentRequestDTO roleAssignmentRequestDTO) {
        switch (userService.updateUserRoles(
                new UserService.UpdateUserRolesArgs.WithRoleIds(userId, roleAssignmentRequestDTO.getRolesIds()))) {
            case UserService.UpdateUserOutcome.NotFound notFound -> {
                log.warn(USER_NOT_FOUND_WITH_ID_MESSAGE.formatted(userId));
                throw notFound(USER_NOT_FOUND_WITH_ID_MESSAGE.formatted(userId));
            }
            case UserService.UpdateUserOutcome.RolesDisabled rolesDisabled -> throw conflict()
                    .withDetail("Error: Some roles are disabled: %s".formatted(rolesDisabled.rolesDisabled()))
                    .build();
            case UserService.UpdateUserOutcome.RolesNotFound rolesNotFound -> throw notFound()
                    .withDetail("Error: Some roles are not found: %s".formatted(rolesNotFound.rolesNotFond()))
                    .build();
            case UserService.UpdateUserOutcome.Success success -> {
                log.info("User {} roles updated successfully", userId);
            }
        }
    }

    @Override
    public UserSessionDataDTO getUserSessionData() {
        if (!authService.isAuthenticated()) {
            log.warn("Attempt to retrieve session data for an unauthenticated user.");
            throw unauthorized("User is not authenticated");
        }
        log.info("Retrieving session data for the authenticated user.");
        return userMapper.toUserSessionDataDTO(authService);
    }
}
