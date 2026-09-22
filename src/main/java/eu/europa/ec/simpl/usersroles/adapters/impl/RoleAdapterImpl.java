package eu.europa.ec.simpl.usersroles.adapters.impl;

import eu.europa.ec.simpl.usersroles.adapters.RoleAdapter;
import eu.europa.ec.simpl.usersroles.adapters.mappers.KeycloakMapper;
import eu.europa.ec.simpl.usersroles.exceptions.KeycloakException;
import eu.europa.ec.simpl.usersroles.models.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * An outbound adapter for Keycloak http operations
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Log4j2
public class RoleAdapterImpl implements RoleAdapter {

    private static final Pattern LAST_PATH_LOCATION_PATTERN = Pattern.compile(".{0,2048}/(.+)");

    RealmResource realm;
    KeycloakMapper keycloakMapper;

    @Override
    public Optional<Role> getRoleByName(@NotBlank String roleName) {
        try {
            var role = realm.roles().get(roleName).toRepresentation();
            return Optional.ofNullable(keycloakMapper.toRole(role));
        } catch (NotFoundException e) {
            log.warn("Error - Role [{}] not found", roleName);
            return Optional.empty();
        } catch (WebApplicationException e) {
            log.error("Error getting role [{}] from Keycloak realm", roleName, e);
            throw new KeycloakException(e.getResponse(), e);
        }
    }

    @Override
    public Optional<Role> getRoleById(@NotNull UUID id) {
        try {
            var role = realm.rolesById().getRole(id.toString());
            return Optional.ofNullable(keycloakMapper.toRole(role));
        } catch (NotFoundException e) {
            log.warn("Error - Role with id[{}] not found", id);
            return Optional.empty();
        } catch (WebApplicationException e) {
            log.error("Error retrieving role with id [{}] from Keycloak", id, e);
            throw new KeycloakException(e.getResponse(), e);
        }
    }

    @Override
    public UpdateKeycloakRoleOutCome updateRole(@NotNull Role roleUpdate) {
        log.info("Updating role {} with data {}", roleUpdate.getCode(), roleUpdate);

        try {
            var roleResourceOpt = getRoleByName(roleUpdate.getCode());

            if (roleResourceOpt.isEmpty()) {
                log.warn("Error updating role {} - role not found", roleUpdate);
                return new UpdateKeycloakRoleOutCome.NotFound(roleUpdate.getCode());
            }

            var existing = roleResourceOpt.get();

            if (!Objects.equals(existing.getCode(), roleUpdate.getCode())) {
                log.warn("Error updating role {} - invalid operation: cannot update role's name", roleUpdate);
                return new UpdateKeycloakRoleOutCome.InvalidInput("Invalid operation: cannot update role's name");
            }

            var roleResource = realm.rolesById();
            existing.setDescription(roleUpdate.getDescription());

            var roleUpdated = keycloakMapper.toRepresentation(existing);
            roleResource.updateRole(existing.getId().toString(), roleUpdated);
            return new UpdateKeycloakRoleOutCome.Success(keycloakMapper.toRole(roleUpdated));

        } catch (WebApplicationException e) {
            log.error("Error updating role [{}] in Keycloak realm", roleUpdate.getCode(), e);
            throw new KeycloakException(e.getResponse(), e);
        }
    }

    @Override
    public CreateKeycloakRoleOutcome createRole(@NotNull Role role) {
        log.info("Creating role [{}] in realm", role.getCode());
        log.info("Role details: [{}]", role);
        try {
            realm.roles().create(keycloakMapper.toRepresentation(role));
            var createdRole = realm.roles().get(role.getCode()).toRepresentation();
            log.info("Added role [{}] to realm successfully", createdRole.getName());
            return new CreateKeycloakRoleOutcome.Success(keycloakMapper.toRole(createdRole));

        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == HttpStatus.CONFLICT.value()) {
                log.warn("Role [{}] already exists", role.getCode());
                return new CreateKeycloakRoleOutcome.Duplicated(role);
            }
            log.error("Error adding role [{}] to realm", role.getCode(), e);
            throw new KeycloakException(e.getResponse(), e);
        }
    }

    @Override
    public DeleteKeycloakRoleOutcome deleteRole(DeleteRoleArgs deleteRoleArgs) {

        Optional<Role> role;

        try {

            switch (deleteRoleArgs) {
                case DeleteRoleArgs.DeleteById deleteById -> {
                    log.info("Deleting role with id [{}] from realm", deleteById.id());
                    role = getRoleById(deleteById.id());
                    if (role.isEmpty()) {
                        return new DeleteKeycloakRoleOutcome.NotFound(new RoleIdentifier.Id(deleteById.id()));
                    }
                    var roleResourceId = realm.rolesById();
                    roleResourceId.deleteRole(role.get().getId().toString());
                }
                case DeleteRoleArgs.DeleteByName deleteByName -> {
                    log.info("Deleting role with name [{}] from realm", deleteByName.name());
                    role = getRoleByName(deleteByName.name());
                    if (role.isEmpty()) {
                        return new DeleteKeycloakRoleOutcome.NotFound(new RoleIdentifier.Code(deleteByName.name()));
                    }
                    realm.roles().get(deleteByName.name()).remove();
                }
            }

            log.info("Role [{}] deleted with success", role.get());
            return new DeleteKeycloakRoleOutcome.Success(role.get());

        } catch (WebApplicationException e) {
            log.error("Error deleting role [{}] from realm", deleteRoleArgs, e);
            throw new KeycloakException(e.getResponse(), e);
        }
    }

    @Override
    public IsKeycloakRoleAssignedOutcome isRoleAssigned(String roleName) {

        log.info("Checking if role [{}] is assigned to any user", roleName);

        try {

            RoleResource roleResource = realm.roles().get(roleName);

            var userWithRole = roleResource.getUserMembers(0, 1);

            return new IsKeycloakRoleAssignedOutcome.Success(!userWithRole.isEmpty());

        } catch (NotFoundException e) {
            log.warn("Error Checking if role [{}] is assigned to any user Error - Role not found", roleName);
            return new IsKeycloakRoleAssignedOutcome.NotFound(roleName);
        } catch (WebApplicationException e) {
            log.error("Error in method isKeyCloakRoleAssigned for role {}", roleName, e);
            throw new KeycloakException(e.getResponse(), e);
        }
    }

    @Override
    public Collection<Role> getRolesList() {

        log.info("Getting role list");
        try {
            var roles = realm.roles().list();
            log.info("roles list retrieved: {}", roles);
            return keycloakMapper.toRoleList(roles);
        } catch (WebApplicationException e) {
            log.error("Getting roles list receive a key cloak failure", e);
            throw new KeycloakException(e.getResponse(), e);
        }
    }
}
