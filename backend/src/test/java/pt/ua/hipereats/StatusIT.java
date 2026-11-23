package pt.ua.hipereats;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Starts the whole application on a random port and calls it over real HTTP.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(PostgresTestcontainersConfiguration.class)
class StatusIT {

    @Autowired
    private RestTestClient client;

    @Test
    void statusEndpointIsUp() {
        client.get().uri("/api/status")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.service").isEqualTo("hipereats-backend")
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void actuatorHealthIsUp() {
        client.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }
}
