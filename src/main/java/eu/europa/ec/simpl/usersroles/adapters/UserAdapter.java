package eu.europa.ec.simpl.usersroles.adapters;

import eu.europa.ec.simpl.usersroles.models.Role;
import eu.europa.ec.simpl.usersroles.models.User;
import eu.europa.ec.simpl.usersroles.models.UserFilter;
import eu.europa.ec.simpl.usersroles.models.UserPage;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

/**
 * An outbound port for HTTP keycloak operations
 */
public interface UserAdapter {

    Optional<User> getUser(UUID userId);

    Optional<User> getUserByEmail(String email);

    CreateUserOutcome createUser(CreateUserArgs args);

    sealed interface CreateUserArgs {
        User user();

        record Default(User user) implements CreateUserArgs {}
    }

    sealed interface CreateUserOutcome {
        record Success(User user) implements CreateUserOutcome {}

        record AlreadyExists() implements CreateUserOutcome {}
    }

    void deleteUser(UUID userId);

    UserPage getUsers(@NotNull UserFilter filter, Pageable pageable);

    ReplaceUserRolesOutcome replaceUserRoles(ReplaceUserRolesArgs args);

    LogoutOutcome logout(LogoutArgs args);

    sealed interface LogoutArgs {
        String sid();

        record Default(String sid) implements LogoutArgs {}
    }

    sealed interface LogoutOutcome {
        record Success() implements LogoutOutcome {}
    }

    sealed interface ReplaceUserRolesArgs {
        UUID userId();

        record WithRoleNames(UUID userId, List<String> roles) implements ReplaceUserRolesArgs {}

        record WithRoleIds(UUID userId, List<UUID> roles) implements ReplaceUserRolesArgs {}
    }

    sealed interface ReplaceUserRolesOutcome {
        record Success() implements ReplaceUserRolesOutcome {}
    }

    List<Role> getUserRoles(UUID userId);

    void updateUser(@NotBlank UUID uuid, @Valid @NotNull User userDTO);
}
