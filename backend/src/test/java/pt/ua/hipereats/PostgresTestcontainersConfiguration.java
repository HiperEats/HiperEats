package pt.ua.hipereats;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * A real PostgreSQL 18 for the integration tests, started in Docker by Testcontainers, with the same
 * image as docker-compose.yml.
 *
 * <p>Tests do not import it directly: they use {@link IntegrationTest}, which imports it. The
 * container is a Spring bean, so
 * Spring starts it before the beans that use it, stops it after them, and tests that share the same
 * context also share the same database (TP03, appendix A4). {@code @ServiceConnection} gives the
 * application the connection details: no URL, user or password in the tests.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        // Same image as docker-compose.yml: Debian, not alpine, so text sorts with language rules.
        return new PostgreSQLContainer("postgres:18");
    }
}
