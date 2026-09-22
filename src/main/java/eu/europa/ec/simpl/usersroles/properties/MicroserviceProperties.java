package eu.europa.ec.simpl.usersroles.properties;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "microservice")
public record MicroserviceProperties(AuthenticationProvider authenticationProvider) {

    /**
     * @param url URL of the external authentication provider service
     */
    public record AuthenticationProvider(URI url) {}
}
