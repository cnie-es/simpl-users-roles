package eu.europa.ec.simpl.usersroles;

import static eu.europa.ec.simpl.common.test.TestUtil.a;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import eu.europa.ec.simpl.usersroles.entities.RoleEntity;
import eu.europa.ec.simpl.usersroles.repositories.IdentityAttributeRolesRepository;
import eu.europa.ec.simpl.usersroles.repositories.RoleRepository;
import eu.europa.ec.simpl.usersroles.repositories.RoleRequestRepository;
import eu.europa.ec.simpl.usersroles.repositories.RoleRequestedRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
        properties = {
            "microservice.authentication-provider.url=http://localhost:8081",
            "keycloak.url=http://localhost:8080/auth",
            "keycloak.master.user=admin",
            "keycloak.master.password=admin",
            "keycloak.app.realm=authority",
            "simpl.kafka.topic.prefix=iaa",
            "simpl.kafka.inbox.topic.pattern=inbox",
            "spring.kafka.bootstrap-servers=localhost:9092",
            "client.authority.url=http://localhost:8086"
        })
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
class UsersRolesApplicationTest {

    @MockitoBean
    IdentityAttributeRolesRepository identityAttributeRolesRepository;

    @MockitoBean
    RoleRequestRepository roleRequestRepository;

    @MockitoBean
    RoleRequestedRepository roleRequestedRepository;

    @MockitoBean
    RoleRepository roleRepository;

    @Autowired
    ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        mockRoles();
        assertThat(applicationContext).isNotNull();
    }

    private void mockRoles() {

        var roleEntity = Optional.of(a(RoleEntity.class));

        given(roleRepository.findByCode(any())).willReturn(roleEntity);
    }
}
