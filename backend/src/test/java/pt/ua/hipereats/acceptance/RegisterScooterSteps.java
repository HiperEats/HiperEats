package pt.ua.hipereats.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.net.URI;
import javax.sql.DataSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;
import pt.ua.hipereats.FleetTestData;

/**
 * Steps of {@code HE-29_register_scooter.feature}.
 *
 * <p>"The scooter" of a scenario is the one registered in it, kept in {@link ScenarioScooterSteps},
 * which also checks its state ({@code the scooter is <state>}).
 */
public class RegisterScooterSteps {

    private final RestTestClient client;
    private final FleetTestData data;
    private final ScenarioScooterSteps scooter;
    private long storeId;
    private String scooterCode;
    private ResponseSpec registration;

    public RegisterScooterSteps(RestTestClient client, DataSource dataSource,
            ScenarioScooterSteps scooter) {
        this.client = client;
        this.data = new FleetTestData(dataSource);
        this.scooter = scooter;
    }

    @Given("a store with capacity {int} and {int} scooters")
    public void aStoreWithCapacityAndScooters(int capacity, int scooters) {
        storeId = data.createStore(capacity);
        data.addScooters(storeId, scooters);
    }

    @When("the operator registers a standard scooter at that store")
    public void theOperatorRegistersAStandardScooterAtThatStore() {
        scooterCode = FleetTestData.uniqueCode();
        scooter.use(scooterCode);
        registration = client.post().uri("/api/v1/scooters")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"code": "%s", "type": "STANDARD", "storeId": %d}
                        """.formatted(scooterCode, storeId))
                .exchange();
    }

    @Then("the scooter is registered at that store")
    public void theScooterIsRegisteredAtThatStore() {
        registration.expectStatus().isCreated();
        URI location = registration.returnResult().getResponseHeaders().getLocation();
        assertThat(location).as("Location of the registered scooter")
                .hasToString("/api/v1/scooters/" + scooterCode);
        client.get().uri(location.toString())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(scooterCode)
                .jsonPath("$.type").isEqualTo("STANDARD")
                .jsonPath("$.storeId").isEqualTo(storeId);
    }

    @Then("the store has {int} scooters")
    public void theStoreHasScooters(int scooters) {
        assertThat(data.countScooters(storeId)).isEqualTo(scooters);
    }

    @Then("the registration is refused because the store is at capacity")
    public void theRegistrationIsRefusedBecauseTheStoreIsAtCapacity() {
        registration.expectStatus().isEqualTo(422)
                .expectBody().jsonPath("$.rule").isEqualTo("BR13");
    }
}
