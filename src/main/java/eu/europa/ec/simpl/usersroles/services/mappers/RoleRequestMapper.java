package eu.europa.ec.simpl.usersroles.services.mappers;

import eu.europa.ec.simpl.usersroles.entities.RoleRequestEntity;
import eu.europa.ec.simpl.usersroles.entities.RoleRequestedEntity;
import eu.europa.ec.simpl.usersroles.models.RoleRequest;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.springframework.data.domain.Page;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RoleRequestMapper {

    @Mapping(target = "rolesRequested", ignore = true)
    RoleRequestEntity toRoleRequestEntity(RoleRequest roleRequest, String userEmail, String reviewedBy);

    @Mapping(target = "role", source = "roleName")
    @Mapping(target = "requestedBy", source = "roleRequest.createdBy")
    @Mapping(target = "approved", constant = "false")
    @Mapping(target = "requestedTimestamp", ignore = true)
    @Mapping(target = "roleRequest", source = "roleRequestEntity")
    @Mapping(target = "id", ignore = true)
    RoleRequestedEntity toRoleRequestedEntity(
            RoleRequest roleRequest, String roleName, RoleRequestEntity roleRequestEntity);

    @Mapping(target = "role", source = "roleCode")
    @Mapping(target = "requestedBy", source = "createdBy")
    @Mapping(target = "approved", constant = "false")
    @Mapping(target = "requestedTimestamp", ignore = true)
    @Mapping(target = "roleRequest", source = "roleRequestEntity")
    @Mapping(target = "id", ignore = true)
    RoleRequestedEntity toRoleRequestedEntity(String createdBy, String roleCode, RoleRequestEntity roleRequestEntity);

    @Mapping(target = "createdBy", source = "userEmail")
    @Mapping(target = "rolesRequested", source = "rolesRequested", qualifiedByName = "rolesRequested")
    @Mapping(target = "rolesAssigned", source = "rolesRequested", qualifiedByName = "rolesAssigned")
    RoleRequest toRoleRequest(RoleRequestEntity entity);

    @Named("rolesRequested")
    default Set<String> mapRolesRequested(Collection<RoleRequestedEntity> entities) {
        if (entities == null) {
            return Set.of();
        }
        return entities.stream()
                .filter(r ->
                        r.getRequestedBy().equalsIgnoreCase(r.getRoleRequest().getUserEmail()))
                .map(RoleRequestedEntity::getRole)
                .collect(Collectors.toSet());
    }

    @Named("rolesAssigned")
    default Set<String> mapRolesAssigned(Collection<RoleRequestedEntity> entities) {
        if (entities == null) {
            return Set.of();
        }
        return entities.stream()
                .filter(e -> Boolean.TRUE.equals(e.getApproved()))
                .map(RoleRequestedEntity::getRole)
                .collect(Collectors.toSet());
    }

    default Page<RoleRequest> toRoleRequestPage(Page<RoleRequestEntity> page) {
        return page.map(this::toRoleRequest);
    }
}
