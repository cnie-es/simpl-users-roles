package eu.europa.ec.simpl.usersroles.services;

import eu.europa.ec.simpl.usersroles.models.IdentityAttribute;
import eu.europa.ec.simpl.usersroles.models.Role;
import eu.europa.ec.simpl.usersroles.models.RoleFilter;
import eu.europa.ec.simpl.usersroles.services.model.InvalidOutput;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RoleService {

    ReplaceIdentityAttributesOutcome replaceIdentityAttributes(UUID roleId, List<String> attributesCode);

    RemoveAttributeForRoleOutcome removeAttributeForRole(String attributeCode, UUID roleId);

    DuplicateIdentityAttributeToAnOtherRoleOutcome duplicateIdentityAttributeToAnOtherRole(
            UUID sourceRoleId, UUID destinationRoleId);

    FindRoleByOutcome findRoleBy(FindByArgs findByArgs);

    FindRolesByOutcome findRolesBy(FindRolesByArgs findRolesByArgs);

    GetIdentityAttributesByRoleIdOutcome findIdentityAttributesByRoleId(UUID roleId);

    PreAssignIdentityAttributesToRoleOutcome preAssignIdentityAttributesToRole(
            Collection<String> idaCodes, String roleName);

    CreateRoleOutcome create(Role role);

    UpdateRoleOutcome update(Role role);

    DeleteRoleOutcome delete(UUID roleId);

    SearchRolesOutcome search(RoleFilter filter, Pageable pageable);

    ImportRolesOutcome importRoles(Collection<Role> roles);

    sealed interface FindByArgs {

        record FindById(@NotNull UUID id) implements FindByArgs {}

        record FindByCode(@NotBlank String code) implements FindByArgs {}
    }

    sealed interface FindRolesByArgs {

        record FindByIds(@NotNull List<UUID> roleIds) implements FindRolesByArgs {}

        record FindByCodes(@NotNull List<String> codes) implements FindRolesByArgs {}
    }

    sealed interface PreAssignIdentityAttributesToRoleOutcome {

        record Success() implements PreAssignIdentityAttributesToRoleOutcome {}

        record RoleNotFound(@NotBlank String roleCode) implements PreAssignIdentityAttributesToRoleOutcome {}
    }

    sealed interface DeleteRoleOutcome {
        record Success(@NotNull UUID uuid) implements DeleteRoleOutcome {}

        record NotFound(@NotNull UUID uuid) implements DeleteRoleOutcome {}

        record IsAssigned(@NotNull UUID uuid) implements DeleteRoleOutcome {}

        record BuiltInRole(@NotNull UUID uuid) implements DeleteRoleOutcome {}
    }

    sealed interface FindRoleByOutcome {
        record Success(@NotNull Role role) implements FindRoleByOutcome {}

        record NotFound(@NotNull RoleIdentifier notFoundRole) implements FindRoleByOutcome {}
    }

    sealed interface FindRolesByOutcome {
        record Success(@NotNull List<Role> roles) implements FindRolesByOutcome {}

        record NotFound(@NotNull List<Role> foundRoles, @NotNull List<RoleIdentifier> notFoundRoles)
                implements FindRolesByOutcome {}
    }

    sealed interface GetIdentityAttributesByRoleIdOutcome {
        record Success(@NotNull List<IdentityAttribute> identityAttributes)
                implements GetIdentityAttributesByRoleIdOutcome {}

        record NotFound(@NotNull UUID uuid) implements GetIdentityAttributesByRoleIdOutcome {}
    }

    sealed interface CreateRoleOutcome {
        record Success(@NotNull Role role) implements CreateRoleOutcome {}

        record Duplicated(@NotNull Role role) implements CreateRoleOutcome {}

        record BuiltInRole(@NotNull Role role) implements CreateRoleOutcome {}
    }

    sealed interface UpdateRoleOutcome {
        record Success(@NotNull Role role) implements UpdateRoleOutcome {}

        record NotFound(@NotNull UUID uuid) implements UpdateRoleOutcome {}

        record InvalidChangeNameAttempt(@NotNull UUID roleId, @NotBlank String roleName) implements UpdateRoleOutcome {}

        record BuiltInRole(@NotNull Role role) implements UpdateRoleOutcome {}
    }

    sealed interface SearchRolesOutcome {
        record Success(Page<Role> page) implements SearchRolesOutcome {}
    }

    sealed interface ReplaceIdentityAttributesOutcome {
        record Success(List<String> identityAttributes) implements ReplaceIdentityAttributesOutcome {}

        record RoleNotFound(@NotNull UUID uuid) implements ReplaceIdentityAttributesOutcome {}

        record InvalidIdentityAttributes(List<InvalidOutput> errors) implements ReplaceIdentityAttributesOutcome {}
    }

    sealed interface RemoveAttributeForRoleOutcome {
        record Success() implements RemoveAttributeForRoleOutcome {}

        record NotFound(@NotNull UUID uuid) implements RemoveAttributeForRoleOutcome {}
    }

    sealed interface ImportRolesOutcome {
        record Success(@NotNull Collection<Role> roles) implements ImportRolesOutcome {}
    }

    sealed interface DuplicateIdentityAttributeToAnOtherRoleOutcome {
        record Success() implements DuplicateIdentityAttributeToAnOtherRoleOutcome {}

        record NotFound(@NotNull UUID uuid) implements DuplicateIdentityAttributeToAnOtherRoleOutcome {}
    }

    sealed interface RoleIdentifier {

        record Id(UUID id) implements RoleIdentifier {}

        record Code(String code) implements RoleIdentifier {}
    }

    record RolesCheckResult(List<RoleIdentifier> notFound, List<Role> disabled) {}
}
