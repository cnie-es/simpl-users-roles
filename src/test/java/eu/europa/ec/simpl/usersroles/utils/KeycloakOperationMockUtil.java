package eu.europa.ec.simpl.usersroles.utils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import eu.europa.ec.simpl.usersroles.adapters.RoleAdapter;
import eu.europa.ec.simpl.usersroles.models.Role;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public final class KeycloakOperationMockUtil {

    private KeycloakOperationMockUtil() {}

    public static void mockGetRoleByName_NotFound(RoleAdapter mock, String roleName) {

        given(mock.getRoleByName(roleName)).willReturn(Optional.empty());
    }

    public static void mockGetRoleByName_Found(RoleAdapter mock, String roleName, Role rep) {

        given(mock.getRoleByName(roleName)).willReturn(Optional.of(rep));
    }

    public static void mockGetRoleById_Found(RoleAdapter mock, UUID id, Role rep) {

        given(mock.getRoleById(id)).willReturn(Optional.of(rep));
    }

    public static void mockGetRoleById_NotFound(RoleAdapter mock, UUID id) {

        given(mock.getRoleById(id)).willReturn(Optional.empty());
    }

    public static void mockCreateRole_Success(RoleAdapter mock, Role rep) {

        given(mock.createRole(rep)).willReturn(new RoleAdapter.CreateKeycloakRoleOutcome.Success(rep));
    }

    public static void mockCreateRole_Duplicated(RoleAdapter mock, Role rep) {

        given(mock.createRole(rep)).willReturn(new RoleAdapter.CreateKeycloakRoleOutcome.Duplicated(rep));
    }

    public static void mockUpdateRole_Success(RoleAdapter mock, Role updated) {

        given(mock.updateRole(updated)).willReturn(new RoleAdapter.UpdateKeycloakRoleOutCome.Success(updated));
    }

    public static void mockUpdateRole_NotFound(RoleAdapter mock, String roleName) {

        given(mock.updateRole(any(Role.class)))
                .willReturn(new RoleAdapter.UpdateKeycloakRoleOutCome.NotFound(roleName));
    }

    public static void mockDeleteRole_Success(RoleAdapter mock, String code, Role rep) {

        given(mock.deleteRole(new RoleAdapter.DeleteRoleArgs.DeleteByName(code)))
                .willReturn(new RoleAdapter.DeleteKeycloakRoleOutcome.Success(rep));
    }

    public static void mockDeleteRole_NotFound(RoleAdapter mock, String code) {

        given(mock.deleteRole(new RoleAdapter.DeleteRoleArgs.DeleteByName(code)))
                .willReturn(
                        new RoleAdapter.DeleteKeycloakRoleOutcome.NotFound(new RoleAdapter.RoleIdentifier.Code(code)));
    }

    public static void mockGetRoleList(RoleAdapter mock, Collection<Role> roles) {
        given(mock.getRolesList()).willReturn(roles);
    }
}
