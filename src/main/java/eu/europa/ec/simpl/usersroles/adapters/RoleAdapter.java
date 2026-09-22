package eu.europa.ec.simpl.usersroles.adapters;

import eu.europa.ec.simpl.usersroles.models.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * An outbound port for HTTP keycloak operations
 */
public interface RoleAdapter {

    Optional<Role> getRoleByName(@NotBlank String roleName);

    Optional<Role> getRoleById(@NotNull UUID id);

    UpdateKeycloakRoleOutCome updateRole(@NotNull Role roleUpdate);

    CreateKeycloakRoleOutcome createRole(@NotNull Role role);

    DeleteKeycloakRoleOutcome deleteRole(@NotNull DeleteRoleArgs deleteRoleArgs);

    IsKeycloakRoleAssignedOutcome isRoleAssigned(@NotBlank String roleName);

    Collection<Role> getRolesList();

    sealed interface CreateKeycloakRoleOutcome {
        record Success(@NotNull Role role) implements CreateKeycloakRoleOutcome {}

        record Duplicated(@NotNull Role role) implements CreateKeycloakRoleOutcome {}
    }

    sealed interface UpdateKeycloakRoleOutCome {
        record Success(@NotNull Role roleUpdated) implements UpdateKeycloakRoleOutCome {}

        record NotFound(@NotBlank String roleName) implements UpdateKeycloakRoleOutCome {}

        record InvalidInput(@NotBlank String message) implements UpdateKeycloakRoleOutCome {}
    }

    sealed interface DeleteKeycloakRoleOutcome {
        record Success(@NotNull Role role) implements DeleteKeycloakRoleOutcome {}

        record NotFound(@NotNull RoleIdentifier roleIdentifier) implements DeleteKeycloakRoleOutcome {}
    }

    sealed interface IsKeycloakRoleAssignedOutcome {
        record Success(boolean assigned) implements IsKeycloakRoleAssignedOutcome {}

        record NotFound(@NotBlank String roleName) implements IsKeycloakRoleAssignedOutcome {}
    }

    sealed interface DeleteRoleArgs {

        record DeleteById(@NotNull UUID id) implements DeleteRoleArgs {}

        record DeleteByName(@NotBlank String name) implements DeleteRoleArgs {}
    }

    sealed interface RoleIdentifier {

        record Id(UUID id) implements RoleIdentifier {}

        record Code(String code) implements RoleIdentifier {}
    }
}
