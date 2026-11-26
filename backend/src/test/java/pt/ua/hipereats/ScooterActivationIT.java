package pt.ua.hipereats;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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

    /**
     * Each request reads the scooter's state before it changes it. Without the lock on the scooter
     * row, every concurrent deactivation would read AVAILABLE and be accepted.
     */
    @Test
    void whenDeactivationsRaceForTheSameScooter_thenOnlyOneIsAccepted() throws Exception {
        String code = data.addScooter(storeId, ScooterState.AVAILABLE);
        // 8 requests at once, fewer than the 10 connections of the default Hikari pool, so that no
        // request waits for a connection and all of them reach the lock on the scooter together.
        int attempts = 8;
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> statuses = new ArrayList<>();
        List<Integer> results = new ArrayList<>();

        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        try {
            for (int i = 0; i < attempts; i++) {
                statuses.add(pool.submit(() -> {
                    start.await();
                    return changeState(code, ScooterState.INACTIVE)
                            .returnResult().getStatus().value();
                }));
            }
            start.countDown();
            for (Future<Integer> status : statuses) {
                results.add(status.get(30, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(results).containsOnly(200, 409).containsOnlyOnce(200);
        assertThat(data.stateOf(code)).isEqualTo(ScooterState.INACTIVE);
    }
}
