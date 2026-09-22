package eu.europa.ec.simpl.usersroles.controllers.mappers;

import eu.europa.ec.simpl.api.usersroles.t1.v2.model.CreateRoleRequestDTO;
import eu.europa.ec.simpl.api.usersroles.t1.v2.model.CreateRoleRequestResponseDTO;
import eu.europa.ec.simpl.api.usersroles.t1.v2.model.RoleRequestDTO;
import eu.europa.ec.simpl.api.usersroles.t1.v2.model.RoleRequestPagedResponseDTO;
import eu.europa.ec.simpl.api.usersroles.t1.v2.model.UserRoleRequestPagedResponseDTO;
import eu.europa.ec.simpl.common.mappers.ToPagedResponse;
import eu.europa.ec.simpl.usersroles.models.RoleRequest;
import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR, unmappedSourcePolicy = ReportingPolicy.ERROR)
public interface RoleRequestMapperTier1V2 {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "status", constant = "OPEN")
    @Mapping(target = "creationTimestamp", ignore = true)
    @Mapping(target = "lastUpdateTimestamp", ignore = true)
    @Mapping(target = "rolesAssigned", ignore = true)
    @Mapping(target = "reviewedBy", ignore = true)
    RoleRequest toRoleRequest(CreateRoleRequestDTO roleRequestEntry);

    RoleRequestDTO toRoleRequestDTO(RoleRequest roleRequest);

    List<RoleRequestDTO> toRoleRequestDTOs(List<RoleRequest> roleRequests);

    @BeanMapping(unmappedSourcePolicy = ReportingPolicy.IGNORE)
    CreateRoleRequestResponseDTO toCreateRoleRequestResponseDTO(RoleRequest roleRequest);

    @ToPagedResponse
    @BeanMapping(ignoreUnmappedSourceProperties = {"links", "resolvableType", "nextLink", "previousLink"})
    RoleRequestPagedResponseDTO toRolesPagedResponseDTO(PagedModel<EntityModel<RoleRequest>> page);

    @ToPagedResponse
    @BeanMapping(ignoreUnmappedSourceProperties = {"links", "resolvableType", "nextLink", "previousLink"})
    UserRoleRequestPagedResponseDTO toUserRoleRequestPagedResponseDTO(PagedModel<EntityModel<RoleRequest>> page);

    @Mapping(target = ".", source = "content")
    @BeanMapping(ignoreUnmappedSourceProperties = {"links", "content"})
    RoleRequestDTO toRoleRequestDTO(EntityModel<RoleRequest> entityModel);
}
