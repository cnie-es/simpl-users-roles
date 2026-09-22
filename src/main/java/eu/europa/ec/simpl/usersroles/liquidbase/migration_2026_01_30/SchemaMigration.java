package eu.europa.ec.simpl.usersroles.liquidbase.migration_2026_01_30;

import eu.europa.ec.simpl.usersroles.liquidbase.AbstractRolePersistenceSchemaMigration;
import eu.europa.ec.simpl.usersroles.models.Role;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ConfigurableApplicationContext;

@Log4j2
public class SchemaMigration extends AbstractRolePersistenceSchemaMigration {

    protected static final Set<String> BUILT_IN_ROLES = Set.of(
            "T2IAA_M",
            "NOTARY",
            "ONBOARDER_M",
            "IATTR_M",
            "T1UAR_M",
            "APPLICANT",
            "Ro-MU-CA",
            "Ro-MU-A",
            "Ro-SD-A",
            "Ro-Pa-A",
            "SD_PUBLISHER",
            "SD_CONSUMER",
            "GA_SCHEMA_ADMIN",
            "GA_SCHEMA_VIEWER",
            "KIBANA_BUSINESS_USER",
            "KIBANA_ADMIN",
            "ORCH_DEVELOPER",
            "ORCH_ADMIN",
            "INFRA_ADMIN",
            "INFRA_DEPLOYER");

    protected static final String GET_ROLE_QUERY = "select code from role where code = ?";
    protected static final String INSERT_ROLE_ST =
            "insert into role (id, code, name, description, builtin, enabled, creation_timestamp, last_update_timestamp) values (?, ?, ?, ?, ?, ?, now(), now())";

    public SchemaMigration() {
        super();
    }

    protected SchemaMigration(Supplier<ConfigurableApplicationContext> supplierConfigurableApplicationContext) {
        super(supplierConfigurableApplicationContext);
    }

    @Override
    protected void migrateData() throws SQLException {
        persistKcRoles();
    }

    private void persistKcRoles() throws SQLException {

        log.info("Importing realm roles from KC to persistence");

        var rolePersistenceMigrationConfig =
                dbSeedingProperties.rolePersistenceMigration().excludeRoles();

        List<String> excludedRoles = StringUtils.isNotBlank(rolePersistenceMigrationConfig)
                ? Arrays.stream(rolePersistenceMigrationConfig.trim().split(","))
                        .map(String::trim)
                        .toList()
                : Collections.emptyList();

        var kcRoles = roleAdapter.getRolesList();

        log.info("Importing realm roles from KC to persistence - keycloak roles: [{}]", kcRoles);

        var imported = new HashSet<String>();
        var skipped = new HashSet<String>();

        try (var existsPs = connection.prepareStatement(GET_ROLE_QUERY);
                var insRolePs = connection.prepareStatement(INSERT_ROLE_ST)) {

            for (var role : kcRoles) {

                insRolePs.clearParameters();

                if (excludedRoles.contains(role.getCode()) || roleAlreadyPersisted(existsPs, role)) {
                    log.info(
                            "Importing realm roles from KC to persistence - [ROLE: {}] already persisted",
                            role.getCode());
                    skipped.add(role.getCode());
                    continue;
                }

                var isRoleBuiltIn = BUILT_IN_ROLES.contains(role.getCode());

                insRolePs.setObject(1, role.getId());
                insRolePs.setString(2, role.getCode());
                insRolePs.setString(3, role.getCode());
                insRolePs.setString(4, role.getDescription());
                insRolePs.setBoolean(5, isRoleBuiltIn);
                insRolePs.setBoolean(6, true);
                insRolePs.addBatch();

                imported.add(role.getCode());
            }

            insRolePs.executeBatch();
        }

        log.info("Successfully imported KC roles into persistence [IMPORTED: {}] - [SKIPPED: {}]", imported, skipped);
    }

    private boolean roleAlreadyPersisted(PreparedStatement existsPs, Role role) throws SQLException {
        existsPs.clearParameters();
        existsPs.setString(1, role.getCode());
        try (var rs = existsPs.executeQuery()) {
            return rs.next();
        }
    }

    protected void setConnection(Connection connection) {
        this.connection = connection;
    }
}
