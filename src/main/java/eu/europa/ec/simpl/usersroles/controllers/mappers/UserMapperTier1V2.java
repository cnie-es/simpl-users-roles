package eu.europa.ec.simpl.usersroles.controllers.mappers;

import eu.europa.ec.simpl.api.usersroles.t1.v2.model.UpdateUserRequestDTO;
import eu.europa.ec.simpl.api.usersroles.t1.v2.model.UserDTO;
import eu.europa.ec.simpl.api.usersroles.t1.v2.model.UserSessionDataDTO;
import eu.europa.ec.simpl.api.usersroles.t1.v2.model.UsersPagedResponseDTO;
import eu.europa.ec.simpl.common.mappers.ToPagedResponse;
import eu.europa.ec.simpl.common.security.AuthService;
import eu.europa.ec.simpl.usersroles.models.User;
import eu.europa.ec.simpl.usersroles.models.UserFilter;
import eu.europa.ec.simpl.usersroles.models.UserPage;
import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserMapperTier1V2 {

    List<User> toUserList(List<UserDTO> users);

    // TODO sprint=26 manage federated users
    @Mapping(target = "participantId", ignore = true)
    @Mapping(target = "organization", ignore = true)
    User toUser(UserDTO user);

    @BeanMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    UserFilter toKeycloakUserFilter(String firstName, String lastName, String username, String email, Boolean enabled);

    // TODO manage federated users
    @Mapping(target = "federated", constant = "false")
    UserDTO toUserDTO(User user);

    @ToPagedResponse
    @BeanMapping(ignoreUnmappedSourceProperties = {"links", "resolvableType", "nextLink", "previousLink"})
    UsersPagedResponseDTO toUsersPagedResponseDTO(PagedModel<EntityModel<User>> page);

    @Mapping(target = "self", ignore = true)
    @Mapping(target = "first", ignore = true)
    @Mapping(target = "last", ignore = true)
    @Mapping(target = "prev", ignore = true)
    @Mapping(target = "next", ignore = true)
    void toUsersPagedResponseDTO(@MappingTarget UsersPagedResponseDTO usersPagedResponseDTO, UserPage userPage);

    @Mapping(target = ".", source = "content")
    @BeanMapping(ignoreUnmappedSourceProperties = {"links", "content"})
    User unwrapEntityModel(EntityModel<User> entityModel);

    @Mapping(target = "password", ignore = true)
    @Mapping(target = "participantId", ignore = true)
    @Mapping(target = "organization", ignore = true)
    User toUpdateUserRequestDTO(UpdateUserRequestDTO updateUserRequestDTO);

    UserSessionDataDTO toUserSessionDataDTO(AuthService authService);
}
