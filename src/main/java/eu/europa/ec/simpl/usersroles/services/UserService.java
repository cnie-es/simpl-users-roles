package eu.europa.ec.simpl.usersroles.services;

import eu.europa.ec.simpl.usersroles.models.Role;
import eu.europa.ec.simpl.usersroles.models.User;
import eu.europa.ec.simpl.usersroles.models.UserFilter;
import eu.europa.ec.simpl.usersroles.models.UserPage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface UserService {

    CreateUserOutcome createUser(User userDTO);

    GetUserOutcome getUserByEmail(String email);

    Collection<CreateUserOutcome> importUsers(List<User> users);

    CreateUserOutcome importUser(User user);

    GetUserRolesOutcome getUserRoles(UUID uuid);

    GetUserOutcome getUserById(UUID userId);

    UpdateUserOutcome updateUser(UUID userId, User userDTO);

    DeleteUserOutcome deleteUser(UUID userId);

    DeleteUserOutcome deleteByEmail(String email);

    UpdateUserOutcome updateUserRoles(UpdateUserRolesArgs args);

    SearchUserOutcome search(UserFilter filter, Pageable pageable);

    LogoutOutcome logout();

    sealed interface CreateUserOutcome {

        record Success(@NotBlank User createdUser) implements CreateUserOutcome {}

        record AlreadyExists(String email) implements CreateUserOutcome {}
    }

    sealed interface UpdateUserRolesArgs {
        UUID userId();

        record WithRoleNames(@NotNull UUID userId, @Valid @NotNull List<@NotBlank String> roles)
                implements UpdateUserRolesArgs {}

        record WithRoleIds(@NotNull UUID userId, @Valid @NotNull List<@NotNull UUID> roles)
                implements UpdateUserRolesArgs {}
    }

    sealed interface UpdateUserOutcome {
        record Success() implements UpdateUserOutcome {}

        record NotFound(UUID userId) implements UpdateUserOutcome {}

        record RolesNotFound(List<RoleService.RoleIdentifier> rolesNotFond) implements UpdateUserOutcome {}

        record RolesDisabled(List<Role> rolesDisabled) implements UpdateUserOutcome {}
    }

    sealed interface DeleteUserOutcome {
        record Success(@NotNull UUID userId) implements DeleteUserOutcome {}

        record NotFoundByEmail(@NotBlank String email) implements DeleteUserOutcome {}

        record NotFoundById(@NotNull UUID userId) implements DeleteUserOutcome {}
    }

    sealed interface SearchUserOutcome {
        record Success(UserPage users) implements SearchUserOutcome {}
    }

    sealed interface GetUserOutcome {
        record Success(@NotNull User user) implements GetUserOutcome {}

        record NotFoundByEmail(@NotBlank String email) implements GetUserOutcome {}

        record NotFoundById(@NotNull UUID userId) implements GetUserOutcome {}
    }

    sealed interface GetUserRolesOutcome {
        record Success(@NotNull List<Role> roles) implements GetUserRolesOutcome {}

        record UserNotFound(UUID userId) implements GetUserRolesOutcome {}

        record RolesNotFound(@NotNull @NotEmpty List<RoleService.RoleIdentifier> rolesNotFond)
                implements GetUserRolesOutcome {}
    }

    sealed interface LogoutOutcome {
        record Success() implements LogoutOutcome {}
    }
}
