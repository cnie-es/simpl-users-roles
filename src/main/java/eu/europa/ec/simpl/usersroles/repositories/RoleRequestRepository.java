package eu.europa.ec.simpl.usersroles.repositories;

import eu.europa.ec.simpl.usersroles.entities.RoleRequestEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface RoleRequestRepository
        extends JpaRepository<RoleRequestEntity, UUID>, JpaSpecificationExecutor<RoleRequestEntity> {

    @Query(
            """
      select rr from RoleRequestEntity rr
      left join fetch rr.rolesRequested
      where rr.id = :id
    """)
    Optional<RoleRequestEntity> findWithRolesRequestedById(UUID id);
}
