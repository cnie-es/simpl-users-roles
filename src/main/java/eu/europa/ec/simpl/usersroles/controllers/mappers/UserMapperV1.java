package eu.europa.ec.simpl.usersroles.controllers.mappers;

import eu.europa.ec.simpl.api.usersroles.v1.model.RoleDTO;
import eu.europa.ec.simpl.api.usersroles.v1.model.UserDTO;
import eu.europa.ec.simpl.usersroles.models.Role;
import eu.europa.ec.simpl.usersroles.models.User;
import eu.europa.ec.simpl.usersroles.models.UserFilter;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserMapperV1 {

    @Mapping(target = "participantId", ignore = true)
    @Mapping(target = "organization", ignore = true)
    User toUser(UserDTO user);

    List<User> toUsers(List<UserDTO> users);

    List<UserDTO> toUserList(List<User> users);

    List<RoleDTO> toRoleDTOList(@NotNull List<Role> roles);

    @Mapping(target = "name", source = "code")
    RoleDTO toRoleDTO(Role role);

    UserDTO toUserDTO(@NotNull User user);

    default UserFilter toKeycloakUserFilterNotNull(
            String username, String firstName, String lastName, String email, Boolean enabled) {
        var result = toKeycloakUserFilter(username, firstName, lastName, email, enabled);
        if (result == null) {
            result = new UserFilter();
        }
        return result;
    }

    UserFilter toKeycloakUserFilter(String username, String firstName, String lastName, String email, Boolean enabled);
}
