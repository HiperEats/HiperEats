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

/** Retiring a scooter through the REST API, on the whole application and a real PostgreSQL (HE-37). */
@IntegrationTest
class ScooterRetirementIT {

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

    private ResponseSpec retire(String code) {
        return client.patch().uri("/api/v1/scooters/{code}", code)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"state": "RETIRED"}
                        """)
                .exchange();
    }

    @Test
    void whenInactiveScooterIsRetired_thenItIsStoredRetired() {
        String code = data.addScooter(storeId, ScooterState.INACTIVE);

        retire(code).expectStatus().isOk()
                .expectBody().jsonPath("$.state").isEqualTo("RETIRED");

        assertThat(data.stateOf(code)).isEqualTo(ScooterState.RETIRED);
    }

    @Test
    void whenAvailableScooterIsRetired_thenConflictAndItStaysAvailable() {
        String code = data.addScooter(storeId, ScooterState.AVAILABLE);

        retire(code).expectStatus().isEqualTo(409)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);

        assertThat(data.stateOf(code)).isEqualTo(ScooterState.AVAILABLE);
    }

    @Test
    void whenScooterDoesNotExist_thenNotFound() {
        retire(FleetTestData.uniqueCode()).expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    /**
     * The scooter row is locked while it changes state, so two changes of the same scooter run one
     * after the other and the second one sees the first: a retired scooter never changes again.
     */
    @Test
    void whenTheSameScooterIsRetiredConcurrently_thenOnlyOneRetirementIsAccepted() throws Exception {
        String code = data.addScooter(storeId, ScooterState.INACTIVE);
        // Fewer requests than the 10 connections of the default Hikari pool (see ScooterRegistrationIT).
        int attempts = 8;
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> statuses = new ArrayList<>();
        List<Integer> results = new ArrayList<>();

        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        try {
            for (int i = 0; i < attempts; i++) {
                statuses.add(pool.submit(() -> {
                    start.await();
                    return retire(code).returnResult().getStatus().value();
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
        assertThat(data.stateOf(code)).isEqualTo(ScooterState.RETIRED);
    }
}
