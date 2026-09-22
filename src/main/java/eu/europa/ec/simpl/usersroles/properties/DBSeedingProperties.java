package eu.europa.ec.simpl.usersroles.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "database-seeding")
public record DBSeedingProperties(
        RoleAttributesMapping roleAttributesMapping, RolePersistenceMigration rolePersistenceMigration) {

    /**
     * @param enabled Enables initialization from RoleAttributesInitializerImpl
     * @param filePath Path to the file used by RoleAttributesInitializerImpl for seeding
     */
    public record RoleAttributesMapping(boolean enabled, String filePath) {}

    public record RolePersistenceMigration(String excludeRoles) {}
}
