package eu.europa.ec.simpl.usersroles.controllers;

import static eu.europa.ec.simpl.common.test.TestUtil.a;
import static eu.europa.ec.simpl.common.test.TestUtil.aListOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.instancio.Select.field;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import eu.europa.ec.simpl.api.usersroles.t1.v2.model.RoleAssignmentRequestDTO;
import eu.europa.ec.simpl.api.usersroles.t1.v2.model.UpdateUserRequestDTO;
import eu.europa.ec.simpl.api.usersroles.t1.v2.model.UserDTO;
import eu.europa.ec.simpl.common.exceptions.http.HttpResponseException;
import eu.europa.ec.simpl.usersroles.controllers.mappers.RoleMapperTier1V2Impl;
import eu.europa.ec.simpl.usersroles.controllers.mappers.UserMapperTier1V2Impl;
import eu.europa.ec.simpl.usersroles.models.Role;
import eu.europa.ec.simpl.usersroles.models.User;
import eu.europa.ec.simpl.usersroles.models.UserPage;
import eu.europa.ec.simpl.usersroles.services.RoleService;
import eu.europa.ec.simpl.usersroles.services.UserService;
import eu.europa.ec.simpl.usersroles.services.UserService.CreateUserOutcome;
import eu.europa.ec.simpl.usersroles.services.UserService.GetUserOutcome;
import eu.europa.ec.simpl.usersroles.services.UserService.GetUserRolesOutcome;
import eu.europa.ec.simpl.usersroles.services.UserService.SearchUserOutcome;
import eu.europa.ec.simpl.usersroles.services.UserService.UpdateUserOutcome;
import eu.europa.ec.simpl.usersroles.services.UserService.UpdateUserRolesArgs;
import java.util.List;
import java.util.UUID;
import org.instancio.Instancio;
import org.instancio.TypeToken;
import org.instancio.junit.InstancioSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class UserControllerTier1V2Test {

    @Mock
    UserService userService;

    @Spy
    UserMapperTier1V2Impl userMapper;

    @Spy
    RoleMapperTier1V2Impl roleMapper;

    @Mock
    PagedResourcesAssembler<User> pagedResourcesAssembler;

    @InjectMocks
    UserControllerTier1V2 controller;

    @Test
    void createNewUser_whenSuccess_shouldReturnCreatedUser() {
        var userDto = a(UserDTO.class);
        var createdUser = a(User.class);

        given(userService.createUser(any(User.class))).willReturn(new CreateUserOutcome.Success(createdUser));

        // Act
        var result = controller.createNewUser(userDto);

        // Assert
        assertThat(result).isNotNull();
        then(userService).should().createUser(any(User.class));
    }

    @Test
    void createNewUser_whenAlreadyExists_shouldThrowConflict() {
        var userDto = new UserDTO().setEmail("mail@test.local");

        given(userService.createUser(any(User.class)))
                .willReturn(new CreateUserOutcome.AlreadyExists(userDto.getEmail()));

        assertThatThrownBy(() -> controller.createNewUser(userDto))
                .isInstanceOfSatisfying(HttpResponseException.class, ex -> assertThat(ex.getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("User already exists with email");
    }

    @Test
    void getUserById_whenSuccess_shouldReturnUserDto() {
        UUID id = UUID.randomUUID();
        var domainUser = a(User.class).setId(id);

        given(userService.getUserById(id)).willReturn(new GetUserOutcome.Success(domainUser));

        var result = controller.getUserById(id);

        assertThat(result).isNotNull();
        then(userService).should().getUserById(id);
    }

    @Test
    void getUserById_whenNotFoundById_shouldThrowNotFound() {
        UUID id = UUID.randomUUID();

        given(userService.getUserById(id)).willReturn(new GetUserOutcome.NotFoundById(id));

        assertThatThrownBy(() -> controller.getUserById(id))
                .isInstanceOfSatisfying(HttpResponseException.class, ex -> assertThat(ex.getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND))
                .hasMessageContaining("User not found with id %s".formatted(id));
    }

    @Test
    void getUserRoles_whenSuccess_shouldReturnRoleList() {
        UUID id = UUID.randomUUID();
        var roles = aListOf(Role.class);

        given(userService.getUserRoles(id)).willReturn(new GetUserRolesOutcome.Success(roles));

        var result = controller.getUserRoles(id);

        assertThat(result).isNotNull();
        then(userService).should().getUserRoles(id);
    }

    @Test
    void getUserRoles_whenUserNotFound_shouldThrowNotFound() {
        UUID id = UUID.randomUUID();

        given(userService.getUserRoles(id)).willReturn(new GetUserRolesOutcome.UserNotFound(id));

        assertThatThrownBy(() -> controller.getUserRoles(id))
                .isInstanceOfSatisfying(HttpResponseException.class, ex -> assertThat(ex.getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND))
                .hasMessageContaining("User not found with id %s".formatted(id));
    }

    @Test
    void getUserRoles_whenUserNotFound_rolesNotFound() {
        UUID id = UUID.randomUUID();

        given(userService.getUserRoles(id))
                .willReturn(new GetUserRolesOutcome.RolesNotFound(
                        List.of(new RoleService.RoleIdentifier.Code("role-code"))));

        assertThatThrownBy(() -> controller.getUserRoles(id))
                .isInstanceOfSatisfying(HttpResponseException.class, ex -> assertThat(ex.getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void searchUsers_whenPaged_shouldUsePageableAndReturnResponse() {
        Integer page = 0;
        Integer pageSize = 10;
        String firstName = "John";
        String lastName = "Doe";
        String username = "jdoe";
        String email = "john.doe@test.local";
        Boolean enabled = true;
        Boolean federated = null;
        Boolean unpaged = false;

        var pageable = Pageable.ofSize(pageSize).withPage(page);
        var domainUser = a(User.class);
        var pageImpl = new PageImpl<>(List.of(domainUser), pageable, 1);
        var userPage = new UserPage(pageSize, page, 1, List.of(domainUser), pageable);

        given(userService.search(any(), any(Pageable.class))).willReturn(new SearchUserOutcome.Success(userPage));

        var pagedModel = Instancio.of(new TypeToken<PagedModel<EntityModel<User>>>() {})
                .ignore(field(Link.class, "rel"))
                .ignore(field(Link.class, "href"))
                .create();

        pagedModel.removeLinks();
        pagedModel.add(Link.of("http://localhost/users?page=0&size=20").withSelfRel());
        pagedModel.add(Link.of("http://localhost/users?page=1&size=20").withRel("next"));

        given(pagedResourcesAssembler.toModel(any())).willReturn(pagedModel);

        controller.searchUsers(page, pageSize, firstName, lastName, username, email, enabled, federated, unpaged);

        then(userService).should().search(any(), any(Pageable.class));
        then(pagedResourcesAssembler).should().toModel(pageImpl);
        then(userMapper).should().toUsersPagedResponseDTO(any());
    }

    @Test
    void searchUsers_whenUnpaged_shouldUseUnpagedPageable() {
        Integer page = 0;
        Integer pageSize = 10;
        Boolean unpaged = true;

        var domainUser = a(User.class);

        var userPage = new UserPage(pageSize, page, 0, List.of(domainUser), Pageable.unpaged());

        given(userService.search(any(), any(Pageable.class))).willReturn(new SearchUserOutcome.Success(userPage));

        var pagedModel = Instancio.of(new TypeToken<PagedModel<EntityModel<User>>>() {})
                .ignore(field(Link.class, "rel"))
                .ignore(field(Link.class, "href"))
                .create();

        pagedModel.removeLinks();
        pagedModel.add(Link.of("http://localhost/users?page=0&size=20").withSelfRel());
        pagedModel.add(Link.of("http://localhost/users?page=1&size=20").withRel("next"));

        given(pagedResourcesAssembler.toModel(any())).willReturn(pagedModel);

        controller.searchUsers(page, pageSize, null, null, null, null, null, null, unpaged);

        then(userService).should().search(any(), argThat(Pageable::isUnpaged));
    }

    @Test
    void testUpdateUserById() {
        UUID id = UUID.randomUUID();
        var userDto = a(UpdateUserRequestDTO.class);

        given(userService.updateUser(any(UUID.class), any(User.class))).willReturn(new UpdateUserOutcome.Success());

        controller.updateUserById(id, userDto);
        assertThatNoException();
    }

    @Test
    void updateUserById_whenNotFound_shouldThrowNotFound() {
        UUID id = UUID.randomUUID();
        var userDto = a(UpdateUserRequestDTO.class);

        given(userService.updateUser(any(UUID.class), any(User.class))).willReturn(new UpdateUserOutcome.NotFound(id));

        assertThatThrownBy(() -> controller.updateUserById(id, userDto))
                .isInstanceOfSatisfying(HttpResponseException.class, ex -> assertThat(ex.getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND))
                .hasMessageContaining("User not found with id %s".formatted(id));
    }

    @Test
    void updateUserById_rolesDisabled() {
        UUID id = UUID.randomUUID();
        var userDto = a(UpdateUserRequestDTO.class);

        var roleDisabled = a(Role.class).setEnabled(false);

        given(userService.updateUser(any(UUID.class), any(User.class)))
                .willReturn(new UpdateUserOutcome.RolesDisabled(List.of(roleDisabled)));

        assertThatThrownBy(() -> controller.updateUserById(id, userDto))
                .isInstanceOfSatisfying(HttpResponseException.class, ex -> assertThat(ex.getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void updateUserById_rolesNotFond() {
        UUID id = UUID.randomUUID();
        var userDto = a(UpdateUserRequestDTO.class);

        given(userService.updateUser(any(UUID.class), any(User.class)))
                .willReturn(new UpdateUserOutcome.RolesNotFound(
                        List.of(new RoleService.RoleIdentifier.Id(UUID.randomUUID()))));

        assertThatThrownBy(() -> controller.updateUserById(id, userDto))
                .isInstanceOfSatisfying(HttpResponseException.class, ex -> assertThat(ex.getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void updateUserRoles_whenSuccess_shouldInvokeServiceWithArgs() {
        UUID id = UUID.randomUUID();
        var dto = a(RoleAssignmentRequestDTO.class);

        given(userService.updateUserRoles(any(UpdateUserRolesArgs.WithRoleIds.class)))
                .willReturn(new UpdateUserOutcome.Success());

        controller.updateUserRoles(id, dto);

        then(userService)
                .should()
                .updateUserRoles(argThat(args -> args instanceof UpdateUserRolesArgs.WithRoleIds withRoleIds
                        && withRoleIds.userId().equals(id)
                        && withRoleIds.roles().containsAll(dto.getRolesIds())));
    }

    @Test
    void updateUserRoles_whenNotFound_shouldThrowNotFound() {
        UUID id = UUID.randomUUID();
        var dto = a(RoleAssignmentRequestDTO.class);

        given(userService.updateUserRoles(any(UpdateUserRolesArgs.WithRoleIds.class)))
                .willReturn(new UpdateUserOutcome.NotFound(id));

        assertThatThrownBy(() -> controller.updateUserRoles(id, dto))
                .isInstanceOfSatisfying(HttpResponseException.class, ex -> assertThat(ex.getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND))
                .hasMessageContaining("User not found with id %s".formatted(id));
    }

    @Test
    void updateUserRoles_disabledRoles() {
        UUID id = UUID.randomUUID();
        var dto = a(RoleAssignmentRequestDTO.class);

        var roleDisabled = a(Role.class).setEnabled(false);

        given(userService.updateUserRoles(any(UpdateUserRolesArgs.WithRoleIds.class)))
                .willReturn(new UpdateUserOutcome.RolesDisabled(List.of(roleDisabled)));

        assertThatThrownBy(() -> controller.updateUserRoles(id, dto))
                .isInstanceOfSatisfying(HttpResponseException.class, ex -> assertThat(ex.getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void updateUserRoles_rolesNotFond() {
        UUID id = UUID.randomUUID();
        var dto = a(RoleAssignmentRequestDTO.class);

        given(userService.updateUserRoles(any(UpdateUserRolesArgs.WithRoleIds.class)))
                .willReturn(new UpdateUserOutcome.RolesNotFound(
                        List.of(new RoleService.RoleIdentifier.Id(UUID.randomUUID()))));

        assertThatThrownBy(() -> controller.updateUserRoles(id, dto))
                .isInstanceOfSatisfying(HttpResponseException.class, ex -> assertThat(ex.getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void testDeleteUserById(UUID id) {
        given(userService.deleteUser(any())).willReturn(new UserService.DeleteUserOutcome.Success(id));

        controller.deleteUserById(id);

        assertThatNoException();
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void testDeleteUserByIdWhenNotFoundByIdShouldThrowNotFound(UUID id) {
        given(userService.deleteUser(any())).willReturn(new UserService.DeleteUserOutcome.NotFoundById(id));

        assertThatThrownBy(() -> controller.deleteUserById(id))
                .isInstanceOfSatisfying(HttpResponseException.class, ex -> assertThat(ex.getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND))
                .hasMessageContaining("User not found with id %s".formatted(id));
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void testDeleteUserByIdWhenNotFoundByEmailShouldThrowIllegalStateException(UUID id) {
        given(userService.deleteUser(any())).willReturn(new UserService.DeleteUserOutcome.NotFoundByEmail("a-email"));

        assertThatThrownBy(() -> controller.deleteUserById(id)).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void testImportUsers(User user, User alreadyExistsUser) {
        given(userService.importUsers(any()))
                .willReturn(List.of(
                        new CreateUserOutcome.Success(user),
                        new CreateUserOutcome.AlreadyExists(alreadyExistsUser.getEmail())));

        var outcomes = userService.importUsers(List.of(user, alreadyExistsUser));

        assertThat(outcomes).hasSize(2);
        assertThat(outcomes).first().isInstanceOf(CreateUserOutcome.Success.class);
        assertThat(outcomes).last().isInstanceOf(CreateUserOutcome.AlreadyExists.class);
    }
}
