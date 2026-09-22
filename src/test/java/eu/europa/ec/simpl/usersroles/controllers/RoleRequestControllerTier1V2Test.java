package eu.europa.ec.simpl.usersroles.controllers;

import static eu.europa.ec.simpl.common.test.TestUtil.a;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europa.ec.simpl.api.usersroles.t1.v2.model.CreateRoleRequestDTO;
import eu.europa.ec.simpl.api.usersroles.t1.v2.model.RoleRequestPagedResponseDTO;
import eu.europa.ec.simpl.api.usersroles.t1.v2.model.RoleRequestReviewDTO;
import eu.europa.ec.simpl.api.usersroles.t1.v2.model.UserRoleRequestPagedResponseDTO;
import eu.europa.ec.simpl.common.security.AuthService;
import eu.europa.ec.simpl.usersroles.controllers.mappers.RoleRequestMapperTier1V2Impl;
import eu.europa.ec.simpl.usersroles.models.RoleRequest;
import eu.europa.ec.simpl.usersroles.models.RoleRequestFilter;
import eu.europa.ec.simpl.usersroles.models.RoleRequestStatusEnum;
import eu.europa.ec.simpl.usersroles.services.RoleRequestService;
import java.util.ArrayList;
import java.util.UUID;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@ExtendWith(MockitoExtension.class)
class RoleRequestControllerTier1V2Test {

    @Mock
    private RoleRequestService roleRequestService;

    @InjectMocks
    private RoleRequestControllerTier1V2 roleRequestController;

    @Spy
    private RoleRequestMapperTier1V2Impl roleRequestMapper;

    @Mock
    private AuthService authService;

    @Mock
    private PagedResourcesAssembler<RoleRequest> pagedResourcesAssembler;

    private MockMvc mvc;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {

        mvc = MockMvcBuilders.standaloneSetup(roleRequestController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void testCreateNewRoleRequest_success() throws Exception {

        var roleRequestDTO = a(CreateRoleRequestDTO.class);
        var roleRequest = roleRequestMapper.toRoleRequest(roleRequestDTO);
        roleRequest.setId(UUID.randomUUID());
        given(roleRequestService.create(any(RoleRequest.class)))
                .willReturn(new RoleRequestService.CreateRoleRequestOutcome.Success(roleRequest));

        mvc.perform(post("/tier1/v2/roleRequests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(roleRequestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(roleRequest.getId().toString()))
                .andExpect(jsonPath("$.createdBy").value(roleRequest.getCreatedBy()))
                .andExpect(jsonPath(
                        "$.rolesRequested",
                        containsInAnyOrder(roleRequest.getRolesRequested().toArray())));

        then(roleRequestService).should(times(1)).create(any(RoleRequest.class));
    }

    @Test
    void testCreateNewRoleRequest_RolesAlreadyHeld() throws Exception {

        var roleRequestDTO = a(CreateRoleRequestDTO.class);
        var roleRequest = roleRequestMapper.toRoleRequest(roleRequestDTO);
        given(roleRequestService.create(any(RoleRequest.class)))
                .willReturn(new RoleRequestService.CreateRoleRequestOutcome.RoleAlreadyHeld(
                        roleRequest, roleRequest.getRolesRequested()));

        mvc.perform(post("/tier1/v2/roleRequests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(roleRequestDTO)))
                .andExpect(status().isConflict());

        then(roleRequestService).should(times(1)).create(any(RoleRequest.class));
    }

    @Test
    void testCreateNewRoleRequest_rolesNotExisting() throws Exception {

        var roleRequestDTO = a(CreateRoleRequestDTO.class);
        var roleRequest = roleRequestMapper.toRoleRequest(roleRequestDTO);
        given(roleRequestService.create(any(RoleRequest.class)))
                .willReturn(new RoleRequestService.CreateRoleRequestOutcome.RolesNotExisting(
                        roleRequest, roleRequest.getRolesRequested()));

        mvc.perform(post("/tier1/v2/roleRequests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(roleRequestDTO)))
                .andExpect(status().isBadRequest());

        then(roleRequestService).should(times(1)).create(any(RoleRequest.class));
    }

    @Test
    void testCreateNewRoleRequest_roleAlreadyRequested() throws Exception {

        var roleRequestDTO = a(CreateRoleRequestDTO.class);
        var roleRequest = roleRequestMapper.toRoleRequest(roleRequestDTO);
        given(roleRequestService.create(any(RoleRequest.class)))
                .willReturn(new RoleRequestService.CreateRoleRequestOutcome.RoleAlreadyRequested(
                        roleRequest, roleRequest.getRolesRequested()));

        mvc.perform(post("/tier1/v2/roleRequests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(roleRequestDTO)))
                .andExpect(status().isUnprocessableEntity());

        then(roleRequestService).should(times(1)).create(any(RoleRequest.class));
    }

    @Test
    void testCreateNewRoleRequest_userNotFound() throws Exception {

        var roleRequestDTO = a(CreateRoleRequestDTO.class);
        var roleRequest = roleRequestMapper.toRoleRequest(roleRequestDTO);
        given(roleRequestService.create(any(RoleRequest.class)))
                .willReturn(new RoleRequestService.CreateRoleRequestOutcome.UserNotFound(roleRequest));

        mvc.perform(post("/tier1/v2/roleRequests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(roleRequestDTO)))
                .andExpect(status().isBadRequest());

        then(roleRequestService).should(times(1)).create(any(RoleRequest.class));
    }

    @Test
    void testCancelRoleRequest_Success() throws Exception {

        var roleRequest = a(RoleRequest.class);

        given(roleRequestService.delete(roleRequest.getId()))
                .willReturn(new RoleRequestService.DeleteRoleRequestOutcome.Success(roleRequest.getId(), roleRequest));

        mvc.perform(MockMvcRequestBuilders.delete("/tier1/v2/roleRequests/{id}", roleRequest.getId()))
                .andExpect(status().isNoContent());

        then(roleRequestService).should(times(1)).delete(roleRequest.getId());
    }

    @Test
    void testCancelRoleRequest_NotFound() throws Exception {

        var roleRequest = a(RoleRequest.class);

        given(roleRequestService.delete(roleRequest.getId()))
                .willReturn(new RoleRequestService.DeleteRoleRequestOutcome.NotFound(roleRequest.getId()));

        mvc.perform(MockMvcRequestBuilders.delete("/tier1/v2/roleRequests/{id}", roleRequest.getId()))
                .andExpect(status().isNotFound());

        then(roleRequestService).should(times(1)).delete(roleRequest.getId());
    }

    @Test
    void testCancelRoleRequest_RequestAlreadyProcessed() throws Exception {

        var roleRequest = a(RoleRequest.class);

        given(roleRequestService.delete(roleRequest.getId()))
                .willReturn(
                        new RoleRequestService.DeleteRoleRequestOutcome.RequestAlreadyProcessed(roleRequest.getId()));

        mvc.perform(MockMvcRequestBuilders.delete("/tier1/v2/roleRequests/{id}", roleRequest.getId()))
                .andExpect(status().isConflict());

        then(roleRequestService).should(times(1)).delete(roleRequest.getId());
    }

    @Test
    void testCancelRoleRequest_NotCreatedByUser() throws Exception {

        var roleRequest = a(RoleRequest.class);

        given(roleRequestService.delete(roleRequest.getId()))
                .willReturn(new RoleRequestService.DeleteRoleRequestOutcome.NotCreatedByUser(roleRequest.getId()));

        mvc.perform(MockMvcRequestBuilders.delete("/tier1/v2/roleRequests/{id}", roleRequest.getId()))
                .andExpect(status().isForbidden());

        then(roleRequestService).should(times(1)).delete(roleRequest.getId());
    }

    @Test
    void testUpdateRoleRequest_notFound() throws Exception {

        var roleRequest = a(RoleRequest.class);

        given(roleRequestService.updateRequest(
                        roleRequest.getId(), RoleRequestStatusEnum.APPROVED, roleRequest.getRolesRequested()))
                .willReturn(new RoleRequestService.UpdateRoleRequestOutcome.NotFound(
                        roleRequest.getId(), RoleRequestStatusEnum.APPROVED, roleRequest.getRolesRequested()));

        var roleRequestReviewDTO = new RoleRequestReviewDTO();
        roleRequestReviewDTO.setRolesAssigned(new ArrayList<>(roleRequest.getRolesRequested()));
        roleRequestReviewDTO.setStatus(RoleRequestReviewDTO.StatusEnum.APPROVED);

        mvc.perform(MockMvcRequestBuilders.put("/tier1/v2/roleRequests/{id}", roleRequest.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(roleRequestReviewDTO)))
                .andExpect(status().isNotFound());

        then(roleRequestService)
                .should(times(1))
                .updateRequest(roleRequest.getId(), RoleRequestStatusEnum.APPROVED, roleRequest.getRolesRequested());
    }

    @Test
    void testUpdateRoleRequest_rolesNotFound() throws Exception {

        var roleRequest = a(RoleRequest.class);

        given(roleRequestService.updateRequest(
                        roleRequest.getId(), RoleRequestStatusEnum.APPROVED, roleRequest.getRolesRequested()))
                .willReturn(new RoleRequestService.UpdateRoleRequestOutcome.RolesNotFound(
                        roleRequest.getId(),
                        RoleRequestStatusEnum.APPROVED,
                        roleRequest.getRolesRequested(),
                        roleRequest.getRolesRequested()));

        var roleRequestReviewDTO = new RoleRequestReviewDTO();
        roleRequestReviewDTO.setRolesAssigned(new ArrayList<>(roleRequest.getRolesRequested()));
        roleRequestReviewDTO.setStatus(RoleRequestReviewDTO.StatusEnum.APPROVED);

        mvc.perform(MockMvcRequestBuilders.put("/tier1/v2/roleRequests/{id}", roleRequest.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(roleRequestReviewDTO)))
                .andExpect(status().isBadRequest());

        then(roleRequestService)
                .should(times(1))
                .updateRequest(roleRequest.getId(), RoleRequestStatusEnum.APPROVED, roleRequest.getRolesRequested());
    }

    @Test
    void testUpdateRoleRequest_statusNotChanged() throws Exception {

        var roleRequest = a(RoleRequest.class);

        given(roleRequestService.updateRequest(
                        roleRequest.getId(), RoleRequestStatusEnum.APPROVED, roleRequest.getRolesRequested()))
                .willReturn(new RoleRequestService.UpdateRoleRequestOutcome.StatusNotChanged(
                        roleRequest.getId(), RoleRequestStatusEnum.APPROVED, roleRequest.getRolesRequested()));

        var roleRequestReviewDTO = new RoleRequestReviewDTO();
        roleRequestReviewDTO.setRolesAssigned(new ArrayList<>(roleRequest.getRolesRequested()));
        roleRequestReviewDTO.setStatus(RoleRequestReviewDTO.StatusEnum.APPROVED);

        mvc.perform(MockMvcRequestBuilders.put("/tier1/v2/roleRequests/{id}", roleRequest.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(roleRequestReviewDTO)))
                .andExpect(status().isBadRequest());

        then(roleRequestService)
                .should(times(1))
                .updateRequest(roleRequest.getId(), RoleRequestStatusEnum.APPROVED, roleRequest.getRolesRequested());
    }

    @Test
    void testUpdateRoleRequest_ApprovedRolesAssignedEmpty() throws Exception {

        var roleRequest = a(RoleRequest.class);

        given(roleRequestService.updateRequest(
                        roleRequest.getId(), RoleRequestStatusEnum.APPROVED, roleRequest.getRolesRequested()))
                .willReturn(new RoleRequestService.UpdateRoleRequestOutcome.RolesAssignedEmpty(
                        roleRequest.getId(), RoleRequestStatusEnum.APPROVED, roleRequest.getRolesRequested()));

        var roleRequestReviewDTO = new RoleRequestReviewDTO();
        roleRequestReviewDTO.setRolesAssigned(new ArrayList<>(roleRequest.getRolesRequested()));
        roleRequestReviewDTO.setStatus(RoleRequestReviewDTO.StatusEnum.APPROVED);

        mvc.perform(MockMvcRequestBuilders.put("/tier1/v2/roleRequests/{id}", roleRequest.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(roleRequestReviewDTO)))
                .andExpect(status().isBadRequest());

        then(roleRequestService)
                .should(times(1))
                .updateRequest(roleRequest.getId(), RoleRequestStatusEnum.APPROVED, roleRequest.getRolesRequested());
    }

    @Test
    void testUpdateRoleRequest_alreadyProcessed() throws Exception {

        var roleRequest = a(RoleRequest.class);

        given(roleRequestService.updateRequest(
                        roleRequest.getId(), RoleRequestStatusEnum.APPROVED, roleRequest.getRolesRequested()))
                .willReturn(new RoleRequestService.UpdateRoleRequestOutcome.AlreadyProcessed(
                        roleRequest.getId(), RoleRequestStatusEnum.APPROVED, roleRequest.getRolesRequested()));

        var roleRequestReviewDTO = new RoleRequestReviewDTO();
        roleRequestReviewDTO.setRolesAssigned(new ArrayList<>(roleRequest.getRolesRequested()));
        roleRequestReviewDTO.setStatus(RoleRequestReviewDTO.StatusEnum.APPROVED);

        mvc.perform(MockMvcRequestBuilders.put("/tier1/v2/roleRequests/{id}", roleRequest.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(roleRequestReviewDTO)))
                .andExpect(status().isConflict());

        then(roleRequestService)
                .should(times(1))
                .updateRequest(roleRequest.getId(), RoleRequestStatusEnum.APPROVED, roleRequest.getRolesRequested());
    }

    @Test
    void testUpdateRoleRequest_Success() throws Exception {

        var roleRequest = a(RoleRequest.class);

        given(roleRequestService.updateRequest(
                        roleRequest.getId(), RoleRequestStatusEnum.APPROVED, roleRequest.getRolesRequested()))
                .willReturn(new RoleRequestService.UpdateRoleRequestOutcome.Success(
                        roleRequest.getId(),
                        RoleRequestStatusEnum.APPROVED,
                        roleRequest.getRolesRequested(),
                        roleRequest));

        var roleRequestReviewDTO = new RoleRequestReviewDTO();
        roleRequestReviewDTO.setRolesAssigned(new ArrayList<>(roleRequest.getRolesRequested()));
        roleRequestReviewDTO.setStatus(RoleRequestReviewDTO.StatusEnum.APPROVED);

        mvc.perform(MockMvcRequestBuilders.put("/tier1/v2/roleRequests/{id}", roleRequest.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(roleRequestReviewDTO)))
                .andExpect(status().isNoContent());

        then(roleRequestService)
                .should(times(1))
                .updateRequest(roleRequest.getId(), RoleRequestStatusEnum.APPROVED, roleRequest.getRolesRequested());
    }

    @Test
    void testUpdateRoleRequest_invalidInputStatus() throws Exception {

        var roleRequest = a(RoleRequest.class);

        given(roleRequestService.updateRequest(
                        roleRequest.getId(), RoleRequestStatusEnum.OPEN, roleRequest.getRolesRequested()))
                .willReturn(new RoleRequestService.UpdateRoleRequestOutcome.InvalidInputStatus(
                        roleRequest.getId(), RoleRequestStatusEnum.OPEN, roleRequest.getRolesRequested()));

        var roleRequestReviewDTO = new RoleRequestReviewDTO();
        roleRequestReviewDTO.setRolesAssigned(new ArrayList<>(roleRequest.getRolesRequested()));
        roleRequestReviewDTO.setStatus(RoleRequestReviewDTO.StatusEnum.OPEN);

        mvc.perform(MockMvcRequestBuilders.put("/tier1/v2/roleRequests/{id}", roleRequest.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(roleRequestReviewDTO)))
                .andExpect(status().isBadRequest());

        then(roleRequestService)
                .should(times(1))
                .updateRequest(roleRequest.getId(), RoleRequestStatusEnum.OPEN, roleRequest.getRolesRequested());
    }

    @Test
    void testSearch_success() throws Exception {

        var rqFilter = a(RoleRequestFilter.class);

        var pageable = PageRequest.of(0, 10);

        var entities = Instancio.ofList(RoleRequest.class).size(10).create();

        var page = new PageImpl<>(entities, PageRequest.of(0, 20), 10);

        given(roleRequestService.search(rqFilter, pageable))
                .willReturn(new RoleRequestService.SearchRoleRequestsOutcome.Success(rqFilter, page));

        var pageResponseDTO = new RoleRequestPagedResponseDTO();
        pageResponseDTO.setPageSize(page.getSize());
        pageResponseDTO.setPage(page.getNumber());
        pageResponseDTO.setTotal(Math.toIntExact(page.getTotalElements()));
        pageResponseDTO.setItems(roleRequestMapper.toRoleRequestDTOs(page.getContent()));

        given(roleRequestMapper.toRolesPagedResponseDTO(any())).willReturn(pageResponseDTO);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("page", "0");
        params.add("size", "10");
        params.add("email", rqFilter.getEmail());
        params.add("status", rqFilter.getStatus());

        mvc.perform(MockMvcRequestBuilders.get("/tier1/v2/roleRequests").queryParams(params))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.total").value(entities.size()))
                .andExpect(jsonPath("$.items[0].id")
                        .value(entities.getFirst().getId().toString()));

        then(roleRequestService).should(times(1)).search(rqFilter, pageable);
        then(pagedResourcesAssembler).should(times(1)).toModel(page);
        then(roleRequestMapper).should(times(1)).toRolesPagedResponseDTO(any());
    }

    @Test
    void testEndUsersSearch_success() throws Exception {

        var rqFilter = a(RoleRequestFilter.class);

        var pageable = PageRequest.of(0, 10);

        var entities = Instancio.ofList(RoleRequest.class).size(10).create();

        var page = new PageImpl<>(entities, PageRequest.of(0, 20), 10);

        rqFilter.setEmail("test@example.com");

        given(authService.getEmail()).willReturn("test@example.com");

        given(roleRequestService.search(rqFilter, pageable))
                .willReturn(new RoleRequestService.SearchRoleRequestsOutcome.Success(rqFilter, page));

        var pageResponseDTO = new UserRoleRequestPagedResponseDTO();
        pageResponseDTO.setPageSize(page.getSize());
        pageResponseDTO.setPage(page.getNumber());
        pageResponseDTO.setTotal(Math.toIntExact(page.getTotalElements()));
        pageResponseDTO.setItems(roleRequestMapper.toRoleRequestDTOs(page.getContent()));

        given(roleRequestMapper.toUserRoleRequestPagedResponseDTO(any())).willReturn(pageResponseDTO);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("page", "0");
        params.add("size", "10");
        params.add("status", rqFilter.getStatus());

        mvc.perform(MockMvcRequestBuilders.get("/tier1/v2/user/roleRequests").queryParams(params))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.total").value(entities.size()))
                .andExpect(jsonPath("$.items[0].id")
                        .value(entities.getFirst().getId().toString()));

        then(roleRequestService).should(times(1)).search(rqFilter, pageable);
        then(pagedResourcesAssembler).should(times(1)).toModel(page);
        then(roleRequestMapper).should(times(1)).toUserRoleRequestPagedResponseDTO(any());
    }

    @Test
    void testGetRoleRequest_success() throws Exception {

        var roleRequest = a(RoleRequest.class);

        given(roleRequestService.findBy(roleRequest.getId()))
                .willReturn(new RoleRequestService.FindByRoleRequestOutcome.Success(roleRequest.getId(), roleRequest));

        mvc.perform(MockMvcRequestBuilders.get("/tier1/v2/roleRequests/{id}", roleRequest.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(roleRequest.getId().toString()))
                .andExpect(jsonPath("$.createdBy").value(roleRequest.getCreatedBy()))
                .andExpect(jsonPath("$.reviewedBy").value(roleRequest.getReviewedBy()))
                .andExpect(jsonPath("$.status").value(roleRequest.getStatus().toString()));

        then(roleRequestService).should(times(1)).findBy(roleRequest.getId());
        then(roleRequestMapper).should(times(1)).toRoleRequestDTO(any(RoleRequest.class));
    }

    @Test
    void testGetRoleRequest_notFound() throws Exception {

        var roleRequest = a(RoleRequest.class);

        given(roleRequestService.findBy(roleRequest.getId()))
                .willReturn(new RoleRequestService.FindByRoleRequestOutcome.RoleRequestNotFound(roleRequest.getId()));

        mvc.perform(MockMvcRequestBuilders.get("/tier1/v2/roleRequests/{id}", roleRequest.getId()))
                .andExpect(status().isNotFound());

        then(roleRequestService).should(times(1)).findBy(roleRequest.getId());
        then(roleRequestMapper).should(never()).toRoleRequestDTO(any(RoleRequest.class));
    }

    @Test
    void testGetRoleRequest_differentCreator() throws Exception {

        var roleRequest = a(RoleRequest.class);

        given(roleRequestService.findBy(roleRequest.getId()))
                .willReturn(new RoleRequestService.FindByRoleRequestOutcome.DifferentCreator(
                        roleRequest.getId(), "a@example.com", roleRequest.getCreatedBy()));

        mvc.perform(MockMvcRequestBuilders.get("/tier1/v2/roleRequests/{id}", roleRequest.getId()))
                .andExpect(status().isForbidden());

        then(roleRequestService).should(times(1)).findBy(roleRequest.getId());
        then(roleRequestMapper).should(never()).toRoleRequestDTO(any(RoleRequest.class));
    }
}
