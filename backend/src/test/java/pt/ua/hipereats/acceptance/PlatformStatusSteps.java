package pt.ua.hipereats.acceptance;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;

/**
 * Steps of {@code platform_status.feature}: checks that the acceptance test setup reaches the
 * running backend.
 */
public class PlatformStatusSteps {

    private final RestTestClient client;
    private ResponseSpec response;

    public PlatformStatusSteps(RestTestClient client) {
        this.client = client;
    }

    @When("a client asks for the platform status")
    public void aClientAsksForThePlatformStatus() {
        response = client.get().uri("/api/status").exchange();
    }

    @Then("the backend reports that it is up")
    public void theBackendReportsThatItIsUp() {
        response.expectStatus().isOk()
                .expectBody()
                .jsonPath("$.service").isEqualTo("hipereats-backend")
                .jsonPath("$.status").isEqualTo("UP");
    }
}
