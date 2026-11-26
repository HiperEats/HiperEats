package pt.ua.hipereats.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.net.URI;
import java.util.Locale;
import javax.sql.DataSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;
import pt.ua.hipereats.FleetTestData;
import pt.ua.hipereats.domain.fleet.ScooterState;

/**
 * Steps of {@code HE-29_register_scooter.feature}.
 *
 * <p>"The scooter" of a scenario is the one registered in it, kept by its code. The step
 * {@code the scooter is <state>} is written for every state, because the features of HE-30 and
 * HE-37 use the same sentence; when they need it, move the scenario's scooter to a class that all
 * their steps share.
 */
public class RegisterScooterSteps {

    private final RestTestClient client;
    private final FleetTestData data;
    private long storeId;
    private String scooterCode;
    private ResponseSpec registration;

    public RegisterScooterSteps(RestTestClient client, DataSource dataSource) {
        this.client = client;
        this.data = new FleetTestData(dataSource);
    }

    /** A state as the features write it: "inactive", "in use", ... */
    @ParameterType("inactive|available|reserved|in use|maintenance|retired")
    public ScooterState scooterState(String state) {
        return ScooterState.valueOf(state.replace(' ', '_').toUpperCase(Locale.ROOT));
    }

    @Given("a store with capacity {int} and {int} scooters")
    public void aStoreWithCapacityAndScooters(int capacity, int scooters) {
        storeId = data.createStore(capacity);
        data.addScooters(storeId, scooters);
    }

    @When("the operator registers a standard scooter at that store")
    public void theOperatorRegistersAStandardScooterAtThatStore() {
        scooterCode = FleetTestData.uniqueCode();
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

    @Then("the scooter is {scooterState}")
    public void theScooterIs(ScooterState state) {
        client.get().uri("/api/v1/scooters/{code}", scooterCode)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(scooterCode)
                .jsonPath("$.state").isEqualTo(state.name());
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
