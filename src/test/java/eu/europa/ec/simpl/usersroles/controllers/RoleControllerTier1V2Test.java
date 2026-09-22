package eu.europa.ec.simpl.usersroles.controllers;

import static eu.europa.ec.simpl.common.test.TestUtil.a;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.simpl.api.usersroles.t1.v2.model.IdentityAttributeDTO;
import eu.europa.ec.simpl.api.usersroles.t1.v2.model.RoleDTO;
import eu.europa.ec.simpl.common.exceptions.http.HttpResponseException;
import eu.europa.ec.simpl.usersroles.controllers.mappers.IdentityAttributeMapperTier1V2Impl;
import eu.europa.ec.simpl.usersroles.controllers.mappers.RoleMapperTier1V2Impl;
import eu.europa.ec.simpl.usersroles.models.IdentityAttribute;
import eu.europa.ec.simpl.usersroles.models.Role;
import eu.europa.ec.simpl.usersroles.services.RoleService;
import eu.europa.ec.simpl.usersroles.services.model.InvalidOutput;
import java.util.List;
import java.util.UUID;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class RoleControllerTier1V2Test {

    @Mock
    private RoleService roleService;

    @InjectMocks
    private RoleControllerTier1V2 roleController;

    @Spy
    private RoleMapperTier1V2Impl roleMapper;

    @Spy
    private IdentityAttributeMapperTier1V2Impl identityAttributeMapper;

    private MockMvc mvc;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {

        mvc = MockMvcBuilders.standaloneSetup(roleController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void testCreateNewRole_success() throws Exception {

        var inputDTO = a(eu.europa.ec.simpl.api.usersroles.t1.v2.model.RoleDTO.class);
        var role = roleMapper.toRole(inputDTO);
        given(roleService.create(any())).willReturn(new RoleService.CreateRoleOutcome.Success(role));

        mvc.perform(post("/tier1/v2/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(inputDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(role.getId().toString()))
                .andExpect(jsonPath("$.code").value(role.getCode()))
                .andExpect(jsonPath("$.name").value(role.getName()))
                .andExpect(jsonPath("$.description").value(role.getDescription()));

        then(roleService).should(times(1)).create(any(Role.class));
    }

    @Test
    void testCreateNewRole_builtIn() throws Exception {

        var inputDTO = a(eu.europa.ec.simpl.api.usersroles.t1.v2.model.RoleDTO.class);
        var role = roleMapper.toRole(inputDTO);
        given(roleService.create(any())).willReturn(new RoleService.CreateRoleOutcome.BuiltInRole(role));

        mvc.perform(post("/tier1/v2/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(inputDTO)))
                .andExpect(status().isConflict());

        then(roleService).should(times(1)).create(any(Role.class));
    }

    @Test
    void testCreateNewRole_duplicated() throws Exception {

        var inputDTO = a(eu.europa.ec.simpl.api.usersroles.t1.v2.model.RoleDTO.class);
        var role = roleMapper.toRole(inputDTO);

        given(roleService.create(any())).willReturn(new RoleService.CreateRoleOutcome.Duplicated(role));

        mvc.perform(post("/tier1/v2/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(role)))
                .andExpect(status().isConflict());

        then(roleService).should(times(1)).create(any(Role.class));
    }

    @Test
    void testUpdateRole_success() throws Exception {

        var inputDTO = a(RoleDTO.class);
        var role = roleMapper.toRole(inputDTO);
        given(roleService.update(any())).willReturn(new RoleService.UpdateRoleOutcome.Success(role));

        mvc.perform(put("/tier1/v2/roles/{id}", inputDTO.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(inputDTO)))
                .andExpect(status().isNoContent());

        then(roleService).should(times(1)).update(any(Role.class));
    }

    @Test
    void testUpdateRole_NotFound() throws Exception {

        var inputDTO = a(RoleDTO.class);
        var role = roleMapper.toRole(inputDTO);
        given(roleService.update(any())).willReturn(new RoleService.UpdateRoleOutcome.NotFound(role.getId()));

        mvc.perform(put("/tier1/v2/roles/{id}", inputDTO.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(inputDTO)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpdateRole_InvalidInput() throws Exception {

        var inputDTO = a(RoleDTO.class);
        given(roleService.update(any()))
                .willReturn(new RoleService.UpdateRoleOutcome.InvalidChangeNameAttempt(
                        inputDTO.getId(), inputDTO.getName()));

        mvc.perform(put("/tier1/v2/roles/{id}", inputDTO.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(inputDTO)))
                .andExpect(status().isForbidden())
                .andExpect(status().reason("Update operation error: invalid change name attempt"));
    }

    @Test
    void testUpdateRole_builtIn() throws Exception {

        var inputDTO = a(RoleDTO.class);

        var role = roleMapper.toRole(inputDTO);

        given(roleService.update(any())).willReturn(new RoleService.UpdateRoleOutcome.BuiltInRole(role));

        mvc.perform(put("/tier1/v2/roles/{id}", inputDTO.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(inputDTO)))
                .andExpect(status().isConflict());
    }

    @Test
    void testDeleteRole_success() throws Exception {

        var inputDTO = a(RoleDTO.class);

        given(roleService.delete(inputDTO.getId()))
                .willReturn(new RoleService.DeleteRoleOutcome.Success(inputDTO.getId()));

        mvc.perform(delete("/tier1/v2/roles/{id}", inputDTO.getId())).andExpect(status().isNoContent());

        then(roleService).should(times(1)).delete(inputDTO.getId());
    }

    @Test
    void testDeleteRole_NotFound() throws Exception {

        var inputDTO = a(RoleDTO.class);

        given(roleService.delete(inputDTO.getId()))
                .willReturn(new RoleService.DeleteRoleOutcome.NotFound(inputDTO.getId()));

        mvc.perform(delete("/tier1/v2/roles/{id}", inputDTO.getId())).andExpect(status().isNotFound());

        then(roleService).should(times(1)).delete(inputDTO.getId());
    }

    @Test
    void testDeleteRole_builtIn() throws Exception {

        var inputDTO = a(RoleDTO.class);

        given(roleService.delete(inputDTO.getId()))
                .willReturn(new RoleService.DeleteRoleOutcome.BuiltInRole(inputDTO.getId()));

        mvc.perform(delete("/tier1/v2/roles/{id}", inputDTO.getId())).andExpect(status().isConflict());

        then(roleService).should(times(1)).delete(inputDTO.getId());
    }

    @Test
    void testDeleteRole_IsAssigned() throws Exception {

        var inputDTO = a(RoleDTO.class);

        given(roleService.delete(inputDTO.getId()))
                .willReturn(new RoleService.DeleteRoleOutcome.IsAssigned(inputDTO.getId()));

        mvc.perform(delete("/tier1/v2/roles/{id}", inputDTO.getId())).andExpect(status().isConflict());

        then(roleService).should(times(1)).delete(inputDTO.getId());
    }

    @Test
    void testFindRoleById_NotFound() throws Exception {

        var roleId = UUID.randomUUID();

        when(roleService.findRoleBy(new RoleService.FindByArgs.FindById(roleId)))
                .thenReturn(new RoleService.FindRoleByOutcome.NotFound(new RoleService.RoleIdentifier.Id(roleId)));

        mvc.perform(get("/tier1/v2/roles/{roleId}", roleId)).andExpect(status().isNotFound());

        verify(roleService, times(1)).findRoleBy(new RoleService.FindByArgs.FindById(roleId));
    }

    @Test
    void assignIdentityAttributes_success() throws Exception {

        var roleId = UUID.randomUUID();

        var ia = Instancio.ofList(IdentityAttributeDTO.class).create();

        var iaCodes = identityAttributeMapper.toCodes(ia);

        when(roleService.replaceIdentityAttributes(roleId, iaCodes))
                .thenReturn(new RoleService.ReplaceIdentityAttributesOutcome.Success(iaCodes));

        mvc.perform(put("/tier1/v2/roles/{roleId}/identityAttributes", roleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(ia)))
                .andExpect(status().isNoContent());

        verify(roleService).replaceIdentityAttributes(eq(roleId), argThat(iaCodes::equals));
    }

    @Test
    void assignIdentityAttributes_NotFound() throws Exception {

        var roleId = UUID.randomUUID();
        var ia = Instancio.ofList(IdentityAttributeDTO.class).create();

        mvc.perform(put("/tier1/v2/role/{roleId}/identityAttributes", roleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(ia)))
                .andExpect(status().isNotFound());
    }

    @Test
    void assignIdentityAttributes_InvalidAttributes() throws Exception {

        var roleId = UUID.randomUUID();

        var ia = Instancio.ofList(IdentityAttributeDTO.class).create();

        var iaCodes = identityAttributeMapper.toCodes(ia);

        var error1 = new InvalidOutput.NotAssignableToRole("role_1");

        var error2 = new InvalidOutput.NotAssignedToParticipant("partecipant_1");

        List<InvalidOutput> errors = List.of(error1, error2);

        when(roleService.replaceIdentityAttributes(roleId, iaCodes))
                .thenReturn(new RoleService.ReplaceIdentityAttributesOutcome.InvalidIdentityAttributes(errors));

        mvc.perform(put("/tier1/v2/roles/{roleId}/identityAttributes", roleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(ia)))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(String.join(
                        "<br>", errors.stream().map(Object::toString).toList())));

        verify(roleService).replaceIdentityAttributes(eq(roleId), argThat(iaCodes::equals));
    }

    @Test
    void testFindRoleById_Found() throws Exception {

        var roleId = UUID.randomUUID();
        var inputDTO = a(eu.europa.ec.simpl.api.usersroles.t1.v2.model.RoleDTO.class);
        var role = roleMapper.toRole(inputDTO);

        when(roleService.findRoleBy(new RoleService.FindByArgs.FindById(roleId)))
                .thenReturn(new RoleService.FindRoleByOutcome.Success(role));

        mvc.perform(get("/tier1/v2/roles/{roleId}", roleId))
                .andExpect(jsonPath("$.id").value(role.getId().toString()))
                .andExpect(jsonPath("$.name").value(role.getName()))
                .andExpect(jsonPath("$.code").value(role.getCode()))
                .andExpect(jsonPath("$.description").value(role.getDescription()))
                .andExpect(status().isOk());

        verify(roleService).findRoleBy(new RoleService.FindByArgs.FindById(roleId));
    }

    @Test
    void getRoleIdentityAttributes_returnsAttributes() {
        var roleId = UUID.randomUUID();
        var identityAttributes =
                List.of(new IdentityAttribute().setCode("CONSUMER"), new IdentityAttribute().setCode("DATA_PROVIDER"));

        when(roleService.findIdentityAttributesByRoleId(roleId))
                .thenReturn(new RoleService.GetIdentityAttributesByRoleIdOutcome.Success(identityAttributes));

        var result = roleController.getRoleIdentityAttributes(roleId);

        assertEquals(2, result.size());
        assertEquals("CONSUMER", result.get(0).getCode());
        assertEquals("DATA_PROVIDER", result.get(1).getCode());
        verify(roleService).findIdentityAttributesByRoleId(roleId);
    }

    @Test
    void getRoleIdentityAttributes_emptyList_isReturned() {
        var roleId = UUID.randomUUID();
        List<IdentityAttribute> identityAttributes = List.of();

        when(roleService.findIdentityAttributesByRoleId(roleId))
                .thenReturn(new RoleService.GetIdentityAttributesByRoleIdOutcome.Success(identityAttributes));

        var result = roleController.getRoleIdentityAttributes(roleId);

        assertTrue(result.isEmpty());
        verify(roleService).findIdentityAttributesByRoleId(roleId);
    }

    @Test
    void getRoleIdentityAttributes_notFound_isReturned() {
        var roleId = UUID.randomUUID();

        when(roleService.findIdentityAttributesByRoleId(roleId))
                .thenReturn(new RoleService.GetIdentityAttributesByRoleIdOutcome.NotFound(roleId));

        var exception =
                assertThrows(HttpResponseException.class, () -> roleController.getRoleIdentityAttributes(roleId));

        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(roleService).findIdentityAttributesByRoleId(roleId);
    }
}
