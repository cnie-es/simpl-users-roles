package eu.europa.ec.simpl.usersroles.repositories;

import eu.europa.ec.simpl.usersroles.entities.IdentityAttributeRole;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface IdentityAttributeRolesRepository
        extends JpaRepository<IdentityAttributeRole, UUID>, JpaSpecificationExecutor<IdentityAttributeRole> {

    @Modifying
    @Query(
            "update IdentityAttributeRole iar set iar.enabled = case when (iar.idaCode in :idaCodes) then true else false end")
    void updateAssignments(List<String> idaCodes);

    List<IdentityAttributeRole> findByRole_CodeAndEnabledTrue(String roleCode);

    void deleteByRole_Code(String roleCode);

    long deleteByRole_CodeAndIdaCode(String roleCode, String idaCode);

    @Modifying
    @Query(
            """
            update IdentityAttributeRole iar
            set iar.enabled = false
            where iar.role.code = :roleCode
            """)
    void disableMappingByRoleCode(String roleCode);

    default boolean existsAtLeastOne() {
        return count() > 0;
    }
}
