package eu.europa.ec.simpl.usersroles.controllers;

import static eu.europa.ec.simpl.common.test.TestUtil.a;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.europa.ec.simpl.api.usersroles.v1.model.UserDTO;
import eu.europa.ec.simpl.common.exceptions.http.HttpResponseException;
import eu.europa.ec.simpl.common.test.TestUtil;
import eu.europa.ec.simpl.usersroles.controllers.mappers.UserMapperV1Impl;
import eu.europa.ec.simpl.usersroles.models.Role;
import eu.europa.ec.simpl.usersroles.models.User;
import eu.europa.ec.simpl.usersroles.models.UserFilter;
import eu.europa.ec.simpl.usersroles.models.UserPage;
import eu.europa.ec.simpl.usersroles.services.RoleService;
import eu.europa.ec.simpl.usersroles.services.UserService;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class UserControllerV1Test {

    private static final String TEST_UUID = "dd8dea83-5123-408d-bcbd-2bd7c11cfb2b";

    @Spy
    private UserMapperV1Impl userMapperV1;

    @InjectMocks
    private UserControllerV1 userControllerV1;

    @Mock
    private UserService userService;

    @Test
    void createUserTest() {

        var user = a(User.class);
        var userDTO = a(UserDTO.class).setId(null);

        when(userService.createUser(any())).thenReturn(new UserService.CreateUserOutcome.Success(user));

        var responseJson = userControllerV1.createUser(userDTO);

        assertThat(responseJson).isNotNull();
        BDDMockito.then(userService).should().createUser(any());
    }

    @Test
    void getRoles() {
        var userId = UUID.fromString(TEST_UUID);
        var role = a(Role.class);
        when(userService.getUserRoles(userId)).thenReturn(new UserService.GetUserRolesOutcome.Success(List.of(role)));
        userControllerV1.getRoles(userId.toString());
        verify(userService).getUserRoles(userId);
    }

    @Test
    void getRoles_roles_not_found() {
        var userId = UUID.fromString(TEST_UUID);
        when(userService.getUserRoles(userId))
                .thenReturn(new UserService.GetUserRolesOutcome.RolesNotFound(
                        List.of(new RoleService.RoleIdentifier.Code("role-code"))));
        assertThatThrownBy(() -> userControllerV1.getRoles(userId.toString()))
                .isInstanceOfSatisfying(HttpResponseException.class, ex -> Assertions.assertThat(ex.getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
        verify(userService).getUserRoles(userId);
    }

    @Test
    void getUserByUuid() {
        var userId = UUID.fromString(TEST_UUID);
        var userDTO = a(UserDTO.class);
        userDTO.setId(userId.toString());
        var user = userMapperV1.toUser(userDTO);
        when(userService.getUserById(userId)).thenReturn(new UserService.GetUserOutcome.Success(user));

        userControllerV1.getUserByUuid(userId.toString());
        verify(userService).getUserById(userId);
    }

    @Test
    void updateUser() {
        var userId = UUID.fromString(TEST_UUID);
        var keycloakUserDTO = a(UserDTO.class).setId(userId.toString());
        var user = userMapperV1.toUser(keycloakUserDTO);
        when(userService.updateUser(userId, user)).thenReturn(new UserService.UpdateUserOutcome.Success());
        userControllerV1.updateUser(userId.toString(), keycloakUserDTO);
        verify(userService).updateUser(userId, user);
    }

    @Test
    void updateUser_rolesNotFond() {
        var userId = UUID.fromString(TEST_UUID);
        var keycloakUserDTO = a(UserDTO.class).setId(userId.toString());
        var user = userMapperV1.toUser(keycloakUserDTO);
        when(userService.updateUser(userId, user))
                .thenReturn(new UserService.UpdateUserOutcome.RolesNotFound(
                        List.of(new RoleService.RoleIdentifier.Code("CODE"))));

        assertThatThrownBy(() -> userControllerV1.updateUser(userId.toString(), keycloakUserDTO))
                .isInstanceOfSatisfying(HttpResponseException.class, ex -> Assertions.assertThat(ex.getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));

        verify(userService).updateUser(userId, user);
    }

    @Test
    void updateUser_rolesDisabled() {
        var userId = UUID.fromString(TEST_UUID);
        var keycloakUserDTO = a(UserDTO.class).setId(userId.toString());
        var user = userMapperV1.toUser(keycloakUserDTO);

        var roleDisabled = a(Role.class).setEnabled(false);

        when(userService.updateUser(userId, user))
                .thenReturn(new UserService.UpdateUserOutcome.RolesDisabled(List.of(roleDisabled)));

        assertThatThrownBy(() -> userControllerV1.updateUser(userId.toString(), keycloakUserDTO))
                .isInstanceOfSatisfying(HttpResponseException.class, ex -> Assertions.assertThat(ex.getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));

        verify(userService).updateUser(userId, user);
    }

    @Test
    void updateUserRoles() {
        var userId = UUID.fromString(TEST_UUID);
        var userRolesElement = a(String.class);
        var userRoles = List.of(userRolesElement);

        when(userService.updateUserRoles(new UserService.UpdateUserRolesArgs.WithRoleNames(userId, userRoles)))
                .thenReturn(new UserService.UpdateUserOutcome.Success());
        userControllerV1.updateUserRoles(userId.toString(), userRoles);
        verify(userService).updateUserRoles(any(UserService.UpdateUserRolesArgs.WithRoleNames.class));
    }

    @Test
    void updateUserRoles_rolesNotFond() {

        var userId = UUID.fromString(TEST_UUID);
        var userRolesElement = a(String.class);
        var userRoles = List.of(userRolesElement);

        given(userService.updateUserRoles(any(UserService.UpdateUserRolesArgs.WithRoleNames.class)))
                .willReturn(new UserService.UpdateUserOutcome.RolesNotFound(
                        List.of(new RoleService.RoleIdentifier.Code("CODE"))));

        assertThatThrownBy(() -> userControllerV1.updateUserRoles(userId.toString(), userRoles))
                .isInstanceOfSatisfying(HttpResponseException.class, ex -> Assertions.assertThat(ex.getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void updateUserRoles_rolesDisabled() {

        var userId = UUID.fromString(TEST_UUID);
        var userRolesElement = a(String.class);
        var userRoles = List.of(userRolesElement);

        var roleDisabled = a(Role.class).setEnabled(false);

        given(userService.updateUserRoles(any(UserService.UpdateUserRolesArgs.WithRoleNames.class)))
                .willReturn(new UserService.UpdateUserOutcome.RolesDisabled(List.of(roleDisabled)));

        assertThatThrownBy(() -> userControllerV1.updateUserRoles(userId.toString(), userRoles))
                .isInstanceOfSatisfying(HttpResponseException.class, ex -> Assertions.assertThat(ex.getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void deleteUser() {
        UUID userId = UUID.fromString(TEST_UUID);
        when(userService.deleteUser(userId)).thenReturn(new UserService.DeleteUserOutcome.Success(userId));
        userControllerV1.deleteUser(userId.toString());
        verify(userService).deleteUser(userId);
    }

    @Test
    void search() {
        var keycloakUserFilter = a(UserFilter.class);

        var users = TestUtil.aListOf(User.class);

        var userPage = new UserPage(0, 0, users.size(), users, Pageable.unpaged());

        when(userService.search(any(UserFilter.class), any()))
                .thenReturn(new UserService.SearchUserOutcome.Success(userPage));

        userControllerV1.search(
                keycloakUserFilter.getUsername(),
                keycloakUserFilter.getFirstName(),
                keycloakUserFilter.getLastName(),
                keycloakUserFilter.getEmail(),
                0,
                10);
        verify(userService).search(any(), any());
    }
}
