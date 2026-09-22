package eu.europa.ec.simpl.usersroles.services.mappers;

import eu.europa.ec.simpl.api.usersroles.v1.model.RoleDTO;
import eu.europa.ec.simpl.usersroles.entities.IdentityAttributeRole;
import eu.europa.ec.simpl.usersroles.entities.RoleEntity;
import eu.europa.ec.simpl.usersroles.models.IdentityAttribute;
import eu.europa.ec.simpl.usersroles.models.Role;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.Generated;
import org.keycloak.representations.idm.RoleRepresentation;
import org.mapstruct.AnnotateWith;
import org.mapstruct.BeanMapping;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

@AnnotateWith(Generated.class)
@Mapper(injectionStrategy = InjectionStrategy.SETTER, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RoleMapper {

    @Mapping(source = "representation.name", target = "name")
    @Mapping(source = "representation.description", target = "description")
    @Mapping(source = "representation", target = "id")
    @Mapping(source = "idas", target = "assignedIdentityAttributes")
    RoleDTO toDto(RoleRepresentation representation, List<IdentityAttributeRole> idas);

    @Mapping(target = "name", source = "role.name")
    @Mapping(target = "description", source = "role.description")
    @Mapping(target = "id", source = "role.id")
    @Mapping(target = "assignedIdentityAttributes", source = "idas")
    @Mapping(target = "builtIn", constant = "false")
    @Mapping(target = "enabled", constant = "true")
    @Mapping(target = "code", source = "role.code")
    Role toRole(Role role, List<IdentityAttributeRole> idas);

    @Mapping(target = "name", source = "role.name")
    @Mapping(target = "description", source = "role.description")
    @Mapping(target = "id", source = "role.id")
    @Mapping(target = "assignedIdentityAttributes", source = "idas")
    @Mapping(target = "builtIn", source = "role.builtIn")
    @Mapping(target = "enabled", source = "role.enabled")
    @Mapping(target = "code", source = "role.code")
    Role toRole(RoleEntity role, List<IdentityAttributeRole> idas);

    @BeanMapping(ignoreUnmappedSourceProperties = {"assignedIdentityAttributes"})
    @Mapping(target = "creationTimestamp", ignore = true)
    @Mapping(target = "lastUpdateTimestamp", ignore = true)
    @Mapping(target = "identityAttributeRoles", ignore = true)
    RoleEntity toRoleEntity(Role role);

    List<Role> toRoleList(List<RoleEntity> roleEntities);

    @Mapping(target = "assignedIdentityAttributes", source = "identityAttributeRoles")
    Role toRole(RoleEntity role);

    @Mapping(target = "creationTimestamp", ignore = true)
    @Mapping(target = "lastUpdateTimestamp", ignore = true)
    @Mapping(target = "identityAttributeRoles", ignore = true)
    void updateRole(@MappingTarget RoleEntity roleEntity, Role role);

    default List<String> roleIdaToRoleDTOIda(List<IdentityAttributeRole> rolesIda) {
        if (rolesIda != null) {
            return rolesIda.stream().map(IdentityAttributeRole::getIdaCode).toList();
        } else {
            return Collections.emptyList();
        }
    }

    default UUID getUuid(RoleRepresentation representation) {
        return UUID.fromString(representation.getId());
    }

    List<IdentityAttribute> toIdentityAttributeList(List<IdentityAttributeRole> identityAttributeRoles);

    @Mapping(target = "code", source = "idaCode")
    IdentityAttribute toIdentityAttribute(IdentityAttributeRole identityAttributeRole);

    default String identityAttributeRoleToIdaCode(IdentityAttributeRole identityAttributeRole) {
        return identityAttributeRole != null ? identityAttributeRole.getIdaCode() : null;
    }

    default Page<Role> toRolePage(Page<RoleEntity> page) {
        return page.map(this::toRole);
    }
}
