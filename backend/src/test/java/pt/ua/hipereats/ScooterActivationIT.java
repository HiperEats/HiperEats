package pt.ua.hipereats;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;
import pt.ua.hipereats.domain.fleet.ScooterState;

/**
 * Activating and deactivating a scooter through the REST API, on the whole application and a real
 * PostgreSQL (HE-30).
 */
@IntegrationTest
class ScooterActivationIT {

    @Autowired
    private RestTestClient client;

    @Autowired
    private DataSource dataSource;

    private FleetTestData data;
    private long storeId;

    @BeforeEach
    void setUp() {
        data = new FleetTestData(dataSource);
        storeId = data.createStore(10);
    }

    private ResponseSpec changeState(String code, ScooterState state) {
        return client.patch().uri("/api/v1/scooters/{code}", code)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"state": "%s"}
                        """.formatted(state))
                .exchange();
    }

    @Test
    void whenInactiveScooterIsActivated_thenItIsStoredAvailable() {
        String code = data.addScooter(storeId, ScooterState.INACTIVE);

        changeState(code, ScooterState.AVAILABLE).expectStatus().isOk()
                .expectBody().jsonPath("$.state").isEqualTo("AVAILABLE");

        assertThat(data.stateOf(code)).isEqualTo(ScooterState.AVAILABLE);
    }

    @Test
    void whenScooterInMaintenanceIsActivated_thenConflictAndItStaysInMaintenance() {
        String code = data.addScooter(storeId, ScooterState.MAINTENANCE);

        changeState(code, ScooterState.AVAILABLE).expectStatus().isEqualTo(409)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);

        assertThat(data.stateOf(code)).isEqualTo(ScooterState.MAINTENANCE);
    }

    @Test
    void whenScooterInMaintenanceIsDeactivated_thenItIsStoredInactive() {
        String code = data.addScooter(storeId, ScooterState.MAINTENANCE);

        changeState(code, ScooterState.INACTIVE).expectStatus().isOk();

        assertThat(data.stateOf(code)).isEqualTo(ScooterState.INACTIVE);
    }

    @Test
    void whenScooterInUseIsDeactivated_thenConflictAndItStaysInUse() {
        String code = data.addScooter(storeId, ScooterState.IN_USE);

        changeState(code, ScooterState.INACTIVE).expectStatus().isEqualTo(409)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);

        assertThat(data.stateOf(code)).isEqualTo(ScooterState.IN_USE);
    }

    @Test
    void whenScooterDoesNotExist_thenNotFound() {
        changeState(FleetTestData.uniqueCode(), ScooterState.AVAILABLE).expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);
    }
}
