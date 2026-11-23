package pt.ua.hipereats.acceptance;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import pt.ua.hipereats.PostgresTestcontainersConfiguration;

/**
 * One Spring context for all scenarios: the whole application on a random port, called over real
 * HTTP through the REST API, as a client would, on a real PostgreSQL in Testcontainers.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(PostgresTestcontainersConfiguration.class)
public class CucumberSpringConfiguration {
}
