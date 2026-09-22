package eu.europa.ec.simpl.usersroles.properties;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "client")
public record MtlsClientProperties(AuthorityProperties authority) {

    /**
     * @param url Endpoint URL for the authority service
     *
     */
    public record AuthorityProperties(URI url) {}
}
