package pt.ua.hipereats;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;

/**
 * The one configuration of every test that starts the whole application: the backend on a random
 * port, a {@code RestTestClient} pointing at it, and a real PostgreSQL 18 in Testcontainers.
 *
 * <p>Use it on every {@code *IT} class and on the Cucumber context configuration. Because they all
 * have the same configuration, Spring starts one application context and one PostgreSQL container
 * for the whole {@code ./mvnw verify} run and reuses them.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(PostgresTestcontainersConfiguration.class)
public @interface IntegrationTest {
}
