package pt.ua.hipereats;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Starts the whole application on a random port and calls it over real HTTP.
 */
@IntegrationTest
class StatusIT {

    @Autowired
    private RestTestClient client;

    @Test
    void whenStatusRequested_thenServiceIsUp() {
        client.get().uri("/api/status")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.service").isEqualTo("hipereats-backend")
                .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void whenActuatorHealthRequested_thenStatusIsUp() {
        client.get().uri("/actuator/health")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("UP");
    }
}
