package eu.europa.ec.simpl.usersroles.controllers;

import eu.europa.ec.simpl.api.usersroles.t1.v2.exchanges.RoleRequestsApi;
import eu.europa.ec.simpl.api.usersroles.t1.v2.model.CreateRoleRequestDTO;
import eu.europa.ec.simpl.api.usersroles.t1.v2.model.CreateRoleRequestResponseDTO;
import eu.europa.ec.simpl.api.usersroles.t1.v2.model.RoleRequestDTO;
import eu.europa.ec.simpl.api.usersroles.t1.v2.model.RoleRequestPagedResponseDTO;
import eu.europa.ec.simpl.api.usersroles.t1.v2.model.RoleRequestReviewDTO;
import eu.europa.ec.simpl.api.usersroles.t1.v2.model.UserRoleRequestPagedResponseDTO;
import eu.europa.ec.simpl.common.exceptions.http.HttpExceptionBuilder;
import eu.europa.ec.simpl.common.exceptions.http.HttpResponseException;
import eu.europa.ec.simpl.common.security.AuthService;
import eu.europa.ec.simpl.common.utils.SortUtilV2;
import eu.europa.ec.simpl.usersroles.controllers.mappers.RoleRequestMapperTier1V2;
import eu.europa.ec.simpl.usersroles.models.RoleRequest;
import eu.europa.ec.simpl.usersroles.models.RoleRequestFilter;
import eu.europa.ec.simpl.usersroles.models.RoleRequestStatusEnum;
import eu.europa.ec.simpl.usersroles.services.RoleRequestService;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequestMapping("tier1/v2")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleRequestControllerTier1V2 implements RoleRequestsApi {

    private static final String ROLE_REQUEST_NOT_FOUND_MESSAGE = "Role request with id %s not found ";

    RoleRequestMapperTier1V2 roleRequestMapper;

    RoleRequestService roleRequestService;

    AuthService authService;

    PagedResourcesAssembler<RoleRequest> pagedResourcesAssembler;

    @Override
    public void cancelRoleRequestById(UUID roleRequestId) {

        log.info("Cancelling role request with id [{}]", roleRequestId);

        var outcome = roleRequestService.delete(roleRequestId);

        switch (outcome) {
            case RoleRequestService.DeleteRoleRequestOutcome.Success success -> {
                log.info(
                        "Successfully cancelled role request with id [{}] - updated role request: [{}]",
                        roleRequestId,
                        success.roleRequest());
            }

            case RoleRequestService.DeleteRoleRequestOutcome.NotFound notFound -> {
                log.warn(
                        "Error cancelling  role request with id [{}], role request not found",
                        notFound.roleRequestId());
                throw HttpExceptionBuilder.notFound()
                        .withDetail(ROLE_REQUEST_NOT_FOUND_MESSAGE.formatted(notFound.roleRequestId()))
                        .build();
            }
            case RoleRequestService.DeleteRoleRequestOutcome.RequestAlreadyProcessed alreadyProcessed -> {
                log.warn(
                        "Error cancelling  role request with id [{}], role request not open",
                        alreadyProcessed.roleRequestId());
                throw HttpExceptionBuilder.conflict()
                        .withDetail("Error cancelling  role request with id [%s], role request not open"
                                .formatted(alreadyProcessed.roleRequestId()))
                        .build();
            }

            case RoleRequestService.DeleteRoleRequestOutcome.NotCreatedByUser notCreatedByUser -> {
                log.warn(
                        "Error cancelling  role request with id [{}], role request not created by user",
                        notCreatedByUser.roleRequestId());
                throw HttpExceptionBuilder.forbidden()
                        .withDetail("Error cancelling  role request with id [%s], role request not created by user"
                                .formatted(notCreatedByUser.roleRequestId()))
                        .build();
            }
        }
    }

    @Override
    public CreateRoleRequestResponseDTO createNewRoleRequest(CreateRoleRequestDTO createRoleRequestDTO) {

        log.info("Creating role request: {}", createRoleRequestDTO);
        return switch (roleRequestService.create(roleRequestMapper.toRoleRequest(createRoleRequestDTO))) {
            case RoleRequestService.CreateRoleRequestOutcome.Success success -> {
                log.info("Successfully created role request: {}", createRoleRequestDTO);
                yield roleRequestMapper.toCreateRoleRequestResponseDTO(success.roleRequest());
            }

            case RoleRequestService.CreateRoleRequestOutcome.RoleAlreadyHeld ah -> {
                log.warn(
                        "Error processing role request: {}, the following roles are already held by user: {}",
                        createRoleRequestDTO,
                        ah.alreadyHeldRoles());
                throw HttpExceptionBuilder.conflict()
                        .withDetail(String.format("Some roles are already held by user: %s", ah.alreadyHeldRoles()))
                        .build();
            }

            case RoleRequestService.CreateRoleRequestOutcome.RolesNotExisting notExisting -> {
                log.warn(
                        "Error processing role request: {}, the following roles are missing: {}",
                        createRoleRequestDTO,
                        notExisting.notExistingRoles());
                throw HttpExceptionBuilder.badRequest()
                        .withDetail(String.format("Some roles are missing: %s", notExisting.notExistingRoles()))
                        .build();
            }

            case RoleRequestService.CreateRoleRequestOutcome.RoleAlreadyRequested ar -> {
                log.warn(
                        "Error processing role request: {}, the following roles are already requested: {}",
                        createRoleRequestDTO,
                        ar.alreadyRequestedRoles());
                throw HttpExceptionBuilder.unprocessableEntity()
                        .withDetail(String.format("Some roles are already requested: %s", ar.alreadyRequestedRoles()))
                        .build();
            }

            case RoleRequestService.CreateRoleRequestOutcome.UserNotFound notFound -> {
                log.warn(
                        "Error processing role request: {}, the user is not a valid user in the system",
                        createRoleRequestDTO);
                throw HttpExceptionBuilder.badRequest()
                        .withDetail("the user is not a valid user in the system")
                        .build();
            }
        };
    }

    @Override
    public RoleRequestDTO getRoleRequestById(UUID roleRequestId) {

        return switch (roleRequestService.findBy(roleRequestId)) {
            case RoleRequestService.FindByRoleRequestOutcome.Success success -> {
                log.info("Retrieved role request with [ID: {}] - Details: [{}]", success.id(), success.roleRequest());
                yield roleRequestMapper.toRoleRequestDTO(success.roleRequest());
            }
            case RoleRequestService.FindByRoleRequestOutcome.RoleRequestNotFound rqNotFound -> {
                log.warn("Retrieved role request with [ID: {}] not found", roleRequestId);
                throw HttpExceptionBuilder.notFound()
                        .withDetail(String.format("Role request with id [%s] not found", roleRequestId))
                        .build();
            }

            case RoleRequestService.FindByRoleRequestOutcome.UserNotFound userNotFound -> {
                log.warn(
                        "Retrieved role request with [ID: {}] - user not found [USER: {}]",
                        roleRequestId,
                        userNotFound.userEmail());
                throw buildUserNotFoundExeption(roleRequestId, userNotFound.userEmail());
            }

            case RoleRequestService.FindByRoleRequestOutcome.DifferentCreator differentOwner -> {
                log.warn(
                        "Cannot retrieve role request with id [ID: {}] of a different owner [USER: {}] - [CREATOR: {}]",
                        roleRequestId,
                        differentOwner.currentUser(),
                        differentOwner.roleRequestCreator());
                throw HttpExceptionBuilder.forbidden()
                        .withDetail(String.format(
                                "Cannot retrieve role request with id [ID: %s] of a different owner [USER: %s] - [CREATOR: %s]",
                                roleRequestId, differentOwner.currentUser(), differentOwner.roleRequestCreator()))
                        .build();
            }
        };
    }

    @Override
    public void putRoleRequestById(UUID roleRequestId, RoleRequestReviewDTO roleRequestReviewDTO) {

        log.info(
                "Updating role request with [ID: {}] - [STATUS: {}] - [ROLES: {}]",
                roleRequestId,
                roleRequestReviewDTO.getStatus(),
                roleRequestReviewDTO.getRolesAssigned());

        var outcome = roleRequestService.updateRequest(
                roleRequestId,
                RoleRequestStatusEnum.valueOf(roleRequestReviewDTO.getStatus().getValue()),
                new HashSet<>(roleRequestReviewDTO.getRolesAssigned()));

        switch (outcome) {
            case RoleRequestService.UpdateRoleRequestOutcome.NotFound nf -> {
                log.warn("Role request with id [{}] not found", roleRequestId);
                throw HttpExceptionBuilder.notFound()
                        .withDetail(String.format("Role request with id [%s] not found", roleRequestId))
                        .build();
            }

            case RoleRequestService.UpdateRoleRequestOutcome.StatusNotChanged statusNotChanged -> {
                log.warn(
                        "Cannot update Role request with id [{}] - Status not changed: [{}]",
                        roleRequestId,
                        roleRequestReviewDTO.getStatus());
                throw HttpExceptionBuilder.badRequest()
                        .withDetail(String.format(
                                "Cannot update Role request with id [%s] - Status not changed: [%s]",
                                roleRequestId, roleRequestReviewDTO.getStatus()))
                        .build();
            }

            case RoleRequestService.UpdateRoleRequestOutcome.AlreadyProcessed alreadyProcessed -> {
                log.warn("Cannot update Role request with id [{}] - Status of role request is not open", roleRequestId);
                throw HttpExceptionBuilder.conflict()
                        .withDetail(String.format(
                                "Cannot update Role request with id [%s] - Status of role request is not open",
                                roleRequestId))
                        .build();
            }

            case RoleRequestService.UpdateRoleRequestOutcome.RolesNotFound rolesNotFound -> {
                log.warn(
                        "Cannot update Role request with id [{}] - Some roles are not found: [{}]",
                        roleRequestId,
                        rolesNotFound.missingRoles());
                throw HttpExceptionBuilder.badRequest()
                        .withDetail(String.format(
                                "Cannot update Role request with id [%s] - Some roles are not found: [%s]",
                                roleRequestId, rolesNotFound.missingRoles()))
                        .build();
            }

            case RoleRequestService.UpdateRoleRequestOutcome.RolesAssignedEmpty rolesAssignedEmpty -> {
                log.warn(
                        "Cannot update Role request with id [{}] - Status APPROVED but assigned roles list is empty or null",
                        roleRequestId);
                throw HttpExceptionBuilder.badRequest()
                        .withDetail("Status APPROVED but assigned roles list is empty or null")
                        .build();
            }

            case RoleRequestService.UpdateRoleRequestOutcome.Success success -> log.info(
                    "Successfully updated Role request with id [{}] - [STATUS: [{}]  - [ROLES: {}]",
                    roleRequestId,
                    success.updatedRoleRequest().getStatus(),
                    success.roleCode());

            case RoleRequestService.UpdateRoleRequestOutcome.InvalidInputStatus invalidInputStatus -> {
                log.warn("Cannot update Role request with id [{}] - input status cannot be open", roleRequestId);

                throw HttpExceptionBuilder.badRequest()
                        .withDetail(String.format(
                                "Cannot update Role request with id [%s] - input status cannot be open", roleRequestId))
                        .build();
            }
        }
    }

    @Override
    public UserRoleRequestPagedResponseDTO searchEndUsersRoleRequests(
            Integer page, Integer pageSize, List<String> sort, String status, Boolean unpaged) {

        log.info(
                "Searching end users role requests with the following parameters: [PAGE: {}] - [PAGE-SIZE: {}] - [SORT: {}] - [STATUS: {}]",
                page,
                pageSize,
                sort,
                authService.getEmail(),
                status);

        var pageable = Boolean.TRUE.equals(unpaged)
                ? Pageable.unpaged()
                : PageRequest.of(page, pageSize, SortUtilV2.toSort(sort));

        return switch (roleRequestService.search(new RoleRequestFilter(authService.getEmail(), status), pageable)) {
            case RoleRequestService.SearchRoleRequestsOutcome.Success success -> {
                log.info(
                        "searchEndUsersRoleRequests - successfully retrieved paged list of role requests: [{}]",
                        success.page());
                var pageModel = pagedResourcesAssembler.toModel(success.page());
                yield roleRequestMapper.toUserRoleRequestPagedResponseDTO(pageModel);
            }
        };
    }

    @Override
    public RoleRequestPagedResponseDTO searchRoleRequests(
            Integer page, Integer pageSize, List<String> sort, String email, String status) {

        log.info(
                "Searching role requests with the following parameters: [PAGE: {}] - [PAGE-SIZE: {}] - [SORT: {}] - [EMAIL: {}] - [STATUS: {}]",
                page,
                pageSize,
                sort,
                email,
                status);

        var pageable = PageRequest.of(page, pageSize, SortUtilV2.toSort(sort));

        return switch (roleRequestService.search(new RoleRequestFilter(email, status), pageable)) {
            case RoleRequestService.SearchRoleRequestsOutcome.Success success -> {
                log.info(
                        "searchRoleRequests - successfully retrieved paged list of role requests: [{}]",
                        success.page());
                var pageModel = pagedResourcesAssembler.toModel(success.page());
                yield roleRequestMapper.toRolesPagedResponseDTO(pageModel);
            }
        };
    }

    private HttpResponseException buildUserNotFoundExeption(UUID id, String email) {

        throw HttpExceptionBuilder.notFound()
                .withDetail(String.format("role request with id [ID: %s] - user not found [USER: %s]", id, email))
                .build();
    }
}
