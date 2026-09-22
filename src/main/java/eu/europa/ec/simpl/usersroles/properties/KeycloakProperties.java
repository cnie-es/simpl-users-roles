package eu.europa.ec.simpl.usersroles.properties;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param url Base URL of the Keycloak server
 */
@ConfigurationProperties(prefix = "keycloak")
public record KeycloakProperties(
        String url, ClientProperties master, ClientProperties app, RoleMigrationProperties clientToRealmRoleMigration) {

    /**
     * @param realm Realm associated with the application in Keycloak
     * @param clientId Client ID for the application in Keycloak
     * @param user Username of the application client in Keycloak
     * @param password Password for the application Keycloak user
     */
    public record ClientProperties(String realm, String clientId, String user, String password) {}

    /**
     * @param clientIds List of client IDs used during Keycloak role migration
     * @param enabled Enables the migration of client roles to realm roles in Keycloak
     */
    public record RoleMigrationProperties(boolean enabled, List<String> clientIds) {}
}
