package eu.europa.ec.simpl.usersroles.liquidbase;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@ExtendWith(MockitoExtension.class)
class RolePersistenceMigrationSpringConfigTest {

    @Test
    void testSpringConfigShouldHaveCorrectLiquibaseMigrationProperty() {
        String property = RolePersistenceMigrationSpringConfig.LIQUIBASE_MIGRATION_PROPERTY;

        assertThat(property).as("property value").isEqualTo("simpl.liquibase-rolepersistence-migration");
    }

    @Test
    void testSpringConfigShouldBeAnnotatedWithConfiguration() {
        boolean hasConfigurationAnnotation =
                RolePersistenceMigrationSpringConfig.class.isAnnotationPresent(Configuration.class);

        assertThat(hasConfigurationAnnotation)
                .as("SpringConfig should be annotated with @Configuration")
                .isTrue();
    }

    @Test
    void testSpringConfigShouldBeAnnotatedWithImport() {
        boolean hasImportAnnotation = RolePersistenceMigrationSpringConfig.class.isAnnotationPresent(Import.class);

        assertThat(hasImportAnnotation)
                .as("SpringConfig should be annotated with @Import")
                .isTrue();
    }

    @Test
    void testSpringConfigShouldBeAnnotatedWithEnableConfigurationProperties() {
        boolean hasEnableConfigurationPropertiesAnnotation =
                RolePersistenceMigrationSpringConfig.class.isAnnotationPresent(EnableConfigurationProperties.class);

        assertThat(hasEnableConfigurationPropertiesAnnotation)
                .as("SpringConfig should be annotated with @EnableConfigurationProperties")
                .isTrue();
    }
}
