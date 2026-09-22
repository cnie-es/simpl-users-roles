package eu.europa.ec.simpl.usersroles.services.impl;

import static eu.europa.ec.simpl.common.test.TestUtil.*;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.*;

import eu.europa.ec.simpl.common.security.JwtService;
import eu.europa.ec.simpl.common.test.TestUtil;
import eu.europa.ec.simpl.usersroles.adapters.UserAdapter;
import eu.europa.ec.simpl.usersroles.exceptions.KeycloakException;
import eu.europa.ec.simpl.usersroles.models.Role;
import eu.europa.ec.simpl.usersroles.models.User;
import eu.europa.ec.simpl.usersroles.models.UserFilter;
import eu.europa.ec.simpl.usersroles.models.UserPage;
import eu.europa.ec.simpl.usersroles.services.RoleService;
import eu.europa.ec.simpl.usersroles.services.UserService;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.instancio.Instancio;
import org.instancio.junit.InstancioSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final String UUID_TEST = "5b7ee24f-e268-4cfa-8bc9-196d652c2336";

    @Mock
    UserAdapter userAdapter;

    @Mock
    RoleService roleService;

    @Mock
    JwtService jwtService;

    @InjectMocks
    UserServiceImpl userService;

    @Test
    void createUser() {

        // Given
        var expectedUserId = anUUID();
        var createdUser = a(User.class);
        given(userAdapter.createUser(any())).willReturn(new UserAdapter.CreateUserOutcome.Success(createdUser));
        // When
        var createUserOutcome = userService.createUser(createdUser.setId(expectedUserId));

        assertThat(createUserOutcome)
                .isInstanceOfSatisfying(
                        UserService.CreateUserOutcome.Success.class,
                        success -> assertThat(success.createdUser().getId()).isEqualTo(expectedUserId));
    }

    @Test
    void createUserFail() {
        given(userAdapter.createUser(any()))
                .willThrow(new KeycloakException(Response.serverError().build()));
        assertThrows(KeycloakException.class, () -> userService.createUser(a(User.class)));
    }

    @Test
    void getUserByEmail() {
        // Given
        var user = a(User.class);

        var roles = aListOf(Role.class);

        user.setRoles(roles.stream().map(Role::getCode).toList());

        given(roleService.findRolesBy(any())).willReturn(new RoleService.FindRolesByOutcome.Success(roles));

        given(userAdapter.getUserByEmail(any())).willReturn(Optional.of(user));

        // When
        var getUserOutcome = userService.getUserByEmail(user.getEmail());

        // Then
        assertThat(getUserOutcome)
                .isInstanceOfSatisfying(
                        UserService.GetUserOutcome.Success.class,
                        outcome -> assertThat(outcome.user().getEmail()).isEqualTo(user.getEmail()));
    }

    @Test
    void getUserByEmail_rolesNotFound() {
        // Given
        var user = a(User.class);

        var roles = Instancio.ofList(Role.class).size(10).create();

        user.setRoles(roles.stream().map(Role::getCode).toList());

        var found = List.of(roles.getFirst());
        var notFound = roles.stream().filter(role -> !found.contains(role)).toList();

        given(roleService.findRolesBy(any()))
                .willReturn(new RoleService.FindRolesByOutcome.NotFound(
                        found,
                        notFound.stream()
                                .<RoleService.RoleIdentifier>map(r -> new RoleService.RoleIdentifier.Code(r.getCode()))
                                .toList()));

        given(userAdapter.getUserByEmail(any())).willReturn(Optional.of(user));
        var outcome = userService.getUserByEmail(user.getEmail());

        assertThat(outcome).isInstanceOfSatisfying(UserService.GetUserOutcome.Success.class, success -> {
            assertThat(success.user().getRoles())
                    .isEqualTo(found.stream().map(Role::getCode).toList());
        });
    }

    @Test
    void getUserByEmailWhenUserNotFoundShouldReturnNotFound() {
        // Given
        var email = a(String.class);

        given(userAdapter.getUserByEmail(any())).willReturn(Optional.empty());

        // When
        var getUserOutcome = userService.getUserByEmail(email);

        // Then
        assertThat(getUserOutcome)
                .isInstanceOfSatisfying(
                        UserService.GetUserOutcome.NotFoundByEmail.class,
                        outcome -> assertThat(outcome.email()).isEqualTo(email));
    }

    @Test
    void importUsers() {
        // Given
        given(userAdapter.createUser(any())).willReturn(new UserAdapter.CreateUserOutcome.Success(a(User.class)));

        // When
        var users = aListOf(User.class);
        userService.importUsers(users);

        // Then
        then(userAdapter).should(times(users.size())).createUser(any());
    }

    @Test
    void importUser() {
        // Given
        given(userAdapter.createUser(any())).willReturn(new UserAdapter.CreateUserOutcome.Success(a(User.class)));

        // When
        userService.importUser(a(User.class));

        // Then
        then(userAdapter).should().createUser(any());
    }

    @Test
    void importUserWhenUserAlreadyExistsShouldReturnDuplicatedOutcome() {
        // Given
        given(userAdapter.createUser(any())).willReturn(new UserAdapter.CreateUserOutcome.AlreadyExists());

        // When
        var outcome = userService.importUser(a(User.class));

        assertThat(outcome).isInstanceOf(UserService.CreateUserOutcome.AlreadyExists.class);
    }

    @Test
    void getUserRoles() {
        var userId = UUID.fromString(UUID_TEST);
        var roles = aListOf(Role.class);

        given(roleService.findRolesBy(any())).willReturn(new RoleService.FindRolesByOutcome.Success(roles));

        given(userAdapter.getUser(any())).willReturn(anOptional(User.class));
        var getRolesDtoOutcome = userService.getUserRoles(userId);

        assertThat(getRolesDtoOutcome)
                .isInstanceOfSatisfying(
                        UserService.GetUserRolesOutcome.Success.class,
                        success -> assertThat(success.roles()).hasSize(roles.size()));
    }

    @Test
    void getUserRoles_rolesNotFound() {
        var userId = UUID.fromString(UUID_TEST);
        var roles = aListOf(Role.class);

        var rolesNotFoundIdentifiers = roles.stream()
                .<RoleService.RoleIdentifier>map(r -> new RoleService.RoleIdentifier.Code(r.getCode()))
                .toList();

        given(roleService.findRolesBy(any()))
                .willReturn(new RoleService.FindRolesByOutcome.NotFound(List.of(), rolesNotFoundIdentifiers));

        given(userAdapter.getUser(any())).willReturn(anOptional(User.class));
        var getRolesDtoOutcome = userService.getUserRoles(userId);

        assertThat(getRolesDtoOutcome)
                .isInstanceOfSatisfying(
                        UserService.GetUserRolesOutcome.RolesNotFound.class,
                        rolesNotFound ->
                                assertThat(rolesNotFound.rolesNotFond()).isEqualTo(rolesNotFoundIdentifiers));
    }

    @Test
    void getUserRolesUserNotFound() {
        var uuid = UUID.fromString(UUID_TEST);

        given(userAdapter.getUser(any())).willReturn(Optional.empty());

        var getRolesDtoOutcome = userService.getUserRoles(uuid);

        assertThat(getRolesDtoOutcome)
                .isInstanceOfSatisfying(UserService.GetUserRolesOutcome.UserNotFound.class, notFound -> assertThat(uuid)
                        .isEqualTo(notFound.userId()));
    }

    @Test
    void getUserById() {
        var id = UUID.fromString(UUID_TEST);

        var user = a(User.class).setId(id);
        var roles = aListOf(Role.class);

        user.setRoles(roles.stream().map(Role::getCode).toList());

        given(roleService.findRolesBy(any())).willReturn(new RoleService.FindRolesByOutcome.Success(roles));

        given(userAdapter.getUser(any())).willReturn(Optional.of(user));
        var getUserOutcome = userService.getUserById(id);

        assertThat(getUserOutcome)
                .isInstanceOfSatisfying(
                        UserService.GetUserOutcome.Success.class,
                        success -> assertThat(success.user().getId()).isEqualTo(id));
    }

    @Test
    void getUserById_rolesNotFound() {
        // Given
        var user = a(User.class);

        var roles = Instancio.ofList(Role.class).size(10).create();

        var found = List.of(roles.getFirst(), roles.getLast());
        var notFound = roles.stream().filter(role -> !found.contains(role)).toList();

        user.setRoles(roles.stream().map(Role::getCode).toList());

        given(roleService.findRolesBy(any()))
                .willReturn(new RoleService.FindRolesByOutcome.NotFound(
                        found,
                        notFound.stream()
                                .<RoleService.RoleIdentifier>map(r -> new RoleService.RoleIdentifier.Code(r.getCode()))
                                .toList()));

        given(userAdapter.getUserByEmail(any())).willReturn(Optional.of(user));

        var outcome = userService.getUserByEmail(user.getEmail());

        assertThat(outcome).isInstanceOfSatisfying(UserService.GetUserOutcome.Success.class, success -> {
            assertThat(success.user().getRoles())
                    .isEqualTo(found.stream().map(Role::getCode).toList());
        });
    }

    @Test
    void getUserByIdWhenUserNotFoundShouldReturnNotFound() {
        // Given
        var id = UUID.fromString(UUID_TEST);

        given(userAdapter.getUser(any())).willReturn(Optional.empty());

        // When
        var getUserOutcome = userService.getUserById(id);

        // Then
        assertThat(getUserOutcome)
                .isInstanceOfSatisfying(
                        UserService.GetUserOutcome.NotFoundById.class,
                        outcome -> assertThat(outcome.userId()).isEqualTo(id));
    }

    @Test
    void updateUser() {
        var id = UUID.fromString(UUID_TEST);
        var user = a(User.class).setId(id);

        var roles = user.getRoles().stream()
                .map(role -> a(Role.class).setEnabled(Boolean.TRUE).setCode(role))
                .toList();

        given(roleService.findRolesBy(new RoleService.FindRolesByArgs.FindByCodes(user.getRoles())))
                .willReturn(new RoleService.FindRolesByOutcome.Success(roles));
        given(userAdapter.getUser(id)).willReturn(Optional.of(user));
        given(userAdapter.replaceUserRoles(any())).willReturn(new UserAdapter.ReplaceUserRolesOutcome.Success());
        var updateUserOutcome = userService.updateUser(id, user);

        assertThat(updateUserOutcome).isInstanceOf(UserService.UpdateUserOutcome.Success.class);
    }

    @Test
    void updateUser_rolesNotFound() {
        var id = UUID.fromString(UUID_TEST);
        var user = a(User.class).setId(id);

        given(roleService.findRolesBy(new RoleService.FindRolesByArgs.FindByCodes(user.getRoles())))
                .willReturn(new RoleService.FindRolesByOutcome.NotFound(
                        List.of(),
                        Collections.singletonList(new RoleService.RoleIdentifier.Code(
                                user.getRoles().getFirst()))));

        given(userAdapter.getUser(id)).willReturn(Optional.of(user));

        verify(userAdapter, never()).updateUser(any(), any());
        verify(userAdapter, never()).replaceUserRoles(any());

        var updateUserOutcome = userService.updateUser(id, user);

        assertThat(updateUserOutcome).isInstanceOf(UserService.UpdateUserOutcome.RolesNotFound.class);

        var updateRolesNotFoundOutcome = (UserService.UpdateUserOutcome.RolesNotFound) updateUserOutcome;

        var rolesNotFond = updateRolesNotFoundOutcome.rolesNotFond();

        assertThat(rolesNotFond)
                .isEqualTo(List.of(
                        new RoleService.RoleIdentifier.Code(user.getRoles().getFirst())));
    }

    @Test
    void updateUser_rolesDisabled() {
        var id = UUID.fromString(UUID_TEST);
        var user = a(User.class).setId(id);

        var roles = user.getRoles().stream()
                .map(role -> a(Role.class).setEnabled(Boolean.TRUE).setCode(role))
                .toList();

        roles.getLast().setEnabled(false);

        given(roleService.findRolesBy(new RoleService.FindRolesByArgs.FindByCodes(user.getRoles())))
                .willReturn(new RoleService.FindRolesByOutcome.Success(roles));

        given(userAdapter.getUser(id)).willReturn(Optional.of(user));

        verify(userAdapter, never()).updateUser(any(), any());
        verify(userAdapter, never()).replaceUserRoles(any());

        var updateUserOutcome = userService.updateUser(id, user);

        assertThat(updateUserOutcome).isInstanceOf(UserService.UpdateUserOutcome.RolesDisabled.class);

        var rolesDisabledOutcome = (UserService.UpdateUserOutcome.RolesDisabled) updateUserOutcome;

        var rolesDisabled =
                rolesDisabledOutcome.rolesDisabled().stream().map(Role::getCode).toList();

        assertThat(rolesDisabled)
                .containsExactlyInAnyOrderElementsOf(List.of(user.getRoles().getLast()));
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void updateUserRoles_withId_success(User user, List<Role> roles) {

        roles.forEach(role -> {
            role.setEnabled(true);
        });

        var roleIds = roles.stream().map(Role::getId).toList();

        var findRolesByArgs = new RoleService.FindRolesByArgs.FindByIds(roleIds);

        given(roleService.findRolesBy(findRolesByArgs)).willReturn(new RoleService.FindRolesByOutcome.Success(roles));

        given(userAdapter.replaceUserRoles(any())).willReturn(new UserAdapter.ReplaceUserRolesOutcome.Success());

        var updateUserRolesArgs = new UserService.UpdateUserRolesArgs.WithRoleIds(user.getId(), roleIds);

        var updateUserOutcome = userService.updateUserRoles(updateUserRolesArgs);

        assertThat(updateUserOutcome).isInstanceOf(UserService.UpdateUserOutcome.Success.class);

        verify(roleService).findRolesBy(findRolesByArgs);

        verify(userAdapter).replaceUserRoles(new UserAdapter.ReplaceUserRolesArgs.WithRoleIds(user.getId(), roleIds));
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void updateUserRoles_withCodes_success(User user, List<Role> roles) {

        roles.forEach(role -> {
            role.setEnabled(true);
        });

        var roleCodes = roles.stream().map(Role::getCode).toList();

        var findRolesByArgs = new RoleService.FindRolesByArgs.FindByCodes(roleCodes);

        given(roleService.findRolesBy(findRolesByArgs)).willReturn(new RoleService.FindRolesByOutcome.Success(roles));

        given(userAdapter.replaceUserRoles(any())).willReturn(new UserAdapter.ReplaceUserRolesOutcome.Success());

        var updateUserRolesArgs = new UserService.UpdateUserRolesArgs.WithRoleNames(user.getId(), roleCodes);

        var updateUserOutcome = userService.updateUserRoles(updateUserRolesArgs);

        assertThat(updateUserOutcome).isInstanceOf(UserService.UpdateUserOutcome.Success.class);

        verify(roleService).findRolesBy(findRolesByArgs);

        verify(userAdapter)
                .replaceUserRoles(new UserAdapter.ReplaceUserRolesArgs.WithRoleNames(user.getId(), roleCodes));
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void updateUserRoles_withId_disabledRole(User user, List<Role> roles) {

        roles.forEach(role -> {
            role.setEnabled(true);
        });

        roles.getLast().setEnabled(false);

        var roleIds = roles.stream().map(Role::getId).toList();

        var findRolesByArgs = new RoleService.FindRolesByArgs.FindByIds(roleIds);

        given(roleService.findRolesBy(findRolesByArgs)).willReturn(new RoleService.FindRolesByOutcome.Success(roles));

        var updateUserRolesArgs = new UserService.UpdateUserRolesArgs.WithRoleIds(user.getId(), roleIds);

        var updateUserOutcome = userService.updateUserRoles(updateUserRolesArgs);

        assertThat(updateUserOutcome)
                .isInstanceOfSatisfying(UserService.UpdateUserOutcome.RolesDisabled.class, rolesDisabled -> {
                    assertThat(rolesDisabled.rolesDisabled()).containsExactlyInAnyOrder(roles.getLast());
                });

        verify(roleService).findRolesBy(findRolesByArgs);

        verifyNoInteractions(userAdapter);
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void updateUserRoles_withCode_disabledRole(User user, List<Role> roles) {

        roles.forEach(role -> {
            role.setEnabled(true);
        });

        roles.getLast().setEnabled(false);

        var roleCodes = roles.stream().map(Role::getCode).toList();

        var findRolesByArgs = new RoleService.FindRolesByArgs.FindByCodes(roleCodes);

        given(roleService.findRolesBy(findRolesByArgs)).willReturn(new RoleService.FindRolesByOutcome.Success(roles));

        var updateUserRolesArgs = new UserService.UpdateUserRolesArgs.WithRoleNames(user.getId(), roleCodes);

        var updateUserOutcome = userService.updateUserRoles(updateUserRolesArgs);

        assertThat(updateUserOutcome)
                .isInstanceOfSatisfying(UserService.UpdateUserOutcome.RolesDisabled.class, rolesDisabled -> {
                    assertThat(rolesDisabled.rolesDisabled()).containsExactlyInAnyOrder(roles.getLast());
                });

        verify(roleService).findRolesBy(findRolesByArgs);

        verifyNoInteractions(userAdapter);
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void updateUserRoles_withCode_NotFoundRole(User user, List<Role> roles) {

        roles.forEach(role -> {
            role.setEnabled(true);
        });

        var roleCodes = roles.stream().map(Role::getCode).toList();

        var findRolesByArgs = new RoleService.FindRolesByArgs.FindByCodes(roleCodes);

        given(roleService.findRolesBy(findRolesByArgs))
                .willReturn(new RoleService.FindRolesByOutcome.NotFound(
                        List.of(),
                        Collections.singletonList(new RoleService.RoleIdentifier.Code(
                                roles.getLast().getCode()))));

        var updateUserRolesArgs = new UserService.UpdateUserRolesArgs.WithRoleNames(user.getId(), roleCodes);

        var updateUserOutcome = userService.updateUserRoles(updateUserRolesArgs);

        assertThat(updateUserOutcome)
                .isInstanceOfSatisfying(UserService.UpdateUserOutcome.RolesNotFound.class, rolesNotFound -> {
                    assertThat(rolesNotFound.rolesNotFond())
                            .containsExactlyInAnyOrder(new RoleService.RoleIdentifier.Code(
                                    roles.getLast().getCode()));
                });

        verify(roleService).findRolesBy(findRolesByArgs);

        verifyNoInteractions(userAdapter);
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void updateUserRoles_withId_NotFoundRole(User user, List<Role> roles) {

        roles.forEach(role -> {
            role.setEnabled(true);
        });

        var roleIds = roles.stream().map(Role::getId).toList();

        var findRolesByArgs = new RoleService.FindRolesByArgs.FindByIds(roleIds);

        given(roleService.findRolesBy(findRolesByArgs))
                .willReturn(new RoleService.FindRolesByOutcome.NotFound(
                        Collections.emptyList(),
                        Collections.singletonList(new RoleService.RoleIdentifier.Id(
                                roles.getLast().getId()))));

        var updateUserRolesArgs = new UserService.UpdateUserRolesArgs.WithRoleIds(user.getId(), roleIds);

        var updateUserOutcome = userService.updateUserRoles(updateUserRolesArgs);

        assertThat(updateUserOutcome)
                .isInstanceOfSatisfying(UserService.UpdateUserOutcome.RolesNotFound.class, rolesNotFound -> {
                    assertThat(rolesNotFound.rolesNotFond())
                            .containsExactlyInAnyOrder(new RoleService.RoleIdentifier.Id(
                                    roles.getLast().getId()));
                });

        verify(roleService).findRolesBy(findRolesByArgs);

        verifyNoInteractions(userAdapter);
    }

    @Test
    void updateUserNotFound() {
        var id = UUID.fromString(UUID_TEST);
        var user = a(User.class);
        given(userAdapter.getUser(id)).willReturn(Optional.empty());

        var updateUserOutcome = userService.updateUser(id, user);

        assertThat(updateUserOutcome)
                .isInstanceOfSatisfying(
                        UserService.UpdateUserOutcome.NotFound.class,
                        outcome -> assertThat(outcome.userId()).isEqualTo(id));
    }

    @Test
    void deleteUserNotFound() {
        UUID userId = UUID.fromString(UUID_TEST);

        given(userAdapter.getUser(userId)).willReturn(Optional.empty());

        var deleteUserOutcome = userService.deleteUser(userId);

        assertThat(deleteUserOutcome)
                .isInstanceOfSatisfying(
                        UserService.DeleteUserOutcome.NotFoundById.class,
                        outcome -> assertThat(outcome.userId()).isEqualTo(userId));
    }

    @Test
    void deleteUser_Success() {
        var id = UUID.fromString(UUID_TEST);
        given(userAdapter.getUser(any())).willReturn(anOptional(User.class));
        var deleteUserOutcome = userService.deleteUser(id);

        assertThat(deleteUserOutcome)
                .isInstanceOfSatisfying(
                        UserService.DeleteUserOutcome.Success.class,
                        outcome -> assertThat(outcome.userId()).isEqualTo(id));
        then(userAdapter).should().deleteUser(id);
    }

    @Test
    void testDeleteUserByEmail() {
        var user = a(User.class);
        var roles = aListOf(Role.class);

        user.setRoles(roles.stream().map(Role::getCode).toList());

        given(roleService.findRolesBy(any())).willReturn(new RoleService.FindRolesByOutcome.Success(roles));
        given(userAdapter.getUserByEmail(any())).willReturn(Optional.of(user));
        var deleteUserOutcome = userService.deleteByEmail(user.getEmail());

        assertThat(deleteUserOutcome).isInstanceOf(UserService.DeleteUserOutcome.Success.class);
        then(userAdapter).should().deleteUser(user.getId());
    }

    @Test
    void search() {
        var filter = a(UserFilter.class);

        var users = TestUtil.aListOf(User.class);

        var userPage = new UserPage(0, 0, users.size(), users, Pageable.unpaged());

        given(userAdapter.getUsers(any(), any())).willReturn(userPage);

        var searchUserOutcome = userService.search(filter, Pageable.unpaged());

        assertThat(searchUserOutcome)
                .isInstanceOfSatisfying(
                        UserService.SearchUserOutcome.Success.class,
                        outcome -> assertThat(outcome.users().items()).hasSameSizeAs(userPage.items()));
    }

    @Test
    void search_ThrowsKeycloakException_WhenClientErrorExceptionOccurs() {
        // Given
        var filter = a(UserFilter.class);
        var clientErrorException = generateClientErrorException();

        given(userAdapter.getUsers(any(), any())).willThrow(clientErrorException);

        // Then
        assertThrows(KeycloakException.class, () -> userService.search(filter, Pageable.unpaged()));
    }

    @Test
    void logout_success() {
        var claim = "sid";
        var sid = "junit-sid";
        given(jwtService.getClaim(claim)).willReturn(sid);
        assertDoesNotThrow(() -> userService.logout());
    }

    private ClientErrorException generateClientErrorException() {
        return new ClientErrorException("Client error", Response.Status.BAD_REQUEST);
    }
}
