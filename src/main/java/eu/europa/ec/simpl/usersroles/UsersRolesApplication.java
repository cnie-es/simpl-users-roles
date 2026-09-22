package eu.europa.ec.simpl.usersroles;

import eu.europa.ec.simpl.api.identityprovider.v1.model.ParticipantWithIdentityAttributesDTO;
import eu.europa.ec.simpl.common.messaging.EnableMessageConsumer;
import java.security.Security;
import lombok.Getter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
@EnableMessageConsumer
public class UsersRolesApplication {

    @Getter
    private static String[] applicationArguments = {};

    public static void main(String[] args) {

        applicationArguments = args;
        Security.addProvider(new BouncyCastleProvider());
        SpringDocUtils.getConfig().addParentType(ParticipantWithIdentityAttributesDTO.class.getSimpleName());
        SpringApplication.run(UsersRolesApplication.class, args);
    }
}
