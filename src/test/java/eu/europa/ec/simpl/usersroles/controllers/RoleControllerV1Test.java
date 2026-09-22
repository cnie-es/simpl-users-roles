package eu.europa.ec.simpl.usersroles.controllers;

import static eu.europa.ec.simpl.common.test.TestUtil.a;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.simpl.api.usersroles.v1.exchanges.RolesApi;
import eu.europa.ec.simpl.api.usersroles.v1.model.RoleDTO;
import eu.europa.ec.simpl.usersroles.controllers.mappers.RoleMapperV1Impl;
import eu.europa.ec.simpl.usersroles.models.Role;
import eu.europa.ec.simpl.usersroles.models.RoleFilter;
import eu.europa.ec.simpl.usersroles.services.RoleService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.RoleRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;

@ExtendWith(MockitoExtension.class)
class RoleControllerV1Test {

    private static final String UUID_1 = "4927a9b6-a04f-4597-9376-13d0b31963e4";
    private static final String UUID_2 = "2f7ec979-8eb0-42f9-a9fd-d269267c8c47";

    @Mock
    private RoleService roleService;

    @Spy
    private RoleMapperV1Impl roleMapper;

    @InjectMocks
    private RoleControllerV1 roleController;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(roleController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void testFindRoleById() throws Exception {
        var roleId = UUID.fromString(UUID_1);
        var expectedRole = a(RoleDTO.class);
        var mockedRole = roleMapper.toRole(expectedRole);

        when(roleService.findRoleBy(new RoleService.FindByArgs.FindById(roleId)))
                .thenReturn(new RoleService.FindRoleByOutcome.Success(mockedRole));

        mvc.perform(get("/v1/roles/{roleId}", roleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(expectedRole.getId().toString()))
                .andExpect(jsonPath("$.name").value(expectedRole.getName()))
                .andExpect(jsonPath("$.description").value(expectedRole.getDescription()));

        verify(roleService).findRoleBy(new RoleService.FindByArgs.FindById(roleId));
    }

    @Test
    void testCreateRole() throws Exception {
        var inputDTO = a(RoleDTO.class);
        var role = roleMapper.toRole(inputDTO);
        given(roleService.create(any())).willReturn(new RoleService.CreateRoleOutcome.Success(role));

        mvc.perform(post("/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(inputDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(role.getId().toString()))
                .andExpect(jsonPath("$.name").value(role.getName()))
                .andExpect(jsonPath("$.description").value(role.getDescription()));

        then(roleService).should(times(1)).create(any(Role.class));
    }

    @Test
    void testCreateRole_builtIn() throws Exception {
        var inputDTO = a(RoleDTO.class);
        var role = roleMapper.toRole(inputDTO);
        given(roleService.create(any())).willReturn(new RoleService.CreateRoleOutcome.BuiltInRole(role));

        mvc.perform(post("/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(inputDTO)))
                .andExpect(status().isConflict());

        then(roleService).should(times(1)).create(any(Role.class));
    }

    @Test
    void testUpdateRole() throws Exception {
        var inputDTO = a(RoleDTO.class);
        var role = roleMapper.toRole(inputDTO);
        given(roleService.update(any())).willReturn(new RoleService.UpdateRoleOutcome.Success(role));

        mvc.perform(put("/v1/roles/{id}", inputDTO.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(inputDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(role.getId().toString()))
                .andExpect(jsonPath("$.name").value(role.getName()))
                .andExpect(jsonPath("$.description").value(role.getDescription()));

        then(roleService).should(times(1)).update(role);
    }

    @Test
    void testUpdateRole_builtIn() throws Exception {
        var inputDTO = a(RoleDTO.class);
        var role = roleMapper.toRole(inputDTO);
        given(roleService.update(any())).willReturn(new RoleService.UpdateRoleOutcome.BuiltInRole(role));

        mvc.perform(put("/v1/roles/{id}", inputDTO.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(inputDTO)))
                .andExpect(status().isConflict());

        then(roleService).should(times(1)).update(role);
    }

    @Test
    void testDeleteRole() throws Exception {
        var roleId = UUID.fromString(UUID_1);
        var roleRepresentation = a(RoleRepresentation.class);

        roleRepresentation.setId(roleId.toString());
        when(roleService.delete(roleId)).thenReturn(new RoleService.DeleteRoleOutcome.Success(roleId));
        mvc.perform(delete("/v1/roles/{roleId}", roleId)).andExpect(status().isNoContent());
        then(roleService).should(times(1)).delete(roleId);
    }

    @Test
    void testDeleteRole_builtIn() throws Exception {
        var roleId = UUID.fromString(UUID_1);
        var roleRepresentation = a(RoleRepresentation.class);

        roleRepresentation.setId(roleId.toString());
        when(roleService.delete(roleId)).thenReturn(new RoleService.DeleteRoleOutcome.BuiltInRole(roleId));
        mvc.perform(delete("/v1/roles/{roleId}", roleId)).andExpect(status().isConflict());
        then(roleService).should(times(1)).delete(roleId);
    }

    @Test
    void assignIdentityAttributes_usingValidUser_success() throws Exception {
        var roleId = UUID.fromString(UUID_1);
        var iaCodes = List.of("ia1", "ia2");

        when(roleService.replaceIdentityAttributes(roleId, iaCodes))
                .thenReturn(new RoleService.ReplaceIdentityAttributesOutcome.Success(iaCodes));

        mvc.perform(request(HttpMethod.PUT, "/v1/roles/{roleId}/identityAttributes", roleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(iaCodes)))
                .andExpect(
                        result -> assertThat(result.getResponse().getStatus()).isEqualTo(200));

        verify(roleService).replaceIdentityAttributes(eq(roleId), argThat(iaCodes::containsAll));
    }

    @Test
    void assignIdentityAttributes_RoleDoesNotExist_404Response() throws Exception {
        var roleId = UUID.fromString(UUID_1);
        var iaCodes = List.of("ia1", "ia2");

        mvc.perform(request(HttpMethod.PUT, "/role/{roleId}/identity-attributes", roleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(iaCodes)))
                .andExpect(
                        result -> assertThat(result.getResponse().getStatus()).isEqualTo(404));
    }

    @Test
    void deleteAttributeFromRole() throws Exception {
        var roleId = UUID.fromString(UUID_1);
        var attributeCode = "AttributeCode";
        var params = new LinkedMultiValueMap<String, String>();
        params.add("roleId", roleId.toString());
        params.add("attributeCode", attributeCode);

        when(roleService.removeAttributeForRole(attributeCode, roleId))
                .thenReturn(new RoleService.RemoveAttributeForRoleOutcome.Success());

        mvc.perform(delete("/v1/roles/{roleId}/identityAttributes", roleId).queryParams(params))
                .andExpect(status().isNoContent());

        then(roleService).should(times(1)).removeAttributeForRole(attributeCode, roleId);
    }

    @Test
    void searchRoles() throws Exception {

        var expectedRole = a(Role.class).setCode("role-1").setName("name-role-1");

        Page<Role> pageResponseResult = new PageImpl<>(List.of(expectedRole), PageRequest.of(0, 20), 1);

        given(roleService.search(any(RoleFilter.class), any(Pageable.class)))
                .willReturn(new RoleService.SearchRolesOutcome.Success(pageResponseResult));

        mvc.perform(get("/v1/roles").param("name", "role-1").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value(expectedRole.getCode()))
                .andExpect(jsonPath("$.content[0].description").value(expectedRole.getDescription()))
                .andExpect(
                        jsonPath("$.content[0].id").value(expectedRole.getId().toString()));
        verify(roleService, times(1)).search(any(RoleFilter.class), any(Pageable.class));
    }

    @Test
    void duplicateIdentityAttributeToAnOtherRole_NoThrowException_successResponse() throws Exception {
        var sourceRoleId = UUID.fromString(UUID_1);
        var destinationRoleId = UUID.fromString(UUID_2);

        when(roleService.duplicateIdentityAttributeToAnOtherRole(sourceRoleId, destinationRoleId))
                .thenReturn(new RoleService.DuplicateIdentityAttributeToAnOtherRoleOutcome.Success());

        // TODO fix accept header in simpl-api-iaa
        mvc.perform(request(HttpMethod.POST, "/v1" + RolesApi.DuplicateIdentityAttributeToAnOtherRolePath, sourceRoleId)
                        //                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(destinationRoleId.toString()))
                .andExpect(
                        result -> assertThat(result.getResponse().getStatus()).isEqualTo(200));
    }

    @Test
    void duplicateIdentityAttributeToAnOtherRole_invalidTargetId_ThrowsException() throws Exception {
        var sourceRoleId = UUID.fromString(UUID_1);

        mvc.perform(request(HttpMethod.POST, "/v1" + RolesApi.DuplicateIdentityAttributeToAnOtherRolePath, sourceRoleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"INVALID_TARGET_ID\"" + UUID_2))
                .andExpect(
                        result -> assertThat(result.getResponse().getStatus()).isEqualTo(400));
    }

    @Test
    void duplicateIdentityAttributeToAnOtherRole_ThrowUserNotFoundException_statusCode404() throws Exception {
        var sourceRoleId = UUID.fromString(UUID_1);
        var destinationRoleId = UUID.fromString(UUID_2);

        mvc.perform(request(HttpMethod.POST, "/role/{roleId}/duplicate-identity-attribute", sourceRoleId)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(destinationRoleId.toString()))
                .andExpect(
                        result -> assertThat(result.getResponse().getStatus()).isEqualTo(404));
    }
}
