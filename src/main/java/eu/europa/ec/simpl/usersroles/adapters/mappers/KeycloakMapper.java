package eu.europa.ec.simpl.usersroles.adapters.mappers;

import eu.europa.ec.simpl.usersroles.models.Role;
import eu.europa.ec.simpl.usersroles.models.User;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import lombok.Generated;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mapstruct.AfterMapping;
import org.mapstruct.AnnotateWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
@AnnotateWith(Generated.class)
public interface KeycloakMapper {

    @Mapping(source = "password", target = "credentials")
    @Mapping(target = "enabled", constant = "true")
    @Mapping(target = "emailVerified", constant = "true")
    @Mapping(target = "attributes", ignore = true)
    @Mapping(target = "userProfileMetadata", ignore = true)
    @Mapping(target = "self", ignore = true)
    @Mapping(target = "createdTimestamp", ignore = true)
    @Mapping(target = "totp", ignore = true)
    @Mapping(target = "requiredActions", ignore = true)
    @Mapping(target = "federatedIdentities", ignore = true)
    @Mapping(target = "socialLinks", ignore = true)
    @Mapping(target = "realmRoles", ignore = true)
    @Mapping(target = "clientRoles", ignore = true)
    @Mapping(target = "clientConsents", ignore = true)
    @Mapping(target = "notBefore", ignore = true)
    @Mapping(target = "federationLink", ignore = true)
    @Mapping(target = "serviceAccountClientId", ignore = true)
    @Mapping(target = "groups", ignore = true)
    @Mapping(target = "origin", ignore = true)
    @Mapping(target = "disableableCredentialTypes", ignore = true)
    @Mapping(target = "access", ignore = true)
    @Mapping(target = "rawAttributes", ignore = true)
    @Mapping(target = "applicationRoles", ignore = true)
    UserRepresentation toRepresentation(User user);

    @Mapping(target = "roles", source = "roleList")
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "participantId", ignore = true)
    @Mapping(target = "organization", ignore = true)
    User toDto(UserRepresentation userRepresentation, List<String> roleList);

    @AfterMapping
    default void readKeycloakAttributes(UserRepresentation representation, @MappingTarget User user) {
        var attrs = representation.getAttributes();
        if (attrs == null) return;
        var participantIdValues = attrs.get("participantId");
        if (participantIdValues != null && !participantIdValues.isEmpty()) {
            user.setParticipantId(participantIdValues.get(0));
        }
        var organizationValues = attrs.get("organization");
        if (organizationValues != null && !organizationValues.isEmpty()) {
            user.setOrganization(organizationValues.get(0));
        }
    }

    @Mapping(target = "credentials", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "attributes", ignore = true)
    @Mapping(target = "userProfileMetadata", ignore = true)
    @Mapping(target = "self", ignore = true)
    @Mapping(target = "createdTimestamp", ignore = true)
    @Mapping(target = "totp", ignore = true)
    @Mapping(target = "requiredActions", ignore = true)
    @Mapping(target = "federatedIdentities", ignore = true)
    @Mapping(target = "socialLinks", ignore = true)
    @Mapping(target = "realmRoles", ignore = true)
    @Mapping(target = "clientRoles", ignore = true)
    @Mapping(target = "clientConsents", ignore = true)
    @Mapping(target = "notBefore", ignore = true)
    @Mapping(target = "federationLink", ignore = true)
    @Mapping(target = "serviceAccountClientId", ignore = true)
    @Mapping(target = "groups", ignore = true)
    @Mapping(target = "origin", ignore = true)
    @Mapping(target = "disableableCredentialTypes", ignore = true)
    @Mapping(target = "access", ignore = true)
    @Mapping(target = "rawAttributes", ignore = true)
    @Mapping(target = "applicationRoles", ignore = true)
    void updateEntity(User dto, @MappingTarget UserRepresentation entity);

    default List<CredentialRepresentation> passwordToCredentialRepresentation(String password) {
        if (password == null) {
            return Collections.emptyList();
        }
        var credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
        credentialRepresentation.setTemporary(Boolean.FALSE);
        credentialRepresentation.setValue(password);
        return Collections.singletonList(credentialRepresentation);
    }

    @AfterMapping
    default void setParticipantIdAttribute(User user, @MappingTarget UserRepresentation representation) {
        if (user.getParticipantId() != null || user.getOrganization() != null) {
            var attrs = new HashMap<String, List<String>>();
            if (user.getParticipantId() != null) {
                attrs.put("participantId", List.of(user.getParticipantId()));
            }
            if (user.getOrganization() != null) {
                attrs.put("organization", List.of(user.getOrganization()));
            }
            representation.setAttributes(attrs);
        }
    }

    @Mapping(target = "clientRole", constant = "true")
    @Mapping(target = "composites", ignore = true)
    @Mapping(target = "composite", ignore = true)
    @Mapping(target = "containerId", ignore = true)
    @Mapping(target = "attributes", ignore = true)
    @Mapping(target = "name", source = "code")
    RoleRepresentation toRepresentation(Role role);

    List<User> toUserList(List<UserRepresentation> userRepresentations);

    @Mapping(target = "roles", source = "realmRoles")
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "participantId", ignore = true)
    @Mapping(target = "organization", ignore = true)
    User toUser(UserRepresentation userRepresentation);

    List<Role> toRoleList(List<RoleRepresentation> roles);

    @Mapping(target = "code", source = "name")
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "builtIn", ignore = true)
    @Mapping(target = "assignedIdentityAttributes", ignore = true)
    @Mapping(target = "name", ignore = true)
    Role toRole(RoleRepresentation role);
}
