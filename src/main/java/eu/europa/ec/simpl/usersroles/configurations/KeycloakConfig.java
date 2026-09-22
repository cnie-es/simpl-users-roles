package eu.europa.ec.simpl.usersroles.configurations;

import eu.europa.ec.simpl.usersroles.properties.KeycloakProperties;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakConfig {

    private final KeycloakProperties properties;

    public KeycloakConfig(KeycloakProperties properties) {
        this.properties = properties;
    }

    @Bean
    public Keycloak keycloak() {
        return KeycloakBuilder.builder()
                .grantType(OAuth2Constants.PASSWORD)
                .realm(properties.master().realm())
                .clientId(properties.master().clientId())
                .username(properties.master().user())
                .password(properties.master().password())
                .serverUrl(properties.url())
                .build();
    }

    @Bean
    public RealmResource appRealm(Keycloak keycloak) {
        return keycloak.realm(properties.app().realm());
    }
}
