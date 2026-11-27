package pt.ua.hipereats.acceptance;

import static org.assertj.core.api.Assertions.assertThat;

import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.servlet.client.RestTestClient;
import pt.ua.hipereats.FleetTestData;
import pt.ua.hipereats.application.port.in.RecordTelemetryUseCase;
import pt.ua.hipereats.domain.fleet.ScooterState;
import pt.ua.hipereats.domain.fleet.ScooterType;
import pt.ua.hipereats.domain.telemetry.TelemetryReading;

/**
 * Steps of {@code HE-31_see_available_scooters.feature}.
 *
 * <p>All scenarios share one database, so the stores and scooter codes of a feature ("Aveiro",
 * "AV-01") are names local to the scenario: each one maps to a new store and a unique code. The
 * battery comes from a telemetry reading, as the simulator would report it.
 *
 * <p>Until login (HE-28) tells which store a courier works at, the courier's store is sent as a
 * parameter, and "the courier is logged in" has no effect.
 */
public class SeeAvailableScootersSteps {

    private final RestTestClient client;
    private final FleetTestData data;
    private final RecordTelemetryUseCase telemetry;
    private final Clock clock;

    private final Map<String, Long> stores = new HashMap<>();
    private final Map<String, String> codes = new HashMap<>();
    private String courierStore;
    private List<Map<String, Object>> listed;

    public SeeAvailableScootersSteps(RestTestClient client, DataSource dataSource,
                                     RecordTelemetryUseCase telemetry, Clock clock) {
        this.client = client;
        this.data = new FleetTestData(dataSource);
        this.telemetry = telemetry;
        this.clock = clock;
    }

    /** A scooter type as the features write it: "standard", "long-range" or "long range". */
    @ParameterType("standard|long-range|long range")
    public ScooterType scooterType(String type) {
        return type.equals("standard") ? ScooterType.STANDARD : ScooterType.LONG_RANGE;
    }

    private long store(String name) {
        return stores.computeIfAbsent(name, n -> data.createStore(10));
    }

    private String code(String featureCode) {
        return codes.computeIfAbsent(featureCode, c -> FleetTestData.uniqueCode());
    }

    private void putScooter(String store, ScooterType type, String featureCode, int battery,
                            ScooterState state) {
        String code = code(featureCode);
        data.addScooter(store(store), code, type, state);
        telemetry.record(new TelemetryReading(code, OffsetDateTime.now(clock), battery,
                40.6405, -8.6538, true, null));
    }

    @Given("a courier who works at store {string}")
    public void aCourierWhoWorksAtStore(String store) {
        store(store);
        courierStore = store;
    }

    @Given("the courier is logged in")
    public void theCourierIsLoggedIn() {
        // Intentionally empty until HE-28: see the class comment.
    }

    @Given("store {string} has an available {scooterType} scooter {string} with battery {int} %")
    public void storeHasAnAvailableScooter(String store, ScooterType type, String code, int battery) {
        putScooter(store, type, code, battery, ScooterState.AVAILABLE);
    }

    @Given("store {string} has a {scooterType} scooter {string} with battery {int} % that is {scooterState}")
    public void storeHasAScooterThatIs(String store, ScooterType type, String code, int battery,
                                       ScooterState state) {
        putScooter(store, type, code, battery, state);
    }

    @When("the courier asks for the available scooters for a delivery {int} km away")
    public void theCourierAsksForTheAvailableScooters(int distanceKm) {
        listed = client.get()
                .uri("/api/v1/scooters?storeId={storeId}&deliveryDistanceKm={km}",
                        store(courierStore), distanceKm)
                .exchange()
                .expectStatus().isOk()
                .returnResult(new ParameterizedTypeReference<List<Map<String, Object>>>() { })
                .getResponseBody();
    }

    private List<Object> listedCodes() {
        return listed.stream().map(scooter -> scooter.get("code")).toList();
    }

    @Then("the list contains {string}")
    public void theListContains(String code) {
        assertThat(listedCodes()).contains(code(code));
    }

    @Then("the list does not contain {string}")
    public void theListDoesNotContain(String code) {
        assertThat(listedCodes()).doesNotContain(code(code));
    }

    @Then("scooter {string} is shown as {scooterType} with battery {int} %")
    public void scooterIsShownAs(String code, ScooterType type, int battery) {
        assertThat(listed).filteredOn(scooter -> code(code).equals(scooter.get("code")))
                .singleElement()
                .satisfies(scooter -> {
                    assertThat(scooter.get("type")).isEqualTo(type.name());
                    assertThat(scooter.get("batteryPercent")).isEqualTo(battery);
                });
    }
}
