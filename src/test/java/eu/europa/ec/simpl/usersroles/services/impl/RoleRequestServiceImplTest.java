package eu.europa.ec.simpl.usersroles.services.impl;

import static eu.europa.ec.simpl.common.test.TestUtil.a;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import eu.europa.ec.simpl.common.security.AuthService;
import eu.europa.ec.simpl.common.utils.CollectionsUtil;
import eu.europa.ec.simpl.usersroles.entities.RoleRequestEntity;
import eu.europa.ec.simpl.usersroles.entities.RoleRequestedEntity;
import eu.europa.ec.simpl.usersroles.models.Role;
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
import eu.europa.ec.simpl.usersroles.services.mappers.RoleRequestMapperImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class RoleRequestServiceImplTest {

    @Spy
    RoleRequestMapperImpl roleRequestMapper;

    @Mock
    RoleService roleService;

    @Mock
    UserService userService;

    @Mock
    RoleRequestRepository roleRequestRepository;

    @Mock
    RoleRequestedRepository roleRequestedRepository;

    @Mock
    AuthService authService;

    @InjectMocks
    RoleRequestServiceImpl roleRequestService;

    @Test
    void testCreateRoleRequest_UserNotFound() {

        var roleRequest = a(RoleRequest.class);

        roleRequest.setRolesRequested(Set.of("ROLE1"));

        var userEmail = "a.b@example.com";

        given(authService.getEmail()).willReturn(userEmail);

        given(userService.getUserByEmail(userEmail))
                .willReturn(new UserService.GetUserOutcome.NotFoundByEmail(userEmail));

        var outcome = roleRequestService.create(roleRequest);

        verifyNoInteractions(roleRequestRepository);

        verifyNoInteractions(roleRequestMapper);

        assertThat(outcome).isInstanceOf(RoleRequestService.CreateRoleRequestOutcome.UserNotFound.class);

        assertThat(outcome.roleRequest()).isEqualTo(roleRequest);
    }

    @Test
    void testCreateRoleRequest_RolesAlreadyHeld() {

        var roleRequest = a(RoleRequest.class);

        var user = a(User.class);

        user.setRoles(List.of("ROLE2", "ROLE4"));

        var userEmail = "a.b@example.com";

        roleRequest.setRolesRequested(Set.of("ROLE1", "ROLE2"));

        given(authService.getEmail()).willReturn(userEmail);

        given(userService.getUserByEmail(userEmail)).willReturn(new UserService.GetUserOutcome.Success(user));

        var outcome = roleRequestService.create(roleRequest);

        verifyNoInteractions(roleRequestRepository);

        verifyNoInteractions(roleRequestMapper);

        assertThat(outcome).isInstanceOf(RoleRequestService.CreateRoleRequestOutcome.RoleAlreadyHeld.class);

        assertThat(outcome.roleRequest()).isEqualTo(roleRequest);

        var roleAlreadyHeldOutcome = (RoleRequestService.CreateRoleRequestOutcome.RoleAlreadyHeld) outcome;

        assertThat(roleAlreadyHeldOutcome.alreadyHeldRoles()).isEqualTo(Set.of("ROLE2"));
    }

    @Test
    void testCreateRoleRequest_NotExistingRole() {

        var roleRequest = a(RoleRequest.class);

        var role1 = a(Role.class);
        role1.setName("ROLE1");
        role1.setCode("ROLE1");

        var role3 = a(Role.class);

        role3.setName("ROLE3");
        role3.setCode("ROLE3");

        var user = a(User.class);

        user.setRoles(List.of("ROLE2", "ROLE4", "ROLE5"));

        var userEmail = "a.b@example.com";

        roleRequest.setRolesRequested(Set.of("ROLE1", "ROLE3"));

        given(authService.getEmail()).willReturn(userEmail);

        given(userService.getUserByEmail(userEmail)).willReturn(new UserService.GetUserOutcome.Success(user));

        given(roleService.findRoleBy(new RoleService.FindByArgs.FindByCode(role1.getCode())))
                .willReturn(new RoleService.FindRoleByOutcome.Success(role1));

        given(roleService.findRoleBy(new RoleService.FindByArgs.FindByCode(role3.getCode())))
                .willReturn(new RoleService.FindRoleByOutcome.NotFound(
                        new RoleService.RoleIdentifier.Code(role3.getCode())));

        var outcome = roleRequestService.create(roleRequest);

        verifyNoInteractions(roleRequestRepository);

        verifyNoInteractions(roleRequestMapper);

        assertThat(outcome).isInstanceOf(RoleRequestService.CreateRoleRequestOutcome.RolesNotExisting.class);

        assertThat(outcome.roleRequest()).isEqualTo(roleRequest);

        var rolesNotExistingOutcome = (RoleRequestService.CreateRoleRequestOutcome.RolesNotExisting) outcome;

        assertThat(rolesNotExistingOutcome.notExistingRoles()).isEqualTo(Set.of("ROLE3"));
    }

    @Test
    void testCreateRoleRequest_rolesAlreadyRequested() {

        var roleRequest = a(RoleRequest.class);

        var role1 = a(Role.class);
        role1.setName("ROLE1");
        role1.setCode("ROLE1");

        var role3 = a(Role.class);

        role3.setName("ROLE3");
        role3.setCode("ROLE3");

        var user = a(User.class);

        user.setRoles(List.of("ROLE2", "ROLE4", "ROLE5"));

        var userEmail = "a.b@example.com";

        roleRequest.setRolesRequested(Set.of("ROLE1", "ROLE3"));

        var alreadyRequestedRole = roleRequestMapper.toRoleRequestedEntity(
                roleRequest, role3.getCode(), roleRequestMapper.toRoleRequestEntity(roleRequest, userEmail, null));

        given(authService.getEmail()).willReturn(userEmail);

        given(userService.getUserByEmail(userEmail)).willReturn(new UserService.GetUserOutcome.Success(user));

        given(roleService.findRoleBy(new RoleService.FindByArgs.FindByCode(role1.getCode())))
                .willReturn(new RoleService.FindRoleByOutcome.Success(role1));

        given(roleService.findRoleBy(new RoleService.FindByArgs.FindByCode(role3.getCode())))
                .willReturn(new RoleService.FindRoleByOutcome.Success(role3));

        given(roleRequestedRepository.findByRoleRequest_UserEmailAndRoleIn(userEmail, roleRequest.getRolesRequested()))
                .willReturn(List.of(alreadyRequestedRole));

        var outcome = roleRequestService.create(roleRequest);

        verify(roleRequestRepository, never()).saveAndFlush(any());

        assertThat(outcome).isInstanceOf(RoleRequestService.CreateRoleRequestOutcome.RoleAlreadyRequested.class);

        assertThat(outcome.roleRequest()).isEqualTo(roleRequest);

        var rolesAlreadyRequestedOutcome = (RoleRequestService.CreateRoleRequestOutcome.RoleAlreadyRequested) outcome;

        assertThat(rolesAlreadyRequestedOutcome.alreadyRequestedRoles()).isEqualTo(Set.of("ROLE3"));
    }

    @Test
    void testCreateRoleRequest_Success() {

        var roleRequest = a(RoleRequest.class);

        var role1 = a(Role.class);
        role1.setName("ROLE1");
        role1.setCode("ROLE1");

        var role3 = a(Role.class);
        role3.setName("ROLE3");
        role3.setCode("ROLE3");

        var user = a(User.class);
        user.setRoles(List.of("ROLE2", "ROLE4", "ROLE5"));

        var userEmail = "a.b@example.com";

        roleRequest.setRolesRequested(Set.of("ROLE1", "ROLE3"));

        given(authService.getEmail()).willReturn(userEmail);

        given(userService.getUserByEmail(userEmail)).willReturn(new UserService.GetUserOutcome.Success(user));

        given(roleService.findRoleBy(new RoleService.FindByArgs.FindByCode(role1.getCode())))
                .willReturn(new RoleService.FindRoleByOutcome.Success(role1));

        given(roleService.findRoleBy(new RoleService.FindByArgs.FindByCode(role3.getCode())))
                .willReturn(new RoleService.FindRoleByOutcome.Success(role3));

        given(roleRequestedRepository.findByRoleRequest_UserEmailAndRoleIn(userEmail, roleRequest.getRolesRequested()))
                .willReturn(Collections.emptyList());

        var outcome = roleRequestService.create(roleRequest);

        verify(roleRequestRepository).saveAndFlush(any());

        verify(roleRequestMapper).toRoleRequest(any());

        assertThat(outcome).isInstanceOf(RoleRequestService.CreateRoleRequestOutcome.Success.class);
    }

    @Test
    void cancelRoleRequest_NotOpen() {

        var roleRequestInput = a(RoleRequest.class);
        roleRequestInput.setStatus(RoleRequestStatusEnum.APPROVED);

        var roleRequestEntity =
                roleRequestMapper.toRoleRequestEntity(roleRequestInput, "test@example.com", "reviewer1@example.com");

        given(roleRequestRepository.findById(roleRequestInput.getId())).willReturn(Optional.of(roleRequestEntity));

        var outcome = roleRequestService.delete(roleRequestInput.getId());

        assertThat(outcome).isInstanceOf(RoleRequestService.DeleteRoleRequestOutcome.RequestAlreadyProcessed.class);

        var alreadyProcessedOutcome = (RoleRequestService.DeleteRoleRequestOutcome.RequestAlreadyProcessed) outcome;

        assertThat(alreadyProcessedOutcome.roleRequestId()).isEqualTo(roleRequestInput.getId());

        assertThat(roleRequestEntity.getStatus()).isEqualTo(RoleRequestStatusEnum.APPROVED);

        verify(roleRequestRepository).findById(roleRequestInput.getId());

        verify(roleRequestRepository, never()).save(any());
    }

    @Test
    void cancelRoleRequest_NotFound() {

        var roleRequestInput = a(RoleRequest.class);

        given(roleRequestRepository.findById(roleRequestInput.getId())).willReturn(Optional.empty());

        var outcome = roleRequestService.delete(roleRequestInput.getId());

        assertThat(outcome).isInstanceOf(RoleRequestService.DeleteRoleRequestOutcome.NotFound.class);

        var notFoundOutcome = (RoleRequestService.DeleteRoleRequestOutcome.NotFound) outcome;

        assertThat(notFoundOutcome.roleRequestId()).isEqualTo(roleRequestInput.getId());

        verify(roleRequestRepository).findById(roleRequestInput.getId());

        verify(roleRequestRepository, never()).flush();

        verify(roleRequestMapper, never()).toRoleRequestEntity(any(), any(), any());
    }

    @Test
    void cancelRoleRequest_NotCreatedByUser() {

        var roleRequestInput = a(RoleRequest.class);
        roleRequestInput.setStatus(RoleRequestStatusEnum.APPROVED);

        var userEmail = "test@example.com";

        roleRequestInput.setCreatedBy("anotheruser@example.com");

        given(authService.getEmail()).willReturn(userEmail);

        var roleRequestEntity =
                roleRequestMapper.toRoleRequestEntity(roleRequestInput, userEmail, "reviewer1@example.com");

        given(roleRequestRepository.findById(roleRequestInput.getId())).willReturn(Optional.of(roleRequestEntity));

        var outcome = roleRequestService.delete(roleRequestInput.getId());

        assertThat(outcome).isInstanceOf(RoleRequestService.DeleteRoleRequestOutcome.RequestAlreadyProcessed.class);

        var alreadyProcessedOutcome = (RoleRequestService.DeleteRoleRequestOutcome.RequestAlreadyProcessed) outcome;

        assertThat(alreadyProcessedOutcome.roleRequestId()).isEqualTo(roleRequestInput.getId());

        assertThat(roleRequestEntity.getStatus()).isEqualTo(RoleRequestStatusEnum.APPROVED);

        verify(roleRequestRepository).findById(roleRequestInput.getId());

        verify(roleRequestRepository, never()).flush();
    }

    @Test
    void cancelRoleRequest_Success() {

        var roleRequestInput = a(RoleRequest.class);
        roleRequestInput.setStatus(RoleRequestStatusEnum.OPEN);
        roleRequestInput.setRolesAssigned(Collections.emptySet());

        var userEmail = "test@example.com";

        roleRequestInput.setCreatedBy("test@example.com");

        given(authService.getEmail()).willReturn(userEmail);

        var roleRequestOutput = new RoleRequest();
        BeanUtils.copyProperties(roleRequestInput, roleRequestOutput);
        roleRequestOutput.setStatus(RoleRequestStatusEnum.CANCELED);

        var roleRequestEntity =
                roleRequestMapper.toRoleRequestEntity(roleRequestInput, userEmail, "reviewer1@example.com");

        roleRequestInput.getRolesRequested().forEach(roleRequested -> {
            roleRequestEntity.addRoleRequested(
                    roleRequestMapper.toRoleRequestedEntity(roleRequestInput, roleRequested, roleRequestEntity));
        });

        given(roleRequestRepository.findById(roleRequestInput.getId())).willReturn(Optional.of(roleRequestEntity));

        var outcome = roleRequestService.delete(roleRequestInput.getId());

        assertThat(outcome).isInstanceOf(RoleRequestService.DeleteRoleRequestOutcome.Success.class);

        var successOutcome = (RoleRequestService.DeleteRoleRequestOutcome.Success) outcome;

        verify(roleRequestRepository).flush();

        assertThat(roleRequestEntity.getStatus()).isEqualTo(RoleRequestStatusEnum.CANCELED);

        assertThat(successOutcome.roleRequestId()).isEqualTo(roleRequestInput.getId());

        assertThat(successOutcome.roleRequest().getStatus()).isEqualTo(RoleRequestStatusEnum.CANCELED);

        verify(roleRequestRepository).findById(roleRequestInput.getId());
    }

    @Test
    void updateRoleRequest_notFound() {

        var roleRequestInput = a(RoleRequest.class);

        given(roleRequestRepository.findById(roleRequestInput.getId())).willReturn(Optional.empty());

        var rolesRequested = Set.of("ROLE1", "ROLE2");

        var outcome = roleRequestService.updateRequest(
                roleRequestInput.getId(), RoleRequestStatusEnum.REJECTED, rolesRequested);

        assertThat(outcome).isInstanceOf(RoleRequestService.UpdateRoleRequestOutcome.NotFound.class);

        var notFoundOutcome = (RoleRequestService.UpdateRoleRequestOutcome.NotFound) outcome;

        assertThat(notFoundOutcome.requestId()).isEqualTo(roleRequestInput.getId());
        assertThat(notFoundOutcome.roleCode()).containsExactlyInAnyOrderElementsOf(rolesRequested);
        assertThat(notFoundOutcome.status()).isEqualTo(RoleRequestStatusEnum.REJECTED);

        verify(roleRequestRepository, never()).save(any());
    }

    @Test
    void updateRoleRequest_statusUnchanged() {

        var roleRequestInput = a(RoleRequest.class);

        var roleRequestEntity =
                roleRequestMapper.toRoleRequestEntity(roleRequestInput, "test@example.com", "reviewer1@example.com");

        roleRequestEntity.setStatus(RoleRequestStatusEnum.REJECTED);

        given(roleRequestRepository.findById(roleRequestInput.getId())).willReturn(Optional.of(roleRequestEntity));

        var rolesRequested = Set.of("ROLE1", "ROLE2");

        var outcome = roleRequestService.updateRequest(
                roleRequestInput.getId(), RoleRequestStatusEnum.REJECTED, rolesRequested);

        assertThat(outcome).isInstanceOf(RoleRequestService.UpdateRoleRequestOutcome.StatusNotChanged.class);

        var statusNotChangedOutcome = (RoleRequestService.UpdateRoleRequestOutcome.StatusNotChanged) outcome;

        assertThat(statusNotChangedOutcome.requestId()).isEqualTo(roleRequestInput.getId());
        assertThat(statusNotChangedOutcome.roleCode()).containsExactlyInAnyOrderElementsOf(rolesRequested);
        assertThat(statusNotChangedOutcome.status()).isEqualTo(RoleRequestStatusEnum.REJECTED);

        verify(roleRequestRepository, never()).save(any());
    }

    @Test
    void updateRoleRequest_inputStatusNotOpen() {

        var roleRequestInput = a(RoleRequest.class);

        var roleRequestEntity =
                roleRequestMapper.toRoleRequestEntity(roleRequestInput, "test@example.com", "reviewer1@example.com");

        roleRequestEntity.setStatus(RoleRequestStatusEnum.REJECTED);

        var rolesRequested = Set.of("ROLE1", "ROLE2");

        var outcome =
                roleRequestService.updateRequest(roleRequestInput.getId(), RoleRequestStatusEnum.OPEN, rolesRequested);

        assertThat(outcome).isInstanceOf(RoleRequestService.UpdateRoleRequestOutcome.InvalidInputStatus.class);

        assertThat(roleRequestEntity.getStatus()).isEqualTo(RoleRequestStatusEnum.REJECTED);

        var invalidInputStatusOutcome = (RoleRequestService.UpdateRoleRequestOutcome.InvalidInputStatus) outcome;

        assertThat(invalidInputStatusOutcome.requestId()).isEqualTo(roleRequestInput.getId());
        assertThat(invalidInputStatusOutcome.roleCode()).containsExactlyInAnyOrderElementsOf(rolesRequested);
        assertThat(invalidInputStatusOutcome.status()).isEqualTo(RoleRequestStatusEnum.OPEN);

        verifyNoInteractions(roleRequestRepository);
    }

    @Test
    void updateRoleRequest_roleRequestStatusNotOpen() {

        var roleRequestInput = a(RoleRequest.class);

        var roleRequestEntity =
                roleRequestMapper.toRoleRequestEntity(roleRequestInput, "test@example.com", "reviewer1@example.com");

        roleRequestEntity.setStatus(RoleRequestStatusEnum.REJECTED);

        given(roleRequestRepository.findById(roleRequestInput.getId())).willReturn(Optional.of(roleRequestEntity));

        var rolesRequested = Set.of("ROLE1", "ROLE2");

        var outcome = roleRequestService.updateRequest(
                roleRequestInput.getId(), RoleRequestStatusEnum.APPROVED, rolesRequested);

        assertThat(outcome).isInstanceOf(RoleRequestService.UpdateRoleRequestOutcome.AlreadyProcessed.class);

        var alreadyProcessedOutcome = (RoleRequestService.UpdateRoleRequestOutcome.AlreadyProcessed) outcome;

        assertThat(roleRequestEntity.getStatus()).isEqualTo(RoleRequestStatusEnum.REJECTED);

        assertThat(alreadyProcessedOutcome.requestId()).isEqualTo(roleRequestInput.getId());
        assertThat(alreadyProcessedOutcome.roleCode()).containsExactlyInAnyOrderElementsOf(rolesRequested);
        assertThat(alreadyProcessedOutcome.status()).isEqualTo(RoleRequestStatusEnum.APPROVED);

        verify(roleRequestRepository, never()).save(any());
    }

    @Test
    void updateRoleRequest_rolesNotFound() {

        var roleRequestInput = a(RoleRequest.class);

        var roleRequestEntity =
                roleRequestMapper.toRoleRequestEntity(roleRequestInput, "test@example.com", "reviewer1@example.com");

        roleRequestEntity.setStatus(RoleRequestStatusEnum.OPEN);

        given(roleRequestRepository.findById(roleRequestInput.getId())).willReturn(Optional.of(roleRequestEntity));

        var rolesRequested = Set.of("ROLE1", "ROLE2");

        rolesRequested.forEach(role -> given(roleService.findRoleBy(new RoleService.FindByArgs.FindByCode(role)))
                .willReturn(new RoleService.FindRoleByOutcome.NotFound(new RoleService.RoleIdentifier.Code(role))));

        var outcome = roleRequestService.updateRequest(
                roleRequestInput.getId(), RoleRequestStatusEnum.APPROVED, rolesRequested);

        assertThat(outcome).isInstanceOf(RoleRequestService.UpdateRoleRequestOutcome.RolesNotFound.class);

        var rolesNotFoundOutcome = (RoleRequestService.UpdateRoleRequestOutcome.RolesNotFound) outcome;

        assertThat(roleRequestEntity.getStatus()).isEqualTo(RoleRequestStatusEnum.OPEN);

        assertThat(rolesNotFoundOutcome.requestId()).isEqualTo(roleRequestInput.getId());
        assertThat(rolesNotFoundOutcome.roleCode()).containsExactlyInAnyOrderElementsOf(rolesRequested);
        assertThat(rolesNotFoundOutcome.status()).isEqualTo(RoleRequestStatusEnum.APPROVED);

        verify(roleRequestRepository, never()).save(any());
    }

    @Test
    void updateRoleRequest_ApprovedRolesAssignedEmpty() {

        var roleRequestInput = a(RoleRequest.class);

        var roleRequestEntity =
                roleRequestMapper.toRoleRequestEntity(roleRequestInput, "test@example.com", "reviewer1@example.com");

        roleRequestEntity.setStatus(RoleRequestStatusEnum.OPEN);

        given(roleRequestRepository.findById(roleRequestInput.getId())).willReturn(Optional.of(roleRequestEntity));

        var outcome = roleRequestService.updateRequest(
                roleRequestInput.getId(), RoleRequestStatusEnum.APPROVED, Collections.emptySet());

        assertThat(outcome).isInstanceOf(RoleRequestService.UpdateRoleRequestOutcome.RolesAssignedEmpty.class);

        var rolesAssignedEmptyOutcome = (RoleRequestService.UpdateRoleRequestOutcome.RolesAssignedEmpty) outcome;

        assertThat(roleRequestEntity.getStatus()).isEqualTo(RoleRequestStatusEnum.OPEN);

        assertThat(rolesAssignedEmptyOutcome.requestId()).isEqualTo(roleRequestInput.getId());
        assertThat(rolesAssignedEmptyOutcome.roleCode()).isEmpty();
        assertThat(rolesAssignedEmptyOutcome.status()).isEqualTo(RoleRequestStatusEnum.APPROVED);

        verify(roleRequestRepository, never()).save(any());
    }

    @Test
    void updateRoleRequest_rejectedSuccess() {

        var roleRequestInput = a(RoleRequest.class);

        var roleRequestEntity =
                roleRequestMapper.toRoleRequestEntity(roleRequestInput, "test@example.com", "reviewer1@example.com");

        roleRequestEntity.setStatus(RoleRequestStatusEnum.OPEN);

        given(roleRequestRepository.findById(roleRequestInput.getId())).willReturn(Optional.of(roleRequestEntity));

        var role1 = a(Role.class);
        var role2 = a(Role.class);

        given(roleService.findRoleBy(new RoleService.FindByArgs.FindByCode(role1.getCode())))
                .willReturn(new RoleService.FindRoleByOutcome.Success(role1));

        given(roleService.findRoleBy(new RoleService.FindByArgs.FindByCode(role2.getCode())))
                .willReturn(new RoleService.FindRoleByOutcome.Success(role2));

        var rolesRequested = Set.of(role1.getCode(), role2.getCode());

        var requestedRole1 =
                roleRequestMapper.toRoleRequestedEntity(roleRequestInput, role1.getCode(), roleRequestEntity);

        var requestedRole2 =
                roleRequestMapper.toRoleRequestedEntity(roleRequestInput, role2.getCode(), roleRequestEntity);

        roleRequestEntity.addRoleRequested(requestedRole1);

        roleRequestEntity.addRoleRequested(requestedRole2);

        var roleRequestUpdated = roleRequestMapper.toRoleRequest(roleRequestEntity);

        roleRequestUpdated.setStatus(RoleRequestStatusEnum.REJECTED);

        var outcome = roleRequestService.updateRequest(
                roleRequestInput.getId(), RoleRequestStatusEnum.REJECTED, rolesRequested);

        assertThat(outcome).isInstanceOf(RoleRequestService.UpdateRoleRequestOutcome.Success.class);

        var successOutcome = (RoleRequestService.UpdateRoleRequestOutcome.Success) outcome;

        verify(roleRequestRepository).flush();

        assertThat(successOutcome.requestId()).isEqualTo(roleRequestInput.getId());
        assertThat(successOutcome.roleCode()).containsExactlyInAnyOrderElementsOf(rolesRequested);
        assertThat(successOutcome.status()).isEqualTo(RoleRequestStatusEnum.REJECTED);

        assertThat(successOutcome.updatedRoleRequest().getId()).isEqualTo(roleRequestEntity.getId());

        assertThat(successOutcome.updatedRoleRequest().getStatus()).isEqualTo(RoleRequestStatusEnum.REJECTED);
    }

    @Test
    void updateRoleRequest_approvedSuccess() {

        var roleRequestInput = a(RoleRequest.class);

        var roleRequestEntity =
                roleRequestMapper.toRoleRequestEntity(roleRequestInput, "test@example.com", "reviewer1@example.com");

        roleRequestEntity.setStatus(RoleRequestStatusEnum.OPEN);

        given(roleRequestRepository.findById(roleRequestInput.getId())).willReturn(Optional.of(roleRequestEntity));

        var role1 = a(Role.class);
        var role2 = a(Role.class);

        given(roleService.findRoleBy(new RoleService.FindByArgs.FindByCode(role1.getCode())))
                .willReturn(new RoleService.FindRoleByOutcome.Success(role1));

        given(roleService.findRoleBy(new RoleService.FindByArgs.FindByCode(role2.getCode())))
                .willReturn(new RoleService.FindRoleByOutcome.Success(role2));

        var rolesRequested = Set.of(role1.getCode(), role2.getCode());

        var userEmail = "a.b@example.com";

        roleRequestEntity.setUserEmail(userEmail);

        var user = a(User.class);

        user.setRoles(List.of(role1.getCode(), "ROLE3"));

        var rolesUnion = CollectionsUtil.union(rolesRequested, new HashSet<>(user.getRoles()));

        var requestedRole1 =
                roleRequestMapper.toRoleRequestedEntity(roleRequestInput, role1.getCode(), roleRequestEntity);

        var requestedRole2 =
                roleRequestMapper.toRoleRequestedEntity(roleRequestInput, role2.getCode(), roleRequestEntity);

        roleRequestEntity.addRoleRequested(requestedRole1);

        roleRequestEntity.addRoleRequested(requestedRole2);

        given(userService.getUserByEmail(roleRequestEntity.getUserEmail()))
                .willReturn(new UserService.GetUserOutcome.Success(user));

        var roleRequestUpdated = roleRequestMapper.toRoleRequest(roleRequestEntity);

        roleRequestUpdated.setStatus(RoleRequestStatusEnum.APPROVED);

        given(userService.updateUserRoles(
                        new UserService.UpdateUserRolesArgs.WithRoleNames(user.getId(), new ArrayList<>(rolesUnion))))
                .willReturn(new UserService.UpdateUserOutcome.Success());

        var outcome = roleRequestService.updateRequest(
                roleRequestInput.getId(), RoleRequestStatusEnum.APPROVED, rolesRequested);

        assertThat(outcome).isInstanceOf(RoleRequestService.UpdateRoleRequestOutcome.Success.class);

        var successOutcome = (RoleRequestService.UpdateRoleRequestOutcome.Success) outcome;

        verify(roleRequestRepository).flush();

        assertThat(successOutcome.requestId()).isEqualTo(roleRequestInput.getId());
        assertThat(successOutcome.roleCode()).containsExactlyInAnyOrderElementsOf(rolesRequested);
        assertThat(successOutcome.status()).isEqualTo(RoleRequestStatusEnum.APPROVED);

        assertThat(successOutcome.updatedRoleRequest().getId()).isEqualTo(roleRequestEntity.getId());

        assertThat(successOutcome.updatedRoleRequest().getStatus()).isEqualTo(RoleRequestStatusEnum.APPROVED);
    }

    @Test
    void updateRoleRequest_approvedOtherRolesAssignedSuccess() {

        var user = a(User.class);

        var roleRequestInput = a(RoleRequest.class);

        var roleRequestEntity =
                roleRequestMapper.toRoleRequestEntity(roleRequestInput, user.getEmail(), "reviewer1@example.com");

        roleRequestEntity.setStatus(RoleRequestStatusEnum.OPEN);

        given(roleRequestRepository.findById(roleRequestInput.getId())).willReturn(Optional.of(roleRequestEntity));

        var roleAssigned1 = a(Role.class);
        var roleAssigned2 = a(Role.class);
        roleAssigned1.setCode("ROLE_ASSIGNED_1");
        roleAssigned2.setCode("ROLE_ASSIGNED_2");
        roleAssigned1.setName("ROLE_ASSIGNED_1");
        roleAssigned2.setName("ROLE_ASSIGNED_2");

        var roleRequested = a(RoleRequestedEntity.class);
        roleRequested.setRequestedBy(user.getEmail());
        roleRequested.setRoleRequest(roleRequestEntity);
        roleRequested.setApproved(false);

        roleRequestEntity.setRolesRequested(new HashSet<>(List.of(roleRequested)));

        given(roleService.findRoleBy(new RoleService.FindByArgs.FindByCode(roleAssigned1.getCode())))
                .willReturn(new RoleService.FindRoleByOutcome.Success(roleAssigned1));

        given(roleService.findRoleBy(new RoleService.FindByArgs.FindByCode(roleAssigned2.getCode())))
                .willReturn(new RoleService.FindRoleByOutcome.Success(roleAssigned2));

        var rolesAssigned = Set.of("ROLE_ASSIGNED_1", "ROLE_ASSIGNED_2");

        given(authService.getEmail()).willReturn("reviewer1@example.com");

        user.setRoles(List.of("ROLE3"));

        var rolesUnion = CollectionsUtil.union(rolesAssigned, new HashSet<>(user.getRoles()));

        given(userService.getUserByEmail(roleRequestEntity.getUserEmail()))
                .willReturn(new UserService.GetUserOutcome.Success(user));

        var roleRequestUpdated = roleRequestMapper.toRoleRequest(roleRequestEntity);

        roleRequestUpdated.setStatus(RoleRequestStatusEnum.APPROVED);

        given(userService.updateUserRoles(
                        new UserService.UpdateUserRolesArgs.WithRoleNames(user.getId(), new ArrayList<>(rolesUnion))))
                .willReturn(new UserService.UpdateUserOutcome.Success());

        var outcome = roleRequestService.updateRequest(
                roleRequestInput.getId(), RoleRequestStatusEnum.APPROVED, rolesAssigned);

        assertThat(outcome).isInstanceOf(RoleRequestService.UpdateRoleRequestOutcome.Success.class);

        var successOutcome = (RoleRequestService.UpdateRoleRequestOutcome.Success) outcome;

        verify(roleRequestRepository).flush();

        assertThat(successOutcome.requestId()).isEqualTo(roleRequestInput.getId());
        assertThat(successOutcome.roleCode()).containsExactlyInAnyOrderElementsOf(rolesAssigned);
        assertThat(successOutcome.status()).isEqualTo(RoleRequestStatusEnum.APPROVED);

        assertThat(successOutcome.updatedRoleRequest().getId()).isEqualTo(roleRequestEntity.getId());
        assertThat(successOutcome.updatedRoleRequest().getRolesRequested())
                .containsExactlyInAnyOrderElementsOf(Set.of(roleRequested.getRole()));
        assertThat(successOutcome.updatedRoleRequest().getRolesAssigned())
                .containsExactlyInAnyOrderElementsOf(rolesAssigned);
        assertThat(successOutcome.updatedRoleRequest().getStatus()).isEqualTo(RoleRequestStatusEnum.APPROVED);
    }

    @Test
    void searchRoleRequests_firstPageSuccess() {

        var filters = new RoleRequestFilter();
        filters.setEmail("s.d@email.com");
        filters.setStatus("OPEN");

        var pageable = PageRequest.of(0, 5);

        var entities = Instancio.ofList(RoleRequestEntity.class).size(10).create();

        entities.forEach(e -> {
            e.setRolesRequested(Set.of(a(RoleRequestedEntity.class)));
        });

        given(roleRequestRepository.findAll(any(Specification.class), eq(pageable)))
                .willReturn(new PageImpl<>(entities.subList(0, 5), pageable, entities.size()));

        var searchOutcome = roleRequestService.search(filters, pageable);

        var specificationArgCaptor = ArgumentCaptor.forClass(Specification.class);

        verify(roleRequestRepository).findAll(specificationArgCaptor.capture(), eq(pageable));

        var specification = specificationArgCaptor.getValue();

        assertThat(specificationArgCaptor.getValue()).isInstanceOf(RoleRequestSpecification.class);

        var roleRequestSpec = (RoleRequestSpecification) specification;

        assertThat(roleRequestSpec.getFilter().getEmail()).isEqualTo("s.d@email.com");
        assertThat(roleRequestSpec.getFilter().getStatus()).isEqualTo("OPEN");

        assertThat(searchOutcome).isInstanceOf(RoleRequestService.SearchRoleRequestsOutcome.Success.class);

        var successOutcome = (RoleRequestService.SearchRoleRequestsOutcome.Success) searchOutcome;

        assertThat(successOutcome.page().getContent()).isNotNull().hasSize(5);
        assertThat(successOutcome.page().getTotalPages()).isEqualTo(2);
        assertThat(successOutcome.page().getNumber()).isZero();
    }

    @Test
    void searchRoleRequests_secondPageSuccess() {

        var filters = new RoleRequestFilter();
        filters.setEmail("s.d@email.com");
        filters.setStatus("OPEN");

        var pageable = PageRequest.of(1, 5);

        var entities = Instancio.ofList(RoleRequestEntity.class).size(10).create();

        entities.forEach(e -> {
            e.setRolesRequested(Set.of(a(RoleRequestedEntity.class)));
        });

        given(roleRequestRepository.findAll(any(Specification.class), eq(pageable)))
                .willReturn(new PageImpl<>(entities.subList(5, 10), pageable, entities.size()));

        var searchOutcome = roleRequestService.search(filters, pageable);

        var specificationArgCaptor = ArgumentCaptor.forClass(Specification.class);

        verify(roleRequestRepository).findAll(specificationArgCaptor.capture(), eq(pageable));

        var specification = specificationArgCaptor.getValue();

        assertThat(specificationArgCaptor.getValue()).isInstanceOf(RoleRequestSpecification.class);

        var roleRequestSpec = (RoleRequestSpecification) specification;

        assertThat(roleRequestSpec.getFilter().getEmail()).isEqualTo("s.d@email.com");
        assertThat(roleRequestSpec.getFilter().getStatus()).isEqualTo("OPEN");

        assertThat(searchOutcome).isInstanceOf(RoleRequestService.SearchRoleRequestsOutcome.Success.class);

        var successOutcome = (RoleRequestService.SearchRoleRequestsOutcome.Success) searchOutcome;

        assertThat(successOutcome.page().getContent()).isNotNull().hasSize(5);
        assertThat(successOutcome.page().getTotalPages()).isEqualTo(2);
        assertThat(successOutcome.page().getNumber()).isOne();
    }

    @Test
    void searchRoleRequests_emptyPageSuccess() {

        var filters = new RoleRequestFilter();
        filters.setEmail("s.d@email.com");
        filters.setStatus("OPEN");

        var pageable = PageRequest.of(0, 5);

        given(roleRequestRepository.findAll(any(Specification.class), eq(pageable)))
                .willReturn(Page.empty(pageable));

        var searchOutcome = roleRequestService.search(filters, pageable);

        var specificationArgCaptor = ArgumentCaptor.forClass(Specification.class);

        verify(roleRequestRepository).findAll(specificationArgCaptor.capture(), eq(pageable));

        var specification = specificationArgCaptor.getValue();

        assertThat(specificationArgCaptor.getValue()).isInstanceOf(RoleRequestSpecification.class);

        var roleRequestSpec = (RoleRequestSpecification) specification;

        assertThat(roleRequestSpec.getFilter().getEmail()).isEqualTo("s.d@email.com");
        assertThat(roleRequestSpec.getFilter().getStatus()).isEqualTo("OPEN");

        assertThat(searchOutcome).isInstanceOf(RoleRequestService.SearchRoleRequestsOutcome.Success.class);

        var successOutcome = (RoleRequestService.SearchRoleRequestsOutcome.Success) searchOutcome;

        assertThat(successOutcome.page().getContent()).isNotNull().isEmpty();
        assertThat(successOutcome.page().getTotalPages()).isZero();
    }

    @Test
    void findBy_success_tier1manager() {

        var roleRequestEntity = a(RoleRequestEntity.class);

        var user = a(User.class);

        user.setRoles(List.of("ROLE_1", "T1UAR_M"));

        var roleRequestedEntity = a(RoleRequestedEntity.class);

        roleRequestEntity.setRolesRequested(Set.of(roleRequestedEntity));

        var roleRequest = roleRequestMapper.toRoleRequest(roleRequestEntity);

        given(authService.getEmail()).willReturn("test@example.com");

        given(userService.getUserByEmail("test@example.com")).willReturn(new UserService.GetUserOutcome.Success(user));

        given(roleRequestRepository.findWithRolesRequestedById(roleRequest.getId()))
                .willReturn(Optional.of(roleRequestEntity));

        var outcome = roleRequestService.findBy(roleRequestEntity.getId());

        verify(roleRequestRepository).findWithRolesRequestedById(roleRequest.getId());

        assertThat(outcome).isInstanceOf(RoleRequestService.FindByRoleRequestOutcome.Success.class);

        var successOutcome = (RoleRequestService.FindByRoleRequestOutcome.Success) outcome;

        assertThat(successOutcome.roleRequest().getId()).isEqualTo(roleRequestEntity.getId());

        assertThat(successOutcome.id()).isEqualTo(roleRequestEntity.getId());

        verify(roleRequestMapper, times(2)).toRoleRequest(roleRequestEntity);
    }

    @Test
    void findBy_success_sameOwner() {

        var roleRequestEntity = a(RoleRequestEntity.class);

        var user = a(User.class);

        user.setRoles(List.of("ROLE_1"));

        var roleRequestedEntity = a(RoleRequestedEntity.class);

        roleRequestEntity.setRolesRequested(Set.of(roleRequestedEntity));

        var roleRequest = roleRequestMapper.toRoleRequest(roleRequestEntity);

        given(authService.getEmail()).willReturn(roleRequestEntity.getUserEmail());

        given(userService.getUserByEmail(roleRequestEntity.getUserEmail()))
                .willReturn(new UserService.GetUserOutcome.Success(user));

        given(roleRequestRepository.findWithRolesRequestedById(roleRequest.getId()))
                .willReturn(Optional.of(roleRequestEntity));

        var outcome = roleRequestService.findBy(roleRequestEntity.getId());

        verify(roleRequestRepository).findWithRolesRequestedById(roleRequest.getId());

        assertThat(outcome).isInstanceOf(RoleRequestService.FindByRoleRequestOutcome.Success.class);

        var successOutcome = (RoleRequestService.FindByRoleRequestOutcome.Success) outcome;

        assertThat(successOutcome.roleRequest().getId()).isEqualTo(roleRequestEntity.getId());

        assertThat(successOutcome.id()).isEqualTo(roleRequestEntity.getId());

        verify(roleRequestMapper, times(2)).toRoleRequest(roleRequestEntity);
    }

    @Test
    void findBy_RequestNotFound() {

        var rqId = UUID.randomUUID();

        given(roleRequestRepository.findWithRolesRequestedById(rqId)).willReturn(Optional.empty());

        var outcome = roleRequestService.findBy(rqId);

        verify(roleRequestRepository).findWithRolesRequestedById(rqId);

        assertThat(outcome).isInstanceOf(RoleRequestService.FindByRoleRequestOutcome.RoleRequestNotFound.class);

        var notFoundOutcome = (RoleRequestService.FindByRoleRequestOutcome.RoleRequestNotFound) outcome;

        assertThat(notFoundOutcome.id()).isEqualTo(rqId);

        verifyNoInteractions(roleRequestMapper);
    }

    @Test
    void findBy_UserNotFound() {

        var roleRequestEntity = a(RoleRequestEntity.class);

        given(roleRequestRepository.findWithRolesRequestedById(roleRequestEntity.getId()))
                .willReturn(Optional.of(roleRequestEntity));

        var userEmail = "test@example.com";

        given(authService.getEmail()).willReturn(userEmail);

        given(userService.getUserByEmail(userEmail))
                .willReturn(new UserService.GetUserOutcome.NotFoundByEmail(userEmail));

        var outcome = roleRequestService.findBy(roleRequestEntity.getId());

        verify(roleRequestRepository).findWithRolesRequestedById(roleRequestEntity.getId());

        assertThat(outcome).isInstanceOf(RoleRequestService.FindByRoleRequestOutcome.UserNotFound.class);

        var notFoundOutcome = (RoleRequestService.FindByRoleRequestOutcome.UserNotFound) outcome;

        assertThat(notFoundOutcome.id()).isEqualTo(roleRequestEntity.getId());

        assertThat(notFoundOutcome.userEmail()).isEqualTo(userEmail);

        verifyNoInteractions(roleRequestMapper);
    }

    @Test
    void findBy_differentOwner() {

        var roleRequestEntity = a(RoleRequestEntity.class);

        var user = a(User.class);

        user.setRoles(List.of("ROLE_1", "ROLE_2"));

        var userEmail = roleRequestEntity.getUserEmail() + "test";

        given(authService.getEmail()).willReturn(userEmail);

        given(userService.getUserByEmail(userEmail)).willReturn(new UserService.GetUserOutcome.Success(user));

        given(roleRequestRepository.findWithRolesRequestedById(roleRequestEntity.getId()))
                .willReturn(Optional.of(roleRequestEntity));

        var outcome = roleRequestService.findBy(roleRequestEntity.getId());

        verify(roleRequestRepository).findWithRolesRequestedById(roleRequestEntity.getId());

        assertThat(outcome).isInstanceOf(RoleRequestService.FindByRoleRequestOutcome.DifferentCreator.class);

        var differentOwnerOutcome = (RoleRequestService.FindByRoleRequestOutcome.DifferentCreator) outcome;

        assertThat(differentOwnerOutcome.id()).isEqualTo(roleRequestEntity.getId());

        verifyNoInteractions(roleRequestMapper);
    }
}
