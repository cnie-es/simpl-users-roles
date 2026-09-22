package eu.europa.ec.simpl.usersroles.repositories;

import eu.europa.ec.simpl.usersroles.entities.RoleEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID>, JpaSpecificationExecutor<RoleEntity> {

    Optional<RoleEntity> findByCode(String code);

    List<RoleEntity> findByIdIn(Collection<UUID> ids);

    List<RoleEntity> findByCodeIn(Collection<String> codes);
}
