package eu.europa.ec.simpl.usersroles.repositories;

import eu.europa.ec.simpl.usersroles.entities.RoleRequestedEntity;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRequestedRepository extends JpaRepository<RoleRequestedEntity, UUID> {

    List<RoleRequestedEntity> findByRoleRequest_UserEmailAndRoleIn(String email, Collection<String> roles);
}
