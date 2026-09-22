package eu.europa.ec.simpl.usersroles.liquidbase;

import eu.europa.ec.simpl.common.autoconfigurations.InstantSourceAutoConfiguration;
import eu.europa.ec.simpl.usersroles.adapters.impl.RoleAdapterImpl;
import eu.europa.ec.simpl.usersroles.adapters.mappers.KeycloakMapperImpl;
import eu.europa.ec.simpl.usersroles.configurations.KeycloakConfig;
import eu.europa.ec.simpl.usersroles.properties.DBSeedingProperties;
import eu.europa.ec.simpl.usersroles.properties.KeycloakProperties;
import lombok.NoArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({InstantSourceAutoConfiguration.class, KeycloakConfig.class, KeycloakMapperImpl.class, RoleAdapterImpl.class})
@ConditionalOnProperty(name = RolePersistenceMigrationSpringConfig.LIQUIBASE_MIGRATION_PROPERTY, havingValue = "true")
@EnableConfigurationProperties({KeycloakProperties.class, DBSeedingProperties.class})
@NoArgsConstructor
public class RolePersistenceMigrationSpringConfig {

    public static final String LIQUIBASE_MIGRATION_PROPERTY = "simpl.liquibase-rolepersistence-migration";
}
