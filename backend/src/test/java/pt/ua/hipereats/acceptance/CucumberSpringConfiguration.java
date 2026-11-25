package pt.ua.hipereats.acceptance;

import io.cucumber.spring.CucumberContextConfiguration;
import pt.ua.hipereats.IntegrationTest;

/**
 * One Spring context for all scenarios: the whole application on a random port, called over real
 * HTTP through the REST API, as a client would, on a real PostgreSQL in Testcontainers.
 */
@CucumberContextConfiguration
@IntegrationTest
public class CucumberSpringConfiguration {
}
