package eu.europa.ec.simpl.usersroles.configurations;

import eu.europa.ec.simpl.common.aspects.Stringifier;
import java.util.List;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.keycloak.representations.idm.RealmRepresentation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingConfig {

    @Bean
    public List<Stringifier> keycloakStringifiers() {
        return List.of(Stringifier.create(
                RealmRepresentation.class, r -> new ReflectionToStringBuilder(r, ToStringStyle.NO_CLASS_NAME_STYLE)
                        .setIncludeFieldNames("id", "realm")
                        .build()));
    }
}
