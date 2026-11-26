package pt.ua.hipereats.acceptance;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;
import pt.ua.hipereats.domain.fleet.ScooterState;

/**
 * The operator changes the state of the scenario's scooter: steps of
 * {@code HE-37_retire_scooter.feature}, written so that HE-30 (activate and deactivate) can add its
 * own changes next to them.
 */
public class ChangeScooterStateSteps {

    private final RestTestClient client;
    private final ScenarioScooterSteps scooter;
    private ResponseSpec change;

    public ChangeScooterStateSteps(RestTestClient client, ScenarioScooterSteps scooter) {
        this.client = client;
        this.scooter = scooter;
    }

    private void changeStateTo(ScooterState state) {
        change = client.patch().uri("/api/v1/scooters/{code}", scooter.code())
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"state": "%s"}
                        """.formatted(state))
                .exchange();
    }

    @When("the operator retires the scooter")
    public void theOperatorRetiresTheScooter() {
        changeStateTo(ScooterState.RETIRED);
    }

    @Then("the change is refused")
    public void theChangeIsRefused() {
        change.expectStatus().isEqualTo(409)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);
    }
}
