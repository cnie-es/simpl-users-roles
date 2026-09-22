package eu.europa.ec.simpl.usersroles.services.impl;

import eu.europa.ec.simpl.common.constants.Roles;
import eu.europa.ec.simpl.common.security.AuthService;
import eu.europa.ec.simpl.common.utils.CollectionsUtil;
import eu.europa.ec.simpl.usersroles.entities.RoleRequestEntity;
import eu.europa.ec.simpl.usersroles.entities.RoleRequestedEntity;
import eu.europa.ec.simpl.usersroles.models.RoleRequest;
import eu.europa.ec.simpl.usersroles.models.RoleRequestFilter;
import eu.europa.ec.simpl.usersroles.models.RoleRequestStatusEnum;
import eu.europa.ec.simpl.usersroles.models.User;
import eu.europa.ec.simpl.usersroles.repositories.RoleRequestRepository;
import eu.europa.ec.simpl.usersroles.repositories.RoleRequestedRepository;
import eu.europa.ec.simpl.usersroles.repositories.specifications.RoleRequestSpecification;
import eu.europa.ec.simpl.usersroles.services.RoleRequestService;
import eu.europa.ec.simpl.usersroles.services.RoleService;
import eu.europa.ec.simpl.usersroles.services.UserService;
import eu.europa.ec.simpl.usersroles.services.mappers.RoleRequestMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoleRequestServiceImpl implements RoleRequestService {

    private static final String ROLE_REQUEST_NOT_OPEN_MSG =
            "Error canceling role request with id %s, cannot cancel role request in a not open state";

    RoleRequestRepository roleRequestRepository;

    RoleRequestedRepository roleRequestedRepository;

    RoleRequestMapper roleRequestMapper;

    AuthService authService;

    RoleService roleService;

    UserService userService;

    @Override
    @Transactional
    public CreateRoleRequestOutcome create(RoleRequest roleRequest) {

        log.info("Creating role request {}", roleRequest);

        var userEmail = authService.getEmail();

        roleRequest.setCreatedBy(userEmail);

        log.info("Retrieving user with email [{}]", userEmail);

        var userOpt = getCurrentUserByEmail();

        if (userOpt.isEmpty()) {
            return new CreateRoleRequestOutcome.UserNotFound(roleRequest);
        }

        var currentUser = userOpt.get();

        Set<String> userRoles =
                currentUser.getRoles() != null ? new HashSet<>(currentUser.getRoles()) : Collections.emptySet();

        var rolesAlreadyHeld = CollectionsUtil.intersection(roleRequest.getRolesRequested(), new HashSet<>(userRoles));

        if (!rolesAlreadyHeld.isEmpty()) {
            log.info(
                    "Error creating role request with [ID: {}] - [USER: {}] - some roles are already held by user: [{}]",
                    roleRequest.getId(),
                    userEmail,
                    rolesAlreadyHeld);
            return new CreateRoleRequestOutcome.RoleAlreadyHeld(roleRequest, rolesAlreadyHeld);
        } else {
            return createRoleRequest(roleRequest, userEmail);
        }
    }

    @Override
    @Transactional
    public DeleteRoleRequestOutcome delete(UUID roleRequestId) {

        log.info("Deleting role request with id [{}]", roleRequestId);

        var roleRequestOpt = roleRequestRepository.findById(roleRequestId);

        if (roleRequestOpt.isEmpty()) {
            log.warn("Error cancelling role request with id [{}] - role request not found", roleRequestId);
            return new DeleteRoleRequestOutcome.NotFound(roleRequestId);
        }

        var roleRequest = roleRequestOpt.get();

        log.info("Found role request with id [{}] - [ROLE-REQUEST-DETAILS: {}]", roleRequestId, roleRequest);

        var userEmail = authService.getEmail();

        if (roleRequest.getStatus() != RoleRequestStatusEnum.OPEN) {
            log.warn(
                    "Error cancelling role request with id [{}] - cannot cancel role request not in open state, actual state: [{}]",
                    roleRequestId,
                    roleRequest.getStatus());
            return new DeleteRoleRequestOutcome.RequestAlreadyProcessed(roleRequestId);
        }

        if (!roleRequest.getUserEmail().equalsIgnoreCase(userEmail)) {
            log.warn(
                    "Error cancelling role request with id [{}] - cannot cancel role request of another user [user-email: {}] - [request-user-email: {}]",
                    roleRequestId,
                    userEmail,
                    roleRequest.getUserEmail());
            return new DeleteRoleRequestOutcome.NotCreatedByUser(roleRequestId);
        }

        return new DeleteRoleRequestOutcome.Success(
                roleRequestId, updateRoleRequestStatus(roleRequest, null, RoleRequestStatusEnum.CANCELED));
    }

    @Override
    @Transactional
    public UpdateRoleRequestOutcome updateRequest(UUID requestId, RoleRequestStatusEnum status, Set<String> roleCode) {

        log.info("Updating role request [{}] - [STATUS: {}] - [ROLES: {}]", requestId, status, roleCode);

        if (status == RoleRequestStatusEnum.OPEN) {
            log.warn(
                    "Error updating role request with id [{}] - the status element in the request body is OPEN",
                    requestId);
            return new UpdateRoleRequestOutcome.InvalidInputStatus(requestId, status, roleCode);
        }

        if (status == RoleRequestStatusEnum.APPROVED && (roleCode == null || roleCode.isEmpty())) {
            log.warn(
                    "Error updating role request with id [{}] - approving role request but roles assigned list is empty or null",
                    requestId);
            return new UpdateRoleRequestOutcome.RolesAssignedEmpty(requestId, status, roleCode);
        }

        var roleRequestOpt = roleRequestRepository.findById(requestId);

        if (roleRequestOpt.isEmpty()) {
            log.warn("Error updating role request with id [{}] - role request not found", requestId);
            return new UpdateRoleRequestOutcome.NotFound(requestId, status, roleCode);
        }

        var roleRequest = roleRequestOpt.get();

        log.info(
                "Retrieving role request - found role request with id [ID: {}] - [DETAILS: {}]",
                requestId,
                roleRequest);

        if (roleRequest.getStatus() == status) {
            log.warn(
                    "Error updating role request with id [{}] - role request has already input status: {}",
                    requestId,
                    status);
            return new UpdateRoleRequestOutcome.StatusNotChanged(requestId, status, roleCode);
        }

        if (roleRequest.getStatus() != RoleRequestStatusEnum.OPEN) {
            log.warn(
                    "Error updating role request with id [{}] - role request is not in OPEN state: {}",
                    requestId,
                    roleRequest.getStatus());
            return new UpdateRoleRequestOutcome.AlreadyProcessed(requestId, status, roleCode);
        }

        var notExistingRoles = getRoleRequestNotExistingRoles(roleCode);

        if (!notExistingRoles.isEmpty()) {
            log.warn(
                    "Error updating role request with id [{}] - some input roles are missing [INPUT-ROLES: {}] - [MISSING-ROLES: {}]",
                    requestId,
                    roleCode,
                    notExistingRoles);
            return new UpdateRoleRequestOutcome.RolesNotFound(requestId, status, roleCode, notExistingRoles);
        }

        var reviewer = authService.getEmail();

        if (status == RoleRequestStatusEnum.APPROVED) {
            return approveRoleRequest(roleRequest, reviewer, roleCode);
        } else {
            log.info("Updating role request with id [ID: {}] - [ROLES-TO-ASSIGN: {}]", roleRequest.getId(), roleCode);
            return new UpdateRoleRequestOutcome.Success(
                    requestId, status, roleCode, updateRoleRequestStatus(roleRequest, reviewer, status));
        }
    }

    @Override
    public SearchRoleRequestsOutcome search(RoleRequestFilter filters, Pageable pageable) {

        log.info("Searching roles with filters: [{}] - [PAGEABLE: {}]", filters, pageable);

        var spec = new RoleRequestSpecification(filters);

        var page = roleRequestRepository.findAll(spec, pageable);

        log.info("Searching roles with filters: [{}] - [PAGE-FOUND: {}]", filters, page);

        return new SearchRoleRequestsOutcome.Success(filters, roleRequestMapper.toRoleRequestPage(page));
    }

    @Override
    public FindByRoleRequestOutcome findBy(UUID id) {

        log.info("Retrieving role request with id {}", id);

        var roleRequestOpt = roleRequestRepository.findWithRolesRequestedById(id);

        if (roleRequestOpt.isEmpty()) {
            return new FindByRoleRequestOutcome.RoleRequestNotFound(id);
        }

        var roleRequestEntity = roleRequestOpt.get();

        log.info(
                "Retrieving role request - found role request with id [ID: {}] - [DETAILS: {}]", id, roleRequestEntity);

        var userEmail = authService.getEmail();

        var userOpt = getCurrentUserByEmail();

        if (userOpt.isEmpty()) {
            return new FindByRoleRequestOutcome.UserNotFound(id, userEmail);
        }

        var user = userOpt.get();

        var userRoles = user.getRoles() != null ? user.getRoles() : new ArrayList<>();

        log.info("Retrieving role request - role request id [ID: {}] - [ROLES: {}]", id, userRoles);

        if (!userRoles.contains(Roles.TIER_1_USER_AND_ROLES_MANAGER)
                && !roleRequestEntity.getUserEmail().equalsIgnoreCase(userEmail)) {
            log.warn(
                    "Error, trying to retrieve role request id [ID: {}] of a different owner: [CURRENT-USER: {}] - [RQ-USER: {}]",
                    id,
                    userEmail,
                    roleRequestEntity.getUserEmail());
            return new FindByRoleRequestOutcome.DifferentCreator(
                    roleRequestEntity.getId(), userEmail, roleRequestEntity.getUserEmail());
        }

        return new FindByRoleRequestOutcome.Success(id, roleRequestMapper.toRoleRequest(roleRequestEntity));
    }

    private CreateRoleRequestOutcome createRoleRequest(RoleRequest roleRequest, String userEmail) {

        var notExistingRoles = getRoleRequestNotExistingRoles(roleRequest.getRolesRequested());

        if (!notExistingRoles.isEmpty()) {
            log.warn(
                    "Error creating role request [{}], some roles are not found: [{}]",
                    roleRequest,
                    roleRequest.getRolesRequested());
            return new CreateRoleRequestOutcome.RolesNotExisting(roleRequest, notExistingRoles);
        }

        var rolesAlreadyRequested = getRolesAlreadyRequested(roleRequest.getRolesRequested(), userEmail);

        if (!rolesAlreadyRequested.isEmpty()) {
            log.warn(
                    "Error creating role request [{}], some roles are already requested: [{}]",
                    roleRequest,
                    rolesAlreadyRequested);
            return new CreateRoleRequestOutcome.RoleAlreadyRequested(roleRequest, rolesAlreadyRequested);
        }

        return new CreateRoleRequestOutcome.Success(saveRoleRequest(roleRequest, userEmail));
    }

    private Set<String> getRoleRequestNotExistingRoles(Set<String> rolesRequested) {

        var notExistingRoles = new HashSet<String>();

        rolesRequested.forEach(role -> {
            switch (roleService.findRoleBy(new RoleService.FindByArgs.FindByCode(role))) {
                case RoleService.FindRoleByOutcome.Success found -> {
                    log.info("Role with code/name [{}] found", role);
                }
                case RoleService.FindRoleByOutcome.NotFound notFound -> {
                    log.info("Role with code/name [{}] not found", role);
                    notExistingRoles.add(role);
                }
            }
        });

        return notExistingRoles;
    }

    private Set<String> getRolesAlreadyRequested(Set<String> rolesRequested, String userEmail) {

        if (StringUtils.isBlank(userEmail)) {
            throw new IllegalArgumentException("Parameter userEmail is null or empty");
        }

        if (rolesRequested == null) {
            throw new IllegalArgumentException("Parameter rolesRequested is null");
        }

        if (rolesRequested.isEmpty()) {
            return Collections.emptySet();
        }

        var userRoleRequested =
                roleRequestedRepository.findByRoleRequest_UserEmailAndRoleIn(userEmail.toLowerCase(), rolesRequested);

        return userRoleRequested.stream().map(RoleRequestedEntity::getRole).collect(Collectors.toSet());
    }

    private UpdateRoleRequestOutcome approveRoleRequest(
            RoleRequestEntity roleRequestEntity, String reviewer, Set<String> rolesAssigned) {

        log.info(
                "Approving role request with id [ID: {}] - [ROLES-TO-ASSIGN: {}]",
                roleRequestEntity.getId(),
                rolesAssigned);

        var userOpt = getCurrentUserByEmail(roleRequestEntity.getUserEmail());

        if (userOpt.isEmpty()) {
            return new UpdateRoleRequestOutcome.NotFound(
                    roleRequestEntity.getId(), RoleRequestStatusEnum.APPROVED, rolesAssigned);
        }

        var user = userOpt.get();

        log.info("Found user with email [{}] - [user: [{}]", roleRequestEntity.getUserEmail(), user);

        Set<String> userRolesAlreadyHeld =
                user.getRoles() != null ? new HashSet<>(user.getRoles()) : Collections.emptySet();

        log.info("Approving role request [ID: {}] - [USER-ROLES: {}]", roleRequestEntity.getId(), userRolesAlreadyHeld);

        var rolesUnion = CollectionsUtil.union(userRolesAlreadyHeld, rolesAssigned);

        log.info("Approving role request with id: [{}] - New Roles: [{}]", roleRequestEntity.getId(), rolesUnion);

        var updatedRq = approveRoleRequest(roleRequestEntity, reviewer, user.getId(), rolesUnion, rolesAssigned);

        log.info(
                "Successfully approved role request [ID: {}] - [OLD-ROLES: [{}] - [NEW-ROLES: [{}]",
                roleRequestEntity.getId(),
                userRolesAlreadyHeld,
                rolesUnion);

        return new UpdateRoleRequestOutcome.Success(
                roleRequestEntity.getId(), RoleRequestStatusEnum.APPROVED, rolesAssigned, updatedRq);
    }

    private RoleRequest saveRoleRequest(RoleRequest roleRequest, String userEmail) {

        var roleRequestEntity = roleRequestMapper.toRoleRequestEntity(roleRequest, userEmail, null);

        roleRequest
                .getRolesRequested()
                .forEach(roleRequested -> roleRequestEntity.addRoleRequested(
                        roleRequestMapper.toRoleRequestedEntity(roleRequest, roleRequested, roleRequestEntity)));

        roleRequestRepository.saveAndFlush(roleRequestEntity);

        return roleRequestMapper.toRoleRequest(roleRequestEntity);
    }

    private RoleRequest updateRoleRequestStatus(
            RoleRequestEntity roleRequestEntity, String reviewer, RoleRequestStatusEnum newStatus) {

        if (newStatus == RoleRequestStatusEnum.CANCELED
                && roleRequestEntity.getStatus() != RoleRequestStatusEnum.OPEN) {
            throw new IllegalStateException(ROLE_REQUEST_NOT_OPEN_MSG.formatted(
                    roleRequestEntity.getId().toString()));
        }

        roleRequestEntity.setStatus(newStatus);
        roleRequestEntity.setReviewedBy(reviewer);

        roleRequestRepository.flush();

        return roleRequestMapper.toRoleRequest(roleRequestEntity);
    }

    private RoleRequest approveRoleRequest(
            RoleRequestEntity roleRequestEntity,
            String reviewer,
            UUID userId,
            Set<String> rolesUnion,
            Set<String> rolesAssigned) {

        roleRequestEntity.getRolesRequested().stream()
                .filter(r -> rolesAssigned.contains(r.getRole()))
                .forEach(r -> r.setApproved(Boolean.TRUE));

        var rolesRequested = roleRequestEntity.getRolesRequested().stream()
                .map(RoleRequestedEntity::getRole)
                .collect(Collectors.toSet());

        rolesAssigned.stream().filter(r -> !rolesRequested.contains(r)).forEach(role -> {
            var roleRequestedByReviewer = roleRequestMapper.toRoleRequestedEntity(reviewer, role, roleRequestEntity);
            roleRequestedByReviewer.setApproved(Boolean.TRUE);
            roleRequestEntity.addRoleRequested(roleRequestedByReviewer);
        });

        roleRequestEntity.setStatus(RoleRequestStatusEnum.APPROVED);
        roleRequestEntity.setReviewedBy(reviewer);

        roleRequestRepository.flush();

        var updatedRq = roleRequestMapper.toRoleRequest(roleRequestEntity);

        userService.updateUserRoles(
                new UserService.UpdateUserRolesArgs.WithRoleNames(userId, new ArrayList<>(rolesUnion)));
        return updatedRq;
    }

    private Optional<User> getCurrentUserByEmail() {
        return getCurrentUserByEmail(authService.getEmail());
    }

    private Optional<User> getCurrentUserByEmail(String email) {

        log.info("Retrieving current user by email [{}]", email);

        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Email cannot be null or blank");
        }

        return switch (userService.getUserByEmail(email)) {
            case UserService.GetUserOutcome.Success s -> {
                log.info("Successfully retrieved user with email [{}] - [USER-DETAILS: {}]", email, s.user());
                yield Optional.of(s.user());
            }
            case UserService.GetUserOutcome.NotFoundByEmail nf -> {
                log.warn("User with email [{}] Not Found", email);
                yield Optional.empty();
            }
            case UserService.GetUserOutcome.NotFoundById nf -> throw new IllegalStateException(
                    "Retrieving user with email but not found by id");
        };
    }
}
