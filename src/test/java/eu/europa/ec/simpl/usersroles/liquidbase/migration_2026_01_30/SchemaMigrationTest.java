package eu.europa.ec.simpl.usersroles.liquidbase.migration_2026_01_30;

import static eu.europa.ec.simpl.usersroles.liquidbase.migration_2026_01_30.SchemaMigration.BUILT_IN_ROLES;
import static eu.europa.ec.simpl.usersroles.liquidbase.migration_2026_01_30.SchemaMigration.GET_ROLE_QUERY;
import static eu.europa.ec.simpl.usersroles.liquidbase.migration_2026_01_30.SchemaMigration.INSERT_ROLE_ST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import eu.europa.ec.simpl.usersroles.adapters.RoleAdapter;
import eu.europa.ec.simpl.usersroles.models.Role;
import eu.europa.ec.simpl.usersroles.properties.DBSeedingProperties;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.instancio.junit.InstancioSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ConfigurableApplicationContext;

@ExtendWith(MockitoExtension.class)
class SchemaMigrationTest {

    @Mock
    DBSeedingProperties.RolePersistenceMigration rolePersistenceMigration;

    @InjectMocks
    DBSeedingProperties dbSeedingProperties;

    @Mock
    RoleAdapter roleAdapter;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Connection connection;

    @Mock
    PreparedStatement existsPs;

    @Mock
    PreparedStatement insRolePs;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ConfigurableApplicationContext configurableApplicationContext;

    @Mock
    ResultSet rs;

    private SchemaMigration schemaMigration;

    @BeforeEach
    void setUp() {
        schemaMigration = spy(new SchemaMigration(() -> configurableApplicationContext));
        schemaMigration.setConnection(connection);
        given(configurableApplicationContext.getBean(RoleAdapter.class)).willReturn(roleAdapter);
        given(configurableApplicationContext.getBean(DBSeedingProperties.class)).willReturn(dbSeedingProperties);
    }

    @ParameterizedTest
    @InstancioSource(samples = 3)
    void migrateKcRolesPersistenceTest(List<Role> roles) throws Exception {

        var excludedRoles = Set.of("default-roles-participant", "offline_access", "uma_authorization");

        roles.getLast().setCode("NOTARY");
        roles.getLast().setName("NOTARY");

        given(rolePersistenceMigration.excludeRoles()).willReturn(String.join(",", excludedRoles));

        given(roleAdapter.getRolesList()).willReturn(roles);

        given(connection.prepareStatement(GET_ROLE_QUERY)).willReturn(existsPs);
        given(connection.prepareStatement(INSERT_ROLE_ST)).willReturn(insRolePs);
        given(existsPs.executeQuery()).willReturn(rs);

        var existingCodes = Set.of(roles.getFirst().getCode(), roles.getLast().getCode());

        AtomicReference<String> lastCheckedCode = new AtomicReference<>();

        doAnswer(inv -> {
                    lastCheckedCode.set(inv.getArgument(1, String.class));
                    return null;
                })
                .when(existsPs)
                .setString(eq(1), anyString());

        given(rs.next()).willAnswer(inv -> existingCodes.contains(lastCheckedCode.get()));

        schemaMigration.setUp();
        schemaMigration.migrateData();

        var insertedCodeCaptor = ArgumentCaptor.forClass(String.class);
        verify(insRolePs, atLeast(0)).setString(eq(2), insertedCodeCaptor.capture());

        var insertedCodes = new HashSet<>(insertedCodeCaptor.getAllValues());

        var expectedInserted = roles.stream().map(Role::getCode).collect(Collectors.toCollection(HashSet::new));
        expectedInserted.removeAll(existingCodes);

        assertEquals(expectedInserted, insertedCodes);

        assertThat(insertedCodes).doesNotContainAnyElementsOf(BUILT_IN_ROLES);

        assertThat(insertedCodes).doesNotContainAnyElementsOf(excludedRoles);

        verify(insRolePs, times(expectedInserted.size())).addBatch();
        verify(insRolePs, times(1)).executeBatch();
    }
}
