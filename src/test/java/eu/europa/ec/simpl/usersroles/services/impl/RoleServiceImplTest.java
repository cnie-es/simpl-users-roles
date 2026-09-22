package eu.europa.ec.simpl.usersroles.services.impl;

import static eu.europa.ec.simpl.common.test.TestUtil.*;
import static eu.europa.ec.simpl.usersroles.utils.KeycloakOperationMockUtil.mockGetRoleByName_Found;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.verify;

import eu.europa.ec.simpl.usersroles.adapters.RoleAdapter;
import eu.europa.ec.simpl.usersroles.entities.IdentityAttributeRole;
import eu.europa.ec.simpl.usersroles.entities.RoleEntity;
import eu.europa.ec.simpl.usersroles.exceptions.KeycloakException;
import eu.europa.ec.simpl.usersroles.models.Role;
import eu.europa.ec.simpl.usersroles.models.RoleFilter;
import eu.europa.ec.simpl.usersroles.repositories.IdentityAttributeRolesRepository;
import eu.europa.ec.simpl.usersroles.repositories.RoleRepository;
import eu.europa.ec.simpl.usersroles.repositories.specifications.RoleSpecification;
import eu.europa.ec.simpl.usersroles.services.IdentityAttributeService;
import eu.europa.ec.simpl.usersroles.services.RoleService;
import eu.europa.ec.simpl.usersroles.services.UserService;
import eu.europa.ec.simpl.usersroles.services.mappers.RoleMapperImpl;
import eu.europa.ec.simpl.usersroles.services.model.InvalidOutput;
import eu.europa.ec.simpl.usersroles.utils.KeycloakOperationMockUtil;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.instancio.Instancio;
import org.instancio.junit.InstancioSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class RoleServiceImplTest {

    @Mock
    IdentityAttributeRolesRepository identityAttributeRolesRepository;

    @Mock
    RoleRepository roleRepository;

    @Mock
    IdentityAttributeService identityAttributeService;

    @Mock
    UserService keycloakService;

    @Mock
    RoleAdapter roleAdapter;

    @Spy
    RoleMapperImpl roleMapper;

    @InjectMocks
    RoleServiceImpl roleService;

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void testFindRoleByUUID_Success(UUID uuid) {

        var role = an(Role.class);
        var idaList = aListOf(IdentityAttributeRole.class);
        role.setId(uuid);

        var roleEntityOpt = Optional.of(roleMapper.toRoleEntity(role));

        roleEntityOpt.get().setId(uuid);

        given(roleRepository.findById(uuid)).willReturn(roleEntityOpt);

        given(identityAttributeRolesRepository.findByRole_CodeAndEnabledTrue(anyString()))
                .willReturn(idaList);
        var getRoleOutcome = roleService.findRoleBy(new RoleService.FindByArgs.FindById(uuid));

        verify(roleRepository).findById(uuid);

        ArgumentCaptor<String> findByRoleNameAndEnabledTrueArgument = ArgumentCaptor.forClass(String.class);
        verify(identityAttributeRolesRepository)
                .findByRole_CodeAndEnabledTrue(findByRoleNameAndEnabledTrueArgument.capture());
        assertEquals(role.getCode(), findByRoleNameAndEnabledTrueArgument.getValue());

        switch (getRoleOutcome) {
            case RoleService.FindRoleByOutcome.Success outcome -> {
                assertThat(outcome.role().getId()).isEqualTo(role.getId());
                assertThat(outcome.role().getCode()).isEqualTo(role.getCode());
                assertThat(outcome.role().getName()).isEqualTo(role.getName());
                assertThat(outcome.role().getDescription()).isEqualTo(role.getDescription());
                assertThat(outcome.role().getAssignedIdentityAttributes())
                        .isEqualTo(idaList.stream()
                                .map(IdentityAttributeRole::getIdaCode)
                                .toList());
            }
            default -> fail("Unexpected value: " + getRoleOutcome);
        }
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void testFindRoleByUUID_NotFound(UUID uuid) {

        KeycloakOperationMockUtil.mockGetRoleById_NotFound(roleAdapter, uuid);

        var getRoleDtoOutcome = roleService.findRoleBy(new RoleService.FindByArgs.FindById(uuid));

        assertThat(getRoleDtoOutcome).isInstanceOf(RoleService.FindRoleByOutcome.NotFound.class);
    }

    @Test
    void testCreateRole_Success() {
        var input = a(Role.class);
        input.setBuiltIn(false);

        given(roleRepository.findByCode(anyString())).willReturn(Optional.empty());

        given(roleAdapter.createRole(input)).willReturn(new RoleAdapter.CreateKeycloakRoleOutcome.Success(input));

        var roleEntity = roleMapper.toRoleEntity(input);

        given(roleRepository.saveAndFlush(any(RoleEntity.class))).willReturn(roleEntity);

        var createRoleOutcome = roleService.create(input);

        verify(roleRepository).saveAndFlush(argThat(arg -> arg.getCode().equals(roleEntity.getCode())));

        verify(roleAdapter).createRole(input);

        assertThat(createRoleOutcome).isInstanceOfSatisfying(RoleService.CreateRoleOutcome.Success.class, outcome -> {
            assertThat(outcome.role().getCode()).isEqualTo(roleEntity.getCode());
            assertThat(outcome.role().getName()).isEqualTo(roleEntity.getName());
            assertThat(outcome.role().getDescription()).isEqualTo(roleEntity.getDescription());
            assertThat(outcome.role().getBuiltIn()).isEqualTo(roleEntity.getBuiltIn());
        });
    }

    @Test
    void testCreateRole_builtIn() {
        var input = a(Role.class);
        input.setBuiltIn(true);

        var createRoleOutcome = roleService.create(input);

        assertThat(createRoleOutcome)
                .isInstanceOfSatisfying(RoleService.CreateRoleOutcome.BuiltInRole.class, outcome -> {
                    assertThat(outcome.role()).isEqualTo(input);
                });
    }

    @Test
    void testCreateRole_duplicate() {

        var input = a(Role.class);
        input.setBuiltIn(false);
        var roleEntityOpt = Optional.of(roleMapper.toRoleEntity(input));

        given(roleRepository.findByCode(input.getCode())).willReturn(roleEntityOpt);

        var createRoleOutcome = roleService.create(input);

        assertThat(createRoleOutcome)
                .isInstanceOfSatisfying(RoleService.CreateRoleOutcome.Duplicated.class, outcome -> {
                    assertThat(outcome.role()).isEqualTo(input);
                });
    }

    @Test
    void testCreateRole_RoleAlreadySaved() {

        var existingRole = a(Role.class);

        existingRole.setBuiltIn(false);

        mockGetRoleByName_Found(roleAdapter, existingRole.getCode(), existingRole);

        var roleEntity = Optional.of(roleMapper.toRoleEntity(existingRole));

        given(roleRepository.findByCode(existingRole.getCode())).willReturn(roleEntity);

        var createRoleOutcome = roleService.create(existingRole);

        assertThat(createRoleOutcome).isInstanceOf(RoleService.CreateRoleOutcome.Duplicated.class);

        verify(roleRepository, never()).saveAndFlush(any(RoleEntity.class));
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void testUpdateRole_Success(UUID uuid, String roleName) {

        var idaRoles = aListOf(IdentityAttributeRole.class);
        var input = a(Role.class).setId(uuid).setName(roleName);
        input.setBuiltIn(false);

        var roleEntityOpt = Optional.of(roleMapper.toRoleEntity(input));

        given(roleRepository.findById(input.getId())).willReturn(roleEntityOpt);

        KeycloakOperationMockUtil.mockGetRoleById_Found(roleAdapter, uuid, input);

        KeycloakOperationMockUtil.mockUpdateRole_Success(roleAdapter, input);

        given(identityAttributeRolesRepository.findByRole_CodeAndEnabledTrue(anyString()))
                .willReturn(idaRoles);

        var updateRoleOutcome = roleService.update(input);

        verify(roleMapper).updateRole(roleEntityOpt.get(), input);

        verify(roleAdapter).updateRole(any(Role.class));

        assertThat(updateRoleOutcome).isInstanceOfSatisfying(RoleService.UpdateRoleOutcome.Success.class, outcome -> {
            assertThat(outcome.role().getId()).isEqualTo(input.getId());
            assertThat(outcome.role().getCode()).isEqualTo(input.getCode());
            assertThat(outcome.role().getDescription()).isEqualTo(input.getDescription());
            assertThat(outcome.role().getAssignedIdentityAttributes())
                    .isEqualTo(idaRoles.stream()
                            .map(IdentityAttributeRole::getIdaCode)
                            .toList());
        });
    }

    @Test
    void testUpdateRole_builtIn() {
        var input = a(Role.class);
        input.setBuiltIn(true);

        var roleEntity = Optional.of(roleMapper.toRoleEntity(input));

        given(roleRepository.findById(input.getId())).willReturn(roleEntity);

        var createRoleOutcome = roleService.update(input);

        assertThat(createRoleOutcome)
                .isInstanceOfSatisfying(RoleService.UpdateRoleOutcome.BuiltInRole.class, outcome -> {
                    assertThat(outcome.role()).isEqualTo(input);
                });
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void testUpdateRole_NotFoundKc(Role input) {

        KeycloakOperationMockUtil.mockGetRoleById_NotFound(roleAdapter, input.getId());

        KeycloakOperationMockUtil.mockUpdateRole_NotFound(roleAdapter, input.getCode());

        RoleService.UpdateRoleOutcome updateRoleOutcome = roleService.update(input);

        assertThat(updateRoleOutcome).isInstanceOf(RoleService.UpdateRoleOutcome.NotFound.class);
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void testUpdateRole_NotFoundDB(Role input) {

        given(roleRepository.findById(input.getId())).willReturn(Optional.empty());

        RoleService.UpdateRoleOutcome updateRoleOutcome = roleService.update(input);

        assertThat(updateRoleOutcome).isInstanceOf(RoleService.UpdateRoleOutcome.NotFound.class);
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void testUpdateRole_InvalidNameChange1(UUID uuid, String roleName) {

        var input = a(Role.class).setId(uuid).setName(roleName);

        input.setBuiltIn(false);

        var roleEntity = Optional.of(roleMapper.toRoleEntity(input));

        given(roleRepository.findById(input.getId())).willReturn(roleEntity);

        input.setCode(roleEntity.get().getCode() + "-changed");

        KeycloakOperationMockUtil.mockGetRoleById_Found(roleAdapter, uuid, input);

        var updateRoleOutcome = roleService.update(input);

        verify(roleRepository).findById(input.getId());

        verifyNoInteractions(roleAdapter);

        assertThat(updateRoleOutcome).isInstanceOf(RoleService.UpdateRoleOutcome.InvalidChangeNameAttempt.class);
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void testUpdateRole_InvalidNameChange2(UUID uuid, String roleName) {

        var input = a(Role.class).setId(uuid).setName(roleName);

        input.setBuiltIn(false);

        var roleEntity = Optional.of(roleMapper.toRoleEntity(input));

        given(roleRepository.findById(input.getId())).willReturn(roleEntity);

        KeycloakOperationMockUtil.mockGetRoleById_Found(roleAdapter, uuid, input);

        given(roleAdapter.updateRole(any()))
                .willReturn(new RoleAdapter.UpdateKeycloakRoleOutCome.InvalidInput(
                        "Invalid operation: cannot update role's name"));

        var updateRoleOutcome = roleService.update(input);

        then(roleAdapter).should().updateRole(any());

        assertThat(updateRoleOutcome).isInstanceOf(RoleService.UpdateRoleOutcome.InvalidChangeNameAttempt.class);
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void testDeleteRole_Success(UUID uuid) {

        var role = a(Role.class);
        role.setId(uuid);
        role.setBuiltIn(false);

        var roleEntityOpt = Optional.of(roleMapper.toRoleEntity(role));

        KeycloakOperationMockUtil.mockGetRoleById_Found(roleAdapter, uuid, role);

        given(roleRepository.findById(role.getId())).willReturn(roleEntityOpt);

        given(roleAdapter.isRoleAssigned(role.getCode()))
                .willReturn(new RoleAdapter.IsKeycloakRoleAssignedOutcome.Success(false));

        KeycloakOperationMockUtil.mockDeleteRole_Success(roleAdapter, role.getCode(), role);

        var deleteRoleOutcome = roleService.delete(uuid);

        verify(roleRepository).findById(role.getId());

        verify(roleRepository).delete(roleEntityOpt.get());

        verify(roleAdapter).deleteRole(new RoleAdapter.DeleteRoleArgs.DeleteByName(role.getCode()));

        then(identityAttributeRolesRepository).should().disableMappingByRoleCode(anyString());

        switch (deleteRoleOutcome) {
            case RoleService.DeleteRoleOutcome.Success outcome -> assertThat(outcome.uuid())
                    .isEqualTo(role.getId());
            default -> fail("Unexpected value: " + deleteRoleOutcome);
        }
    }

    @Test
    void testDeleteRole_builtIn() {
        var input = a(Role.class);
        input.setBuiltIn(true);

        var roleEntity = Optional.of(roleMapper.toRoleEntity(input));

        given(roleRepository.findById(input.getId())).willReturn(roleEntity);

        var createRoleOutcome = roleService.delete(input.getId());

        assertThat(createRoleOutcome)
                .isInstanceOfSatisfying(RoleService.DeleteRoleOutcome.BuiltInRole.class, outcome -> {
                    assertThat(outcome.uuid()).isEqualTo(input.getId());
                });
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void testDeleteRole_given_NotFoundKC(UUID uuid) {
        var role = a(Role.class);
        role.setId(uuid);
        KeycloakOperationMockUtil.mockDeleteRole_NotFound(roleAdapter, role.getCode());
        assertThat(roleService.delete(uuid)).isInstanceOf(RoleService.DeleteRoleOutcome.NotFound.class);
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void testDeleteRole_given_NotFoundDB(UUID uuid) {
        var role = a(Role.class);
        role.setId(uuid);
        given(roleRepository.findById(uuid)).willReturn(Optional.empty());
        assertThat(roleService.delete(uuid)).isInstanceOf(RoleService.DeleteRoleOutcome.NotFound.class);
    }

    @Test
    void testDeleteRoleIsAssigned() {

        UUID roleId = UUID.randomUUID();

        var role = new Role();
        role.setId(roleId);
        role.setName("TEST_ROLE_ASSIGNED");

        var roleEntityOpt = Optional.of(roleMapper.toRoleEntity(role));

        given(roleRepository.findById(role.getId())).willReturn(roleEntityOpt);

        KeycloakOperationMockUtil.mockGetRoleById_Found(roleAdapter, roleId, role);

        given(roleAdapter.isRoleAssigned(role.getCode()))
                .willReturn(new RoleAdapter.IsKeycloakRoleAssignedOutcome.Success(true));

        RoleService.DeleteRoleOutcome outcome = roleService.delete(roleId);

        assertThat(outcome).isInstanceOf(RoleService.DeleteRoleOutcome.IsAssigned.class);

        RoleService.DeleteRoleOutcome.IsAssigned isAssignedOutcome = (RoleService.DeleteRoleOutcome.IsAssigned) outcome;

        assertThat(isAssignedOutcome.uuid()).isEqualTo(roleId);

        verify(roleRepository, never()).delete(any(RoleEntity.class));

        verify(roleAdapter, never()).deleteRole(any());

        verify(identityAttributeRolesRepository, never()).disableMappingByRoleCode(anyString());
    }

    @Test
    void importRoles_Success() {
        List<Role> rolesToImport = Instancio.ofList(Role.class).create();

        rolesToImport.forEach(role -> {
            role.setEnabled(true);
            role.setBuiltIn(false);
            KeycloakOperationMockUtil.mockCreateRole_Success(roleAdapter, role);
        });

        RoleService.ImportRolesOutcome outcome = roleService.importRoles(rolesToImport);
        int rolesCount = rolesToImport.size();

        verify(roleRepository, times(rolesCount)).findByCode(any(String.class));
        verify(roleAdapter, times(rolesCount)).createRole(any(Role.class));
        verify(roleRepository, times(rolesCount)).saveAndFlush(any(RoleEntity.class));

        switch (outcome) {
            case RoleService.ImportRolesOutcome.Success success -> assertEquals(success.roles(), rolesToImport);
            default -> fail("Unexpected value: " + outcome);
        }
    }

    @Test
    void importRoles_Duplicated1() {

        var role1 = a(Role.class);
        var role2 = a(Role.class);

        var rolesToImport = List.of(role1, role2);

        rolesToImport.forEach(role -> {
            role.setEnabled(true);
            role.setBuiltIn(false);
        });

        var expected = List.of(role1);

        var roleEntityOpt = Optional.of(roleMapper.toRoleEntity(role2));

        given(roleRepository.findByCode(role2.getCode())).willReturn(roleEntityOpt);

        given(roleAdapter.createRole(role1)).willReturn(new RoleAdapter.CreateKeycloakRoleOutcome.Success(role1));

        RoleService.ImportRolesOutcome outcome = roleService.importRoles(rolesToImport);

        verify(roleAdapter).createRole(role1);

        verify(roleAdapter, never()).createRole(role2);

        switch (outcome) {
            case RoleService.ImportRolesOutcome.Success success -> assertEquals(success.roles(), expected);
            default -> fail("Unexpected value: " + outcome);
        }
    }

    @Test
    void importRoles_Duplicated2() {

        var role1 = a(Role.class);
        var role2 = a(Role.class);

        var rolesToImport = List.of(role1, role2);

        rolesToImport.forEach(role -> {
            role.setEnabled(true);
            role.setBuiltIn(false);
        });

        var expected = List.of(role1);

        given(roleAdapter.createRole(role2)).willReturn(new RoleAdapter.CreateKeycloakRoleOutcome.Duplicated(role2));

        var successOutcome = new RoleAdapter.CreateKeycloakRoleOutcome.Success(role1);
        var duplicatedOutcome = new RoleAdapter.CreateKeycloakRoleOutcome.Duplicated(role2);

        given(roleAdapter.createRole(any())).willReturn(successOutcome).willReturn(duplicatedOutcome);

        RoleService.ImportRolesOutcome outcome = roleService.importRoles(rolesToImport);

        verify(roleAdapter).createRole(role1);

        verify(roleAdapter).createRole(role2);

        switch (outcome) {
            case RoleService.ImportRolesOutcome.Success success -> assertEquals(success.roles(), expected);
            default -> fail("Unexpected value: " + outcome);
        }
    }

    @Test
    void importRoles_builtIn() {

        var role1 = a(Role.class);
        var role2 = a(Role.class);

        var rolesToImport = List.of(role1, role2);

        role1.setBuiltIn(false);
        role2.setBuiltIn(true);

        var expected = List.of(role1);

        given(roleAdapter.createRole(role1)).willReturn(new RoleAdapter.CreateKeycloakRoleOutcome.Success(role1));

        RoleService.ImportRolesOutcome outcome = roleService.importRoles(rolesToImport);

        verify(roleRepository)
                .saveAndFlush(argThat(arg -> arg != null && role1.getCode().equals(arg.getCode())));

        verify(roleRepository, never())
                .saveAndFlush(argThat(arg -> arg != null && role2.getCode().equals(arg.getCode())));

        verify(roleAdapter).createRole(role1);

        verify(roleAdapter, never()).createRole(role2);

        switch (outcome) {
            case RoleService.ImportRolesOutcome.Success success -> assertEquals(success.roles(), expected);
            default -> fail("Unexpected value: " + outcome);
        }
    }

    @Test
    void importRoles_Failure_rollback() {
        List<Role> rolesToImport = Instancio.ofList(Role.class).create();
        Role lastRole = rolesToImport.getLast();
        lastRole.setBuiltIn(false);

        rolesToImport.stream().filter(role -> !role.equals(lastRole)).forEach(role -> {
            role.setBuiltIn(false);
            KeycloakOperationMockUtil.mockCreateRole_Success(roleAdapter, role);
        });

        given(roleAdapter.createRole(argThat(arg -> arg.getId().equals(lastRole.getId()))))
                .willThrow(KeycloakException.class);

        var deleteCaptor = ArgumentCaptor.forClass(RoleAdapter.DeleteRoleArgs.DeleteByName.class);

        assertThrows(KeycloakException.class, () -> roleService.importRoles(rolesToImport));

        verify(roleAdapter, times(rolesToImport.size())).createRole(any(Role.class));

        verify(roleAdapter, times(rolesToImport.size() - 1)).deleteRole(deleteCaptor.capture());

        rolesToImport.stream().filter(role -> !role.equals(lastRole)).forEach(role -> {
            verify(roleAdapter).deleteRole(new RoleAdapter.DeleteRoleArgs.DeleteByName(role.getCode()));
        });

        verify(roleAdapter, never()).deleteRole(new RoleAdapter.DeleteRoleArgs.DeleteById(lastRole.getId()));

        var deletedCodes = deleteCaptor.getAllValues().stream()
                .map(RoleAdapter.DeleteRoleArgs.DeleteByName::name)
                .toList();

        var expectedCodes = rolesToImport.stream()
                .filter(role -> !role.equals(lastRole))
                .map(Role::getCode)
                .toList();

        assertThat(deletedCodes).containsExactlyInAnyOrderElementsOf(expectedCodes);
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void assignIdentityAttributes_replaceNewIdentityAttributesRoles_success(
            UUID uuid, UUID replace, List<String> codes) {
        var attributes = codes.stream()
                .map(code -> Instancio.create(IdentityAttributeRole.class).setIdaCode(code))
                .toList();
        var roleEntity = a(RoleEntity.class);
        roleEntity.setId(uuid);

        given(roleRepository.findById(replace)).willReturn(Optional.of(roleEntity));

        given(identityAttributeRolesRepository.saveAll(anyList())).willReturn(attributes);
        given(identityAttributeService.validateIdentityAttributesAssignableToRoles(anyList()))
                .willReturn(new IdentityAttributeService.ValidateIdentityAttributeOutcome.Success());

        var replaceIdentityAttributesOutcome = roleService.replaceIdentityAttributes(replace, codes);

        then(identityAttributeRolesRepository).should().saveAll(anyList());

        switch (replaceIdentityAttributesOutcome) {
            case RoleService.ReplaceIdentityAttributesOutcome.Success outcome -> assertThat(
                            outcome.identityAttributes())
                    .isEqualTo(codes);
            default -> fail("Unexpected value: " + replaceIdentityAttributesOutcome);
        }
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void replaceIdentityAttributes_notExistRole(UUID uuid, List<String> codes) {

        KeycloakOperationMockUtil.mockGetRoleById_NotFound(roleAdapter, uuid);

        given(identityAttributeService.validateIdentityAttributesAssignableToRoles(anyList()))
                .willReturn(new IdentityAttributeService.ValidateIdentityAttributeOutcome.Success());
        var replaceIdentityAttributesOutcome = roleService.replaceIdentityAttributes(uuid, codes);

        assertThat(replaceIdentityAttributesOutcome)
                .isInstanceOf(RoleService.ReplaceIdentityAttributesOutcome.RoleNotFound.class);
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void replaceIdentityAttributes_notValidated(UUID uuid, List<String> codes) {
        var attributes = codes.stream()
                .map(code -> Instancio.create(IdentityAttributeRole.class).setIdaCode(code))
                .toList();
        var role = a(Role.class);
        role.setId(uuid);

        var roleEntity = Optional.of(roleMapper.toRoleEntity(role));

        given(roleRepository.findById(uuid)).willReturn(roleEntity);

        KeycloakOperationMockUtil.mockGetRoleById_Found(roleAdapter, uuid, role);

        given(identityAttributeRolesRepository.saveAll(anyList())).willReturn(attributes);
        given(identityAttributeService.validateIdentityAttributesAssignableToRoles(anyList()))
                .willReturn(new IdentityAttributeService.ValidateIdentityAttributeOutcome.Invalid(
                        List.of(new InvalidOutput.NotAssignableToRole("code"))));
        var replaceIdentityAttributesOutcome = roleService.replaceIdentityAttributes(uuid, codes);

        assertThat(replaceIdentityAttributesOutcome)
                .isInstanceOf(RoleService.ReplaceIdentityAttributesOutcome.InvalidIdentityAttributes.class);
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void replaceIdentityAttributes_invalidIdentityAttributes(UUID uuid, List<String> codes) {
        var role = a(Role.class);
        role.setId(uuid);

        var roleEntityOpt = Optional.of(roleMapper.toRoleEntity(role));

        given(roleRepository.findById(role.getId())).willReturn(roleEntityOpt);

        KeycloakOperationMockUtil.mockGetRoleById_Found(roleAdapter, uuid, role);

        given(identityAttributeService.validateIdentityAttributesAssignableToRoles(anyList()))
                .willReturn(new IdentityAttributeService.ValidateIdentityAttributeOutcome.Invalid(
                        List.of(new InvalidOutput.NotFound("code"))));
        var replaceIdentityAttributesOutcome = roleService.replaceIdentityAttributes(uuid, codes);

        assertThat(replaceIdentityAttributesOutcome)
                .isInstanceOf(RoleService.ReplaceIdentityAttributesOutcome.InvalidIdentityAttributes.class);

        verify(identityAttributeService).validateIdentityAttributesAssignableToRoles(codes);
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void removeAttributeRoleMapping(UUID uuid) {
        var role = a(Role.class);
        role.setId(uuid);
        KeycloakOperationMockUtil.mockGetRoleById_Found(roleAdapter, uuid, role);

        var roleEntity = Optional.of(roleMapper.toRoleEntity(role));

        given(roleRepository.findById(role.getId())).willReturn(roleEntity);

        var attributeRoles = a(IdentityAttributeRole.class);
        given(identityAttributeRolesRepository.deleteByRole_CodeAndIdaCode(anyString(), anyString()))
                .willReturn(1L);

        var removeAttributeForRoleOutcome =
                roleService.removeAttributeForRole(attributeRoles.getIdaCode(), role.getId());

        then(identityAttributeRolesRepository)
                .should()
                .deleteByRole_CodeAndIdaCode(role.getCode(), attributeRoles.getIdaCode());

        assertThat(removeAttributeForRoleOutcome).isInstanceOf(RoleService.RemoveAttributeForRoleOutcome.Success.class);
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void removeAttributeRoleMapping_roleNotFound(UUID uuid) {
        var role = a(Role.class);
        role.setId(uuid);
        KeycloakOperationMockUtil.mockGetRoleById_Found(roleAdapter, uuid, role);

        given(roleRepository.findById(role.getId())).willReturn(Optional.empty());

        var attributeRoles = a(IdentityAttributeRole.class);
        given(identityAttributeRolesRepository.deleteByRole_CodeAndIdaCode(anyString(), anyString()))
                .willReturn(1L);

        var removeAttributeForRoleOutcome =
                roleService.removeAttributeForRole(attributeRoles.getIdaCode(), role.getId());

        verifyNoInteractions(identityAttributeRolesRepository);

        assertThat(removeAttributeForRoleOutcome)
                .isInstanceOfSatisfying(RoleService.RemoveAttributeForRoleOutcome.NotFound.class, notFound -> {
                    assertThat(notFound.uuid()).isEqualTo(uuid);
                });
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void removeAttributeRoleMappingNotFound(UUID uuid) {
        var role = a(Role.class);
        role.setId(uuid);

        KeycloakOperationMockUtil.mockGetRoleById_Found(roleAdapter, uuid, role);

        var roleEntity = Optional.of(roleMapper.toRoleEntity(role));

        given(roleRepository.findById(role.getId())).willReturn(roleEntity);

        given(identityAttributeRolesRepository.deleteByRole_CodeAndIdaCode(anyString(), anyString()))
                .willReturn(0L);
        var removeAttributeForRoleOutcome = roleService.removeAttributeForRole("ia1", uuid);
        then(identityAttributeRolesRepository).should().deleteByRole_CodeAndIdaCode(any(), any());

        assertThat(removeAttributeForRoleOutcome)
                .isInstanceOf(RoleService.RemoveAttributeForRoleOutcome.NotFound.class);
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void duplicateIdentityAttributeToAnOtherRole_existingRoles_success(UUID sourceId, UUID destinationId) {
        var attributeRolesSourceList = aListOf(IdentityAttributeRole.class);
        var attributeRolesDestinationList = attributeRolesSourceList.subList(0, 1);
        var source = a(Role.class);
        source.setId(sourceId);
        var destination = a(Role.class);
        destination.setId(destinationId);

        var sourceRoleEntity = Optional.of(roleMapper.toRoleEntity(source));

        given(roleRepository.findById(source.getId())).willReturn(sourceRoleEntity);

        var destRoleEntity = Optional.of(roleMapper.toRoleEntity(destination));

        given(roleRepository.findById(destination.getId())).willReturn(destRoleEntity);

        KeycloakOperationMockUtil.mockGetRoleById_Found(roleAdapter, sourceId, source);

        var roleEntity = Optional.of(roleMapper.toRoleEntity(source));

        given(roleRepository.findById(source.getId())).willReturn(roleEntity);

        given(identityAttributeRolesRepository.findByRole_CodeAndEnabledTrue(source.getCode()))
                .willReturn(attributeRolesSourceList);

        KeycloakOperationMockUtil.mockGetRoleById_Found(roleAdapter, destinationId, destination);

        given(identityAttributeRolesRepository.findByRole_CodeAndEnabledTrue(destination.getCode()))
                .willReturn(attributeRolesDestinationList);

        var duplicateIdentityAttributeToAnOtherRoleOutcome =
                roleService.duplicateIdentityAttributeToAnOtherRole(sourceId, destinationId);

        verify(roleRepository).findById(source.getId());
        verify(roleRepository).findById(destination.getId());
        then(identityAttributeRolesRepository).should(times(2)).findByRole_CodeAndEnabledTrue(anyString());
        then(identityAttributeRolesRepository)
                .should(times(attributeRolesSourceList.size() - attributeRolesDestinationList.size()))
                .save(any(IdentityAttributeRole.class));

        assertThat(duplicateIdentityAttributeToAnOtherRoleOutcome)
                .isInstanceOf(RoleService.DuplicateIdentityAttributeToAnOtherRoleOutcome.Success.class);
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void duplicateIdentityAttributeToAnOtherRole_noExistingSourceRole_throwRoleNotFoundException(
            UUID sourceId, UUID destinationId) {

        KeycloakOperationMockUtil.mockGetRoleById_NotFound(roleAdapter, sourceId);

        var duplicateIdentityAttributeToAnOtherRoleOutcome =
                roleService.duplicateIdentityAttributeToAnOtherRole(sourceId, destinationId);

        assertThat(duplicateIdentityAttributeToAnOtherRoleOutcome)
                .isInstanceOf(RoleService.DuplicateIdentityAttributeToAnOtherRoleOutcome.NotFound.class);
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void duplicateIdentityAttributeToAnOtherRole_noExistingDestinationRole_throwRoleNotFoundException(
            UUID sourceId, UUID destinationId) {
        var attributeRolesList = aListOf(IdentityAttributeRole.class);
        var roleRepresentation = a(Role.class);
        roleRepresentation.setId(sourceId);

        KeycloakOperationMockUtil.mockGetRoleById_Found(roleAdapter, sourceId, roleRepresentation);

        KeycloakOperationMockUtil.mockGetRoleById_NotFound(roleAdapter, destinationId);

        given(identityAttributeRolesRepository.findByRole_CodeAndEnabledTrue(anyString()))
                .willReturn(attributeRolesList);

        var duplicateIdentityAttributeToAnOtherRoleOutcome =
                roleService.duplicateIdentityAttributeToAnOtherRole(sourceId, destinationId);

        assertThat(duplicateIdentityAttributeToAnOtherRoleOutcome)
                .isInstanceOf(RoleService.DuplicateIdentityAttributeToAnOtherRoleOutcome.NotFound.class);
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void duplicateIdentityAttributeToAnOtherRole_noExistingDestinationRoleDB(UUID sourceId, UUID destinationId) {
        var attributeRolesList = aListOf(IdentityAttributeRole.class);
        var roleRepresentation = a(Role.class);
        roleRepresentation.setId(sourceId);

        var roleEntity = Optional.of(roleMapper.toRoleEntity(roleRepresentation));

        given(roleRepository.findById(sourceId)).willReturn(roleEntity);

        given(roleRepository.findById(destinationId)).willReturn(Optional.empty());

        KeycloakOperationMockUtil.mockGetRoleById_NotFound(roleAdapter, destinationId);

        given(identityAttributeRolesRepository.findByRole_CodeAndEnabledTrue(anyString()))
                .willReturn(attributeRolesList);

        var duplicateIdentityAttributeToAnOtherRoleOutcome =
                roleService.duplicateIdentityAttributeToAnOtherRole(sourceId, destinationId);

        assertThat(duplicateIdentityAttributeToAnOtherRoleOutcome)
                .isInstanceOfSatisfying(
                        RoleService.DuplicateIdentityAttributeToAnOtherRoleOutcome.NotFound.class, notFound -> {
                            assertThat(notFound.uuid()).isEqualTo(destinationId);
                        });
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void preAssignIdentityAttributesToRoleTest_success(List<String> idaCodes, Role r) {

        var roleEntity = Optional.of(roleMapper.toRoleEntity(r));

        given(roleRepository.findByCode(r.getCode())).willReturn(roleEntity);

        roleService.preAssignIdentityAttributesToRole(idaCodes, r.getCode());

        verify(identityAttributeRolesRepository).saveAll(argThat(mappings -> {
            var role = mappings.iterator().next();
            assertThat(role.getRole()).isNotNull();
            assertThat(role.getRole().getCode()).isEqualTo(r.getCode());
            assertThat(role.getIdaCode()).isEqualTo(idaCodes.getFirst());
            assertThat(role.getEnabled()).isFalse();
            return true;
        }));
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void findIdentityAttributesByRoleId(Role role) {

        var roleEntity = Optional.of(roleMapper.toRoleEntity(role));

        given(roleRepository.findById(role.getId())).willReturn(roleEntity);

        given(identityAttributeRolesRepository.findByRole_CodeAndEnabledTrue(role.getCode()))
                .willReturn(aListOf(IdentityAttributeRole.class));
        var outcome = roleService.findIdentityAttributesByRoleId(role.getId());

        verify(roleRepository).findById(role.getId());

        assertThat(outcome).isInstanceOf(RoleService.GetIdentityAttributesByRoleIdOutcome.Success.class);
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void findIdentityAttributesByRoleIdgivenNotFoundWillReturnNotFound(UUID roleId) {
        given(roleAdapter.getRoleById(roleId)).willReturn(Optional.empty());
        var outcome = roleService.findIdentityAttributesByRoleId(roleId);
        assertThat(outcome).isInstanceOf(RoleService.GetIdentityAttributesByRoleIdOutcome.NotFound.class);
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void testFindRoleByCode_Success(Role role) {

        var idaList = aListOf(IdentityAttributeRole.class);

        var roleEntityOpt = Optional.of(roleMapper.toRoleEntity(role));

        roleEntityOpt.get().setId(role.getId());

        given(roleRepository.findByCode(role.getCode())).willReturn(roleEntityOpt);

        given(identityAttributeRolesRepository.findByRole_CodeAndEnabledTrue(anyString()))
                .willReturn(idaList);
        var getRoleOutcome = roleService.findRoleBy(new RoleService.FindByArgs.FindByCode(role.getCode()));

        ArgumentCaptor<String> findByRoleNameAndEnabledTrueArgument = ArgumentCaptor.forClass(String.class);
        verify(identityAttributeRolesRepository)
                .findByRole_CodeAndEnabledTrue(findByRoleNameAndEnabledTrueArgument.capture());
        assertEquals(role.getCode(), findByRoleNameAndEnabledTrueArgument.getValue());

        switch (getRoleOutcome) {
            case RoleService.FindRoleByOutcome.Success outcome -> {
                assertThat(outcome.role().getId()).isEqualTo(role.getId());
                assertThat(outcome.role().getCode()).isEqualTo(role.getCode());
                assertThat(outcome.role().getName()).isEqualTo(role.getName());
                assertThat(outcome.role().getDescription()).isEqualTo(role.getDescription());
                assertThat(outcome.role().getAssignedIdentityAttributes())
                        .isEqualTo(idaList.stream()
                                .map(IdentityAttributeRole::getIdaCode)
                                .toList());
            }
            default -> fail("Unexpected value: " + getRoleOutcome);
        }
    }

    @ParameterizedTest
    @InstancioSource(samples = 1)
    void testFindRoleByCode_NotFound(String roleCode) {

        KeycloakOperationMockUtil.mockGetRoleByName_NotFound(roleAdapter, roleCode);

        var getRoleOutcome = roleService.findRoleBy(new RoleService.FindByArgs.FindByCode(roleCode));

        assertThat(getRoleOutcome).isInstanceOf(RoleService.FindRoleByOutcome.NotFound.class);
    }

    @Test
    void searchRole_firstPageSuccess() {

        var filters = new RoleFilter();
        filters.setCode("CODE1");

        var pageable = PageRequest.of(0, 5);

        var entities = Instancio.ofList(RoleEntity.class).size(10).create();

        given(roleRepository.findAll(any(Specification.class), eq(pageable)))
                .willReturn(new PageImpl<>(entities.subList(0, 5), pageable, entities.size()));

        var searchOutcome = roleService.search(filters, pageable);

        var specificationArgCaptor = ArgumentCaptor.forClass(Specification.class);

        verify(roleRepository).findAll(specificationArgCaptor.capture(), eq(pageable));

        var specification = specificationArgCaptor.getValue();

        assertThat(specificationArgCaptor.getValue()).isInstanceOf(RoleSpecification.class);

        var roleRequestSpec = (RoleSpecification) specification;

        assertThat(roleRequestSpec.getFilter().getCode()).isEqualTo("CODE1");

        assertThat(searchOutcome).isInstanceOfSatisfying(RoleService.SearchRolesOutcome.Success.class, success -> {
            assertThat(success.page().getContent()).isNotNull().hasSize(5);
            assertThat(success.page().getTotalPages()).isEqualTo(2);
            assertThat(success.page().getNumber()).isZero();
        });
    }

    @Test
    void searchRoleRequests_secondPageSuccess() {

        var filters = new RoleFilter();
        filters.setName(List.of("name1"));

        var pageable = PageRequest.of(1, 5);

        var entities = Instancio.ofList(RoleEntity.class).size(10).create();

        given(roleRepository.findAll(any(Specification.class), eq(pageable)))
                .willReturn(new PageImpl<>(entities.subList(5, 10), pageable, entities.size()));

        var searchOutcome = roleService.search(filters, pageable);

        var specificationArgCaptor = ArgumentCaptor.forClass(Specification.class);

        verify(roleRepository).findAll(specificationArgCaptor.capture(), eq(pageable));

        var specification = specificationArgCaptor.getValue();

        assertThat(specificationArgCaptor.getValue()).isInstanceOf(RoleSpecification.class);

        var roleRequestSpec = (RoleSpecification) specification;

        assertThat(roleRequestSpec.getFilter().getName()).isEqualTo(filters.getName());

        assertThat(searchOutcome).isInstanceOfSatisfying(RoleService.SearchRolesOutcome.Success.class, success -> {
            assertThat(success.page().getContent()).isNotNull().hasSize(5);
            assertThat(success.page().getTotalPages()).isEqualTo(2);
            assertThat(success.page().getNumber()).isOne();
        });
    }

    @Test
    void searchRoleRequests_emptyPageSuccess() {

        var filters = new RoleFilter();
        filters.setBuiltIn(true);
        filters.setDescription("Description");

        var pageable = PageRequest.of(0, 5);

        given(roleRepository.findAll(any(Specification.class), eq(pageable))).willReturn(Page.empty(pageable));

        var searchOutcome = roleService.search(filters, pageable);

        var specificationArgCaptor = ArgumentCaptor.forClass(Specification.class);

        verify(roleRepository).findAll(specificationArgCaptor.capture(), eq(pageable));

        var specification = specificationArgCaptor.getValue();

        assertThat(specificationArgCaptor.getValue()).isInstanceOf(RoleSpecification.class);

        var roleRequestSpec = (RoleSpecification) specification;

        assertThat(roleRequestSpec.getFilter().getBuiltIn()).isTrue();
        assertThat(roleRequestSpec.getFilter().getDescription()).isEqualTo("Description");

        assertThat(searchOutcome).isInstanceOfSatisfying(RoleService.SearchRolesOutcome.Success.class, success -> {
            assertThat(success.page().getContent()).isNotNull().isEmpty();
            assertThat(success.page().getTotalPages()).isZero();
        });
    }

    @Test
    void findRolesByIds_empty_success() {

        var findRolesByArgs = new RoleService.FindRolesByArgs.FindByIds(Collections.emptyList());

        var outcome = roleService.findRolesBy(findRolesByArgs);

        assertThat(outcome).isInstanceOfSatisfying(RoleService.FindRolesByOutcome.Success.class, success -> {
            assertThat(success.roles()).isEmpty();
        });
    }

    @Test
    void findRolesByCodes_empty_success() {

        var findRolesByArgs = new RoleService.FindRolesByArgs.FindByCodes(Collections.emptyList());

        var outcome = roleService.findRolesBy(findRolesByArgs);

        assertThat(outcome).isInstanceOfSatisfying(RoleService.FindRolesByOutcome.Success.class, success -> {
            assertThat(success.roles()).isEmpty();
        });
    }

    @Test
    void findRolesByIds_missing() {

        var roles = Instancio.ofList(RoleEntity.class).size(10).create();

        var roleIds = roles.stream().map(RoleEntity::getId).toList();

        var findRolesByArgs = new RoleService.FindRolesByArgs.FindByIds(roleIds);

        var foundEntities = List.of(roles.getFirst(), roles.get(1));

        var expectedMissing = roles.subList(2, roles.size()).stream()
                .map(role -> new RoleService.RoleIdentifier.Id(role.getId()))
                .toList();

        given(roleRepository.findByIdIn(roleIds)).willReturn(foundEntities);

        var outcome = roleService.findRolesBy(findRolesByArgs);

        assertThat(outcome).isInstanceOfSatisfying(RoleService.FindRolesByOutcome.NotFound.class, notFound -> {
            assertThat(notFound.notFoundRoles())
                    .extracting(rid -> ((RoleService.RoleIdentifier.Id) rid).id())
                    .containsExactlyInAnyOrderElementsOf(expectedMissing.stream()
                            .map(RoleService.RoleIdentifier.Id::id)
                            .toList());
        });
    }

    @Test
    void findRolesByCodes_missing() {

        var roles = Instancio.ofList(RoleEntity.class).size(10).create();

        var roleCodes = roles.stream().map(RoleEntity::getCode).toList();

        var findRolesByArgs = new RoleService.FindRolesByArgs.FindByCodes(roleCodes);

        var foundEntities = List.of(roles.getFirst(), roles.get(1));

        var expectedMissing = roles.subList(2, roles.size()).stream()
                .map(role -> new RoleService.RoleIdentifier.Code(role.getCode()))
                .toList();

        given(roleRepository.findByCodeIn(roleCodes)).willReturn(foundEntities);

        var outcome = roleService.findRolesBy(findRolesByArgs);

        assertThat(outcome).isInstanceOfSatisfying(RoleService.FindRolesByOutcome.NotFound.class, notFound -> {
            assertThat(notFound.notFoundRoles())
                    .extracting(roleCode -> ((RoleService.RoleIdentifier.Code) roleCode).code())
                    .containsExactlyInAnyOrderElementsOf(expectedMissing.stream()
                            .map(RoleService.RoleIdentifier.Code::code)
                            .toList());
        });
    }

    @Test
    void findRolesByIds_success() {

        var roles = Instancio.ofList(RoleEntity.class).size(4).create();

        var identityAttributes =
                Instancio.ofList(IdentityAttributeRole.class).size(2).create();

        var mappedRoles = roles.stream()
                .map(roleEntity -> roleMapper.toRole(roleEntity, identityAttributes))
                .toList();

        var roleIds = roles.stream().map(RoleEntity::getId).toList();

        var findRolesByArgs = new RoleService.FindRolesByArgs.FindByIds(roleIds);

        given(roleRepository.findByIdIn(roleIds)).willReturn(roles);

        given(identityAttributeRolesRepository.findByRole_CodeAndEnabledTrue(any(String.class)))
                .willReturn(identityAttributes);

        var outcome = roleService.findRolesBy(findRolesByArgs);

        assertThat(outcome).isInstanceOfSatisfying(RoleService.FindRolesByOutcome.Success.class, success -> {
            assertThat(mappedRoles).containsExactlyInAnyOrderElementsOf(success.roles());
        });
    }

    @Test
    void findRolesByCodes_success() {

        var roles = Instancio.ofList(RoleEntity.class).size(4).create();

        var identityAttributes =
                Instancio.ofList(IdentityAttributeRole.class).size(2).create();

        var mappedRoles = roles.stream()
                .map(roleEntity -> roleMapper.toRole(roleEntity, identityAttributes))
                .toList();

        var roleCodes = roles.stream().map(RoleEntity::getCode).toList();

        var findRolesByArgs = new RoleService.FindRolesByArgs.FindByCodes(roleCodes);

        given(roleRepository.findByCodeIn(roleCodes)).willReturn(roles);

        given(identityAttributeRolesRepository.findByRole_CodeAndEnabledTrue(any(String.class)))
                .willReturn(identityAttributes);

        var outcome = roleService.findRolesBy(findRolesByArgs);

        assertThat(outcome).isInstanceOfSatisfying(RoleService.FindRolesByOutcome.Success.class, success -> {
            assertThat(mappedRoles).containsExactlyInAnyOrderElementsOf(success.roles());
        });
    }
}
