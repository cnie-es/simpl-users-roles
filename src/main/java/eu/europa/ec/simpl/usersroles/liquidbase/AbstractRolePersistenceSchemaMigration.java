package eu.europa.ec.simpl.usersroles.liquidbase;

import eu.europa.ec.simpl.usersroles.UsersRolesApplication;
import eu.europa.ec.simpl.usersroles.adapters.RoleAdapter;
import eu.europa.ec.simpl.usersroles.properties.DBSeedingProperties;
import java.util.function.Supplier;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public abstract class AbstractRolePersistenceSchemaMigration extends AbstractSchemaMigration {

    protected RoleAdapter roleAdapter;

    protected DBSeedingProperties dbSeedingProperties;

    protected Supplier<ConfigurableApplicationContext> supplierConfigurableApplicationContext;

    protected AbstractRolePersistenceSchemaMigration(
            Supplier<ConfigurableApplicationContext> supplierConfigurableApplicationContext) {
        this.supplierConfigurableApplicationContext = supplierConfigurableApplicationContext;
    }

    protected AbstractRolePersistenceSchemaMigration() {
        this(AbstractRolePersistenceSchemaMigration::createConfigurableApplicationContext);
    }

    @Override
    public void setUp() {
        super.setUp();
        var applicationContext = supplierConfigurableApplicationContext.get();
        this.roleAdapter = applicationContext.getBean(RoleAdapter.class);
        this.dbSeedingProperties = applicationContext.getBean(DBSeedingProperties.class);
    }

    private static ConfigurableApplicationContext createConfigurableApplicationContext() {
        return new SpringApplicationBuilder(RolePersistenceMigrationSpringConfig.class)
                .web(WebApplicationType.NONE)
                .properties(RolePersistenceMigrationSpringConfig.LIQUIBASE_MIGRATION_PROPERTY + "=true")
                .run(UsersRolesApplication.getApplicationArguments());
    }
}
