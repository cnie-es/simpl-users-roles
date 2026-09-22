package eu.europa.ec.simpl.usersroles.liquidbase;

import eu.europa.ec.simpl.common.exceptions.RuntimeWrapperException;
import java.sql.Connection;
import java.sql.SQLException;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import lombok.extern.log4j.Log4j2;

@Log4j2
public abstract class AbstractSchemaMigration implements CustomTaskChange {

    protected Connection connection;

    protected abstract void migrateData() throws SQLException;

    @Override
    public void execute(Database database) throws CustomChangeException {

        var jdbcConnection = (JdbcConnection) database.getConnection();
        connection = jdbcConnection.getUnderlyingConnection();
        try {
            connection.setAutoCommit(false);

            migrateData();

            connection.commit();
        } catch (Exception e) {
            try {
                connection.rollback();
            } catch (SQLException e1) {
                log.error("Rollback failed due to SQL exception", e1);
                throw new RuntimeWrapperException(e);
            }
            throw new RuntimeWrapperException(e);
        }
    }

    @Override
    public String getConfirmationMessage() {
        return "Data migration %s completed successfully.".formatted(getClass().getName());
    }

    @Override
    public void setUp() {
        // No operation
    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {
        // No operation
    }

    @Override
    public ValidationErrors validate(Database database) {
        return new ValidationErrors();
    }
}
