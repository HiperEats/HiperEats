package pt.ua.hipereats.acceptance;

import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import java.util.Locale;
import javax.sql.DataSource;
import org.springframework.test.web.servlet.client.RestTestClient;
import pt.ua.hipereats.FleetTestData;
import pt.ua.hipereats.domain.fleet.ScooterState;

/**
 * "The scooter" of a scenario, shared by the steps of the fleet features (HE-29, HE-30, HE-37).
 *
 * <p>Cucumber creates the step classes once per scenario, and the other step classes get this one
 * in their constructor, so they all see the same scooter.
 */
public class ScenarioScooterSteps {

    private final RestTestClient client;
    private final FleetTestData data;
    private String code;

    public ScenarioScooterSteps(RestTestClient client, DataSource dataSource) {
        this.client = client;
        this.data = new FleetTestData(dataSource);
    }

    /** A state as the features write it: "inactive", "in use", ... */
    @ParameterType("inactive|available|reserved|in use|maintenance|retired")
    public ScooterState scooterState(String state) {
        return ScooterState.valueOf(state.replace(' ', '_').toUpperCase(Locale.ROOT));
    }

    String code() {
        return code;
    }

    void use(String scooterCode) {
        this.code = scooterCode;
    }

    @Given("a scooter that is {scooterState}")
    public void aScooterThatIs(ScooterState state) {
        code = data.addScooter(data.createStore(10), state);
    }

    @Then("the scooter is {scooterState}")
    public void theScooterIs(ScooterState state) {
        client.get().uri("/api/v1/scooters/{code}", code)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(code)
                .jsonPath("$.state").isEqualTo(state.name());
    }

    @Then("the scooter is still {scooterState}")
    public void theScooterIsStill(ScooterState state) {
        theScooterIs(state);
    }
}
