package eu.europa.ec.simpl.usersroles.adapters.impl;

import static eu.europa.ec.simpl.common.test.TestUtil.a;
import static eu.europa.ec.simpl.usersroles.adapters.RoleAdapter.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;
import static org.mockito.BDDMockito.verify;
import static org.mockito.BDDMockito.willThrow;

import eu.europa.ec.simpl.usersroles.adapters.RoleAdapter;
import eu.europa.ec.simpl.usersroles.adapters.mappers.KeycloakMapperImpl;
import eu.europa.ec.simpl.usersroles.exceptions.KeycloakException;
import eu.europa.ec.simpl.usersroles.models.Role;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleByIdResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class RoleAdapterImplTest {

    @Mock
    RealmResource realm;

    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS)
    RolesResource rolesResource;

    @Mock
    RoleByIdResource roleByIdResource;

    @Mock
    RoleResource roleResource;

    @Spy
    KeycloakMapperImpl keycloakMapper;

    @InjectMocks
    RoleAdapterImpl adapter;

    @Test
    void getRoleByName_whenRoleExists_returnsOptionalWithRole() {

        given(realm.roles()).willReturn(rolesResource);

        String roleName = "existing-role";
        RoleRepresentation rep = new RoleRepresentation();
        rep.setName(roleName);

        given(rolesResource.get(roleName)).willReturn(roleResource);
        given(roleResource.toRepresentation()).willReturn(rep);

        var result = adapter.getRoleByName(roleName);

        assertThat(result).isPresent();
        then(rolesResource).should().get(roleName);
        then(roleResource).should().toRepresentation();
    }

    @Test
    void getRoleByName_whenNotFound_returnsEmptyOptional() {

        given(realm.roles()).willReturn(rolesResource);

        String roleName = "missing-role";

        given(rolesResource.get(roleName)).willReturn(roleResource);
        given(roleResource.toRepresentation()).willThrow(new NotFoundException("not found"));

        var result = adapter.getRoleByName(roleName);

        assertThat(result).isEmpty();
    }

    @Test
    void getRoleByName_whenWebApplicationException_wrappedInException() {
        String roleName = "failing-role";
        Response resp = Response.status(500).build();
        WebApplicationException wae = new WebApplicationException(resp);
        given(realm.roles()).willReturn(rolesResource);
        given(rolesResource.get(roleName)).willReturn(roleResource);
        given(roleResource.toRepresentation()).willThrow(wae);

        assertThatThrownBy(() -> adapter.getRoleByName(roleName)).isInstanceOf(KeycloakException.class);
    }

    @Test
    void getRoleById_whenRoleExists_returnsOptionalWithRole() {

        given(realm.rolesById()).willReturn(roleByIdResource);

        var id = UUID.randomUUID();
        RoleRepresentation rep = new RoleRepresentation();
        rep.setId(id.toString());

        given(roleByIdResource.getRole(id.toString())).willReturn(rep);

        var result = adapter.getRoleById(id);

        assertThat(result).isPresent();
        then(roleByIdResource).should().getRole(id.toString());
    }

    @Test
    void getRoleById_whenNotFound_returnsEmptyOptional() {

        given(realm.rolesById()).willReturn(roleByIdResource);

        var id = UUID.randomUUID();

        given(roleByIdResource.getRole(id.toString())).willThrow(new NotFoundException("not found"));

        var result = adapter.getRoleById(id);

        assertThat(result).isEmpty();
    }

    @Test
    void getRoleById_whenWebApplicationException_wrappedInException() {
        given(realm.rolesById()).willReturn(roleByIdResource);
        UUID id = UUID.randomUUID();
        Response resp = Response.status(500).build();
        WebApplicationException wae = new WebApplicationException(resp);

        given(roleByIdResource.getRole(id.toString())).willThrow(wae);

        assertThatThrownBy(() -> adapter.getRoleById(id)).isInstanceOf(KeycloakException.class);
    }

    @Test
    void updateRole_whenRoleDoesNotExist_returnsNotFound() {
        var update = new Role();
        update.setId(UUID.randomUUID());
        update.setCode("role-code");
        update.setName("role-code");

        given(realm.roles()).willReturn(rolesResource);

        given(rolesResource.get(update.getCode())).willThrow(new NotFoundException("not found"));

        var outcome = adapter.updateRole(update);

        assertThat(outcome).isInstanceOf(UpdateKeycloakRoleOutCome.NotFound.class);
    }

    @Test
    void updateRole_whenNameChanges_returnsInvalidInputAndDoesNotCallUpdateRole() {

        given(realm.roles()).willReturn(rolesResource);

        UUID id = UUID.randomUUID();

        RoleRepresentation existing = new RoleRepresentation();
        existing.setId(id.toString());
        existing.setName("original-name");
        existing.setDescription("old-desc");

        var update = new Role();
        update.setId(id);
        update.setCode(existing.getName() + "-changed");
        update.setName(existing.getName());
        update.setDescription("new-desc");

        given(rolesResource.get(update.getCode())).willReturn(roleResource);

        given(roleResource.toRepresentation()).willReturn(existing);

        var outcome = adapter.updateRole(update);

        assertThat(outcome).isInstanceOf(UpdateKeycloakRoleOutCome.InvalidInput.class);

        then(roleByIdResource).should(never()).updateRole(anyString(), any(RoleRepresentation.class));
    }

    @Test
    void updateRole_whenValidUpdate_updatesDescriptionAndReturnsSuccess() {

        given(realm.roles()).willReturn(rolesResource);

        UUID id = UUID.randomUUID();

        var existing = new RoleRepresentation();
        existing.setId(id.toString());
        existing.setName("role-name");
        existing.setDescription("old-desc");

        var update = new Role();
        update.setId(id);
        update.setCode("role-name");
        update.setName("role-name-human-readable");
        update.setDescription("new-desc");

        given(rolesResource.get(update.getCode())).willReturn(roleResource);
        given(roleResource.toRepresentation()).willReturn(existing);

        given(realm.rolesById()).willReturn(roleByIdResource);

        var outcome = adapter.updateRole(update);
        assertThat(outcome).isInstanceOfSatisfying(UpdateKeycloakRoleOutCome.Success.class, success -> assertThat(
                        success.roleUpdated().getDescription())
                .isEqualTo(update.getDescription()));

        then(roleByIdResource).should().updateRole(eq(update.getId().toString()), any());
    }

    @Test
    void updateRole_whenWebApplicationException_wrappedInException() {

        given(realm.roles()).willReturn(rolesResource);

        String roleCode = "role-code";

        given(rolesResource.get(roleCode)).willReturn(roleResource);

        UUID id = UUID.randomUUID();
        var update = new Role();
        update.setId(id);
        update.setCode(roleCode);
        update.setName(roleCode);
        update.setDescription("desc");

        Response resp = Response.status(500).build();
        WebApplicationException wae = new WebApplicationException(resp);

        given(rolesResource.get(update.getCode())).willThrow(wae);

        assertThatThrownBy(() -> adapter.updateRole(update)).isInstanceOf(KeycloakException.class);
    }

    @Test
    void createRole_whenSuccess_returnsSuccessOutcome() {

        given(realm.roles()).willReturn(rolesResource);

        var role = new Role();
        role.setName("new-role");

        var roleRepresentation = a(RoleRepresentation.class);
        roleRepresentation.setId(null);
        given(rolesResource.get(any()).toRepresentation()).willReturn(roleRepresentation);

        var outcome = adapter.createRole(role);

        assertThat(outcome).isInstanceOf(CreateKeycloakRoleOutcome.Success.class);

        then(rolesResource).should().create(any());
    }

    @Test
    void createRole_whenConflict_returnsDuplicatedOutcome() {

        given(realm.roles()).willReturn(rolesResource);

        var role = new Role();
        role.setName("existing-role");

        Response response = Response.status(HttpStatus.CONFLICT.value()).build();
        WebApplicationException wae = new WebApplicationException(response);

        willThrow(wae).given(rolesResource).create(any());

        var outcome = adapter.createRole(role);

        assertThat(outcome).isInstanceOf(CreateKeycloakRoleOutcome.Duplicated.class);
    }

    @Test
    void createRole_whenOtherError_wrappedInException() {

        given(realm.roles()).willReturn(rolesResource);

        var role = new Role();
        role.setName("failing-role");

        Response response = Response.status(500).build();
        WebApplicationException wae = new WebApplicationException(response);

        willThrow(wae).given(rolesResource).create(any());

        assertThatThrownBy(() -> adapter.createRole(role)).isInstanceOf(KeycloakException.class);
    }

    @Test
    void deleteRole_whenRoleDoesNotExist_returnsNotFound() {

        given(realm.rolesById()).willReturn(roleByIdResource);

        var id = UUID.randomUUID();

        given(roleByIdResource.getRole(id.toString())).willThrow(new NotFoundException("not found"));

        var outcome = adapter.deleteRole(new DeleteRoleArgs.DeleteById(id));

        assertThat(outcome).isInstanceOf(RoleAdapter.DeleteKeycloakRoleOutcome.NotFound.class);

        then(roleByIdResource).should(never()).deleteRole(anyString());
    }

    @Test
    void deleteRole_whenSuccess_deletesRoleAndReturnsSuccess() {

        given(realm.rolesById()).willReturn(roleByIdResource);

        UUID id = UUID.randomUUID();

        RoleRepresentation rep = new RoleRepresentation();
        rep.setId(id.toString());
        rep.setName("role-to-delete");

        given(roleByIdResource.getRole(id.toString())).willReturn(rep);

        var outcome = adapter.deleteRole(new DeleteRoleArgs.DeleteById(id));

        assertThat(outcome)
                .isInstanceOfSatisfying(
                        RoleAdapter.DeleteKeycloakRoleOutcome.Success.class,
                        success -> assertThat(success.role().getId()).isEqualTo(id));

        then(roleByIdResource).should().deleteRole(rep.getId());
    }

    @Test
    void deleteRole_whenWebApplicationException_wrappedInException() {

        given(realm.rolesById()).willReturn(roleByIdResource);

        UUID id = UUID.randomUUID();

        Response response = Response.status(500).build();
        WebApplicationException wae = new WebApplicationException(response);

        given(roleByIdResource.getRole(id.toString())).willThrow(wae);

        assertThatThrownBy(() -> adapter.deleteRole(new DeleteRoleArgs.DeleteById(id)))
                .isInstanceOf(KeycloakException.class);
    }

    @Test
    void getRolesList_whenSuccess_returnsSuccessOutcomeWithRoles() {

        given(realm.roles()).willReturn(rolesResource);

        RoleRepresentation rep1 = new RoleRepresentation();
        rep1.setName("r1");
        RoleRepresentation rep2 = new RoleRepresentation();
        rep2.setName("r2");

        List<RoleRepresentation> roles = List.of(rep1, rep2);

        given(rolesResource.list()).willReturn(roles);

        var actual = adapter.getRolesList();

        assertThat(actual).hasSize(2);
    }

    @Test
    void getRolesList_whenWebApplicationException_wrappedInKeycloakException() {
        given(realm.roles()).willReturn(rolesResource);
        Response response = Response.status(500).build();
        WebApplicationException wae = new WebApplicationException(response);

        given(rolesResource.list()).willThrow(wae);

        assertThatThrownBy(() -> adapter.getRolesList()).isInstanceOf(KeycloakException.class);
    }

    @Test
    void isRoleAssigned_success_empty() {

        given(realm.roles()).willReturn(rolesResource);

        var roleName = "my-role";
        given(rolesResource.get(roleName)).willReturn(roleResource);

        given(roleResource.getUserMembers(0, 1)).willReturn(List.of());

        var outcome = adapter.isRoleAssigned(roleName);

        assertThat(outcome).isInstanceOfSatisfying(IsKeycloakRoleAssignedOutcome.Success.class, success -> assertThat(
                        success.assigned())
                .isFalse());
    }

    @Test
    void isRoleAssigned_success_notempty() {

        given(realm.roles()).willReturn(rolesResource);

        var roleName = "my-role";
        given(rolesResource.get(roleName)).willReturn(roleResource);

        var usersRoleAssigned = Instancio.ofList(UserRepresentation.class).create();

        given(roleResource.getUserMembers(0, 1)).willReturn(usersRoleAssigned);

        var outcome = adapter.isRoleAssigned(roleName);

        assertThat(outcome).isInstanceOfSatisfying(IsKeycloakRoleAssignedOutcome.Success.class, success -> assertThat(
                        success.assigned())
                .isTrue());

        verify(rolesResource, times(1)).get(roleName);

        verify(roleResource, times(1)).getUserMembers(0, 1);
    }

    @Test
    void isRoleAssigned_success_notfound() {

        given(realm.roles()).willReturn(rolesResource);

        var roleName = "my-role";

        given(rolesResource.get(roleName)).willThrow(new NotFoundException("Role not found"));

        var outcome = adapter.isRoleAssigned(roleName);

        assertThat(outcome).isInstanceOfSatisfying(IsKeycloakRoleAssignedOutcome.NotFound.class, success -> assertThat(
                        success.roleName())
                .isEqualTo(roleName));

        verify(rolesResource, times(1)).get(roleName);

        verify(roleResource, never()).getUserMembers(0, 1);
    }

    @Test
    void isRoleAssigned_success_waeException() {

        given(realm.roles()).willReturn(rolesResource);

        var roleName = "my-role";

        var resp = Response.status(500).build();

        var wae = new WebApplicationException(resp);

        given(rolesResource.get(roleName)).willThrow(wae);

        assertThatThrownBy(() -> adapter.isRoleAssigned(roleName)).isInstanceOf(KeycloakException.class);
    }
}
