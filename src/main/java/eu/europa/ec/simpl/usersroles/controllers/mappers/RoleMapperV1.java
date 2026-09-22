package eu.europa.ec.simpl.usersroles.controllers.mappers;

import eu.europa.ec.simpl.api.usersroles.v1.model.PageResponseRoleDTO;
import eu.europa.ec.simpl.api.usersroles.v1.model.RoleDTO;
import eu.europa.ec.simpl.api.usersroles.v1.model.SearchRolesFilterParameterDTO;
import eu.europa.ec.simpl.usersroles.models.Role;
import eu.europa.ec.simpl.usersroles.models.RoleFilter;
import java.util.Collection;
import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RoleMapperV1 {

    @Mapping(target = "code", source = "name")
    @Mapping(target = "builtIn", constant = "false")
    @Mapping(target = "enabled", constant = "true")
    Role toRole(RoleDTO dto);

    List<Role> toRoles(Collection<RoleDTO> dtos);

    @Mapping(target = "name", source = "code")
    RoleDTO toDto(Role model);

    List<RoleDTO> toDTOs(List<Role> roles);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "builtIn", ignore = true)
    RoleFilter toSearchRolesFilterParameter(SearchRolesFilterParameterDTO filter);

    @Mapping(target = "content", source = "content")
    @Mapping(target = "page", source = ".")
    @Mapping(target = "empty", expression = "java(page.isEmpty())")
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.ERROR)
    PageResponseRoleDTO toRolesPagedResponseDTO(Page<Role> page);
}
