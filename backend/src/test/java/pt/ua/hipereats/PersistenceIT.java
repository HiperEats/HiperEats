package pt.ua.hipereats;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * The persistence base (HE-45): the application runs on PostgreSQL 18 and Flyway creates the
 * schema when the application starts.
 */
@SpringBootTest
@Import(PostgresTestcontainersConfiguration.class)
class PersistenceIT {

    @Autowired
    private DataSource dataSource;

    @Test
    void whenApplicationStarts_thenItRunsOnPostgreSQL18() throws SQLException {
        String serverVersion = queryForString("SHOW server_version");

        assertThat(serverVersion).startsWith("18");
    }

    @Test
    void whenApplicationStarts_thenFlywayAppliesTheBaselineMigration() throws SQLException {
        String baselineApplied =
                queryForString("SELECT success FROM flyway_schema_history WHERE version = '1'");

        assertThat(baselineApplied).isEqualTo("t");
    }

    private String queryForString(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(sql)) {
            assertThat(result.next()).as("query returns a row: %s", sql).isTrue();
            return result.getString(1);
        }
    }
}
