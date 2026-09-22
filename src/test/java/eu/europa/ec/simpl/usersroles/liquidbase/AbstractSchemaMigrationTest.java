package eu.europa.ec.simpl.usersroles.liquidbase;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import eu.europa.ec.simpl.common.exceptions.RuntimeWrapperException;
import java.sql.Connection;
import java.sql.SQLException;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.ValidationErrors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AbstractSchemaMigrationTest {
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private AbstractSchemaMigration abstractSchemaMigration;

    @Mock
    private Connection connection;

    @Mock
    private JdbcConnection jdbcConnection;

    @Mock
    private Database database;

    @Test
    void executeTest() throws SQLException, CustomChangeException {
        given(database.getConnection()).willReturn(jdbcConnection);
        given(jdbcConnection.getUnderlyingConnection()).willReturn(connection);
        willDoNothing().given(abstractSchemaMigration).migrateData();
        abstractSchemaMigration.execute(database);
    }

    @Test
    void executeWithExceptionTest() throws SQLException {

        given(database.getConnection()).willReturn(jdbcConnection);
        given(jdbcConnection.getUnderlyingConnection()).willReturn(connection);
        willThrow(new SQLException("Test Exception"))
                .given(abstractSchemaMigration)
                .migrateData();

        assertThrows(RuntimeWrapperException.class, () -> abstractSchemaMigration.execute(database));
        verify(connection).rollback();
    }

    @Test
    void executeWithRollbackExceptionTest() throws SQLException {

        given(database.getConnection()).willReturn(jdbcConnection);
        given(jdbcConnection.getUnderlyingConnection()).willReturn(connection);
        willThrow(new SQLException("Test Exception"))
                .given(abstractSchemaMigration)
                .migrateData();
        willThrow(new SQLException("Rollback Exception")).given(connection).rollback();

        assertThrows(RuntimeWrapperException.class, () -> abstractSchemaMigration.execute(database));
        verify(connection).rollback();
    }

    @Test
    void getConfirmationMessageTest() {
        String message = abstractSchemaMigration.getConfirmationMessage();
        assertTrue(message.contains(abstractSchemaMigration.getClass().getName()));
    }

    @Test
    void setUpTest() {
        abstractSchemaMigration.setUp();
    }

    @Test
    void setFileOpenerTest() {
        abstractSchemaMigration.setFileOpener(null);
    }

    @Test
    void validateTest() {
        ValidationErrors errors = abstractSchemaMigration.validate(database);
        assertNotNull(errors);
        assertFalse(errors.hasErrors());
    }
}
