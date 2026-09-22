package eu.europa.ec.simpl.usersroles.services;

import eu.europa.ec.simpl.usersroles.models.RoleRequest;
import eu.europa.ec.simpl.usersroles.models.RoleRequestFilter;
import eu.europa.ec.simpl.usersroles.models.RoleRequestStatusEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;

@Validated
public interface RoleRequestService {

    CreateRoleRequestOutcome create(@NotNull RoleRequest roleRequest);

    sealed interface CreateRoleRequestOutcome {

        RoleRequest roleRequest();

        record Success(@NotNull RoleRequest roleRequest) implements CreateRoleRequestOutcome {}

        record RoleAlreadyHeld(@NotNull RoleRequest roleRequest, @NotNull Set<String> alreadyHeldRoles)
                implements CreateRoleRequestOutcome {}

        record RolesNotExisting(@NotNull RoleRequest roleRequest, @NotNull @NotEmpty Set<String> notExistingRoles)
                implements CreateRoleRequestOutcome {}

        record RoleAlreadyRequested(@NotNull RoleRequest roleRequest, @NotNull Set<String> alreadyRequestedRoles)
                implements CreateRoleRequestOutcome {}

        record UserNotFound(@NotNull RoleRequest roleRequest) implements CreateRoleRequestOutcome {}
    }

    DeleteRoleRequestOutcome delete(@NotNull UUID roleRequestId);

    sealed interface DeleteRoleRequestOutcome {

        UUID roleRequestId();

        record Success(@NotNull UUID roleRequestId, RoleRequest roleRequest) implements DeleteRoleRequestOutcome {}

        record RequestAlreadyProcessed(@NotNull UUID roleRequestId) implements DeleteRoleRequestOutcome {}

        record NotFound(@NotNull UUID roleRequestId) implements DeleteRoleRequestOutcome {}

        record NotCreatedByUser(@NotNull UUID roleRequestId) implements DeleteRoleRequestOutcome {}
    }

    UpdateRoleRequestOutcome updateRequest(
            @NotNull UUID requestId, @NotNull RoleRequestStatusEnum status, Set<@NotBlank String> roleCode);

    sealed interface UpdateRoleRequestOutcome {

        UUID requestId();

        RoleRequestStatusEnum status();

        Set<String> roleCode();

        record RolesNotFound(
                @NotNull UUID requestId,
                @NotNull RoleRequestStatusEnum status,
                @NotEmpty Set<@NotBlank String> roleCode,
                @NotEmpty Set<@NotBlank String> missingRoles)
                implements UpdateRoleRequestOutcome {}

        record NotFound(
                @NotNull UUID requestId,
                @NotNull RoleRequestStatusEnum status,
                @NotEmpty Set<@NotBlank String> roleCode)
                implements UpdateRoleRequestOutcome {}

        record StatusNotChanged(
                @NotNull UUID requestId,
                @NotNull RoleRequestStatusEnum status,
                @NotEmpty Set<@NotBlank String> roleCode)
                implements UpdateRoleRequestOutcome {}

        record AlreadyProcessed(
                @NotNull UUID requestId,
                @NotNull RoleRequestStatusEnum status,
                @NotEmpty Set<@NotBlank String> roleCode)
                implements UpdateRoleRequestOutcome {}

        record Success(
                @NotNull UUID requestId,
                @NotNull RoleRequestStatusEnum status,
                @NotEmpty Set<@NotBlank String> roleCode,
                RoleRequest updatedRoleRequest)
                implements UpdateRoleRequestOutcome {}

        record InvalidInputStatus(
                @NotNull UUID requestId,
                @NotNull RoleRequestStatusEnum status,
                @NotEmpty Set<@NotBlank String> roleCode)
                implements UpdateRoleRequestOutcome {}

        record RolesAssignedEmpty(
                @NotNull UUID requestId,
                @NotNull RoleRequestStatusEnum status,
                @NotEmpty Set<@NotBlank String> roleCode)
                implements UpdateRoleRequestOutcome {}
    }

    SearchRoleRequestsOutcome search(@NotNull RoleRequestFilter filters, Pageable pageable);

    sealed interface SearchRoleRequestsOutcome {
        RoleRequestFilter filters();

        record Success(@NotNull RoleRequestFilter filters, @NotNull Page<RoleRequest> page)
                implements SearchRoleRequestsOutcome {}
    }

    FindByRoleRequestOutcome findBy(UUID id);

    sealed interface FindByRoleRequestOutcome {

        UUID id();

        record Success(@NotNull UUID id, @NotNull RoleRequest roleRequest) implements FindByRoleRequestOutcome {}

        record RoleRequestNotFound(@NotNull UUID id) implements FindByRoleRequestOutcome {}

        record UserNotFound(@NotNull UUID id, @NotBlank String userEmail) implements FindByRoleRequestOutcome {}

        record DifferentCreator(@NotNull UUID id, @NotBlank String currentUser, @NotBlank String roleRequestCreator)
                implements FindByRoleRequestOutcome {}
    }
}
