package eu.europa.ec.simpl.usersroles.controllers.mappers;

import eu.europa.ec.simpl.api.usersroles.t1.v2.model.RoleDTO;
import eu.europa.ec.simpl.api.usersroles.t1.v2.model.RolesPagedResponseDTO;
import eu.europa.ec.simpl.common.mappers.ToPagedResponse;
import eu.europa.ec.simpl.usersroles.models.Role;
import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RoleMapperTier1V2 {

    @Mapping(target = "assignedIdentityAttributes", ignore = true)
    Role toRole(RoleDTO dto);

    RoleDTO toDto(Role model);

    List<RoleDTO> toRoleDTOList(List<Role> roles);

    List<Role> toRoles(List<RoleDTO> dtos);

    @ToPagedResponse
    @BeanMapping(ignoreUnmappedSourceProperties = {"links", "resolvableType", "nextLink", "previousLink"})
    RolesPagedResponseDTO toRolesPagedResponseDTO(PagedModel<EntityModel<Role>> page);

    @Mapping(target = ".", source = "content")
    @BeanMapping(ignoreUnmappedSourceProperties = {"links", "content"})
    Role unwrapEntityModel(EntityModel<Role> entityModel);
}
