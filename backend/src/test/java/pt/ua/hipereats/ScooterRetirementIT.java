package pt.ua.hipereats;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.sql.Connection;
import java.sql.PreparedStatement;
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
import org.springframework.test.web.servlet.client.EntityExchangeResult;
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

    /** The status of a response and, for an error, the title of its ProblemDetail. */
    private record Outcome(int status, String title) {

        static Outcome of(EntityExchangeResult<String> result) {
            int status = result.getStatus().value();
            String title = status == 200 ? null : JsonPath.read(result.getResponseBody(), "$.title");
            return new Outcome(status, title);
        }
    }

    /**
     * The scooter row is locked while it changes state, so concurrent retirements run one after the
     * other: the first one retires the scooter, and every other one reads it already retired and is
     * refused by the lifecycle ("Scooter state change refused").
     *
     * <p>This is what proves the lock. Without it, all of them would read the scooter inactive, the
     * conditional update would still let only one through, and the others would fail on the update
     * with "Concurrent change" instead.
     */
    @Test
    void whenTheSameScooterIsRetiredConcurrently_thenOnlyOneRetirementIsAccepted() throws Exception {
        String code = data.addScooter(storeId, ScooterState.INACTIVE);
        // Fewer requests than the 10 connections of the default Hikari pool (see ScooterRegistrationIT).
        int attempts = 8;
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Outcome>> pending = new ArrayList<>();
        List<Outcome> outcomes = new ArrayList<>();

        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        try {
            for (int i = 0; i < attempts; i++) {
                pending.add(pool.submit(() -> {
                    start.await();
                    return Outcome.of(retire(code).expectBody(String.class).returnResult());
                }));
            }
            start.countDown();
            for (Future<Outcome> outcome : pending) {
                outcomes.add(outcome.get(30, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(outcomes).filteredOn(o -> o.status() == 200).hasSize(1);
        assertThat(outcomes).filteredOn(o -> o.status() != 200)
                .hasSize(attempts - 1)
                .allSatisfy(o -> assertThat(o).isEqualTo(
                        new Outcome(409, "Scooter state change refused")));
        assertThat(data.stateOf(code)).isEqualTo(ScooterState.RETIRED);
    }

    /**
     * Another transaction holds the scooter's row lock. The retirement waits for the lock timeout
     * (3 s), not forever, and is refused; the scooter keeps its state.
     */
    @Test
    void whenScooterIsLockedByAnotherTransaction_thenConflictAfterTheLockTimeout() throws Exception {
        String code = data.addScooter(storeId, ScooterState.INACTIVE);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try (Connection holder = dataSource.getConnection()) {
            holder.setAutoCommit(false);
            try (PreparedStatement lock =
                    holder.prepareStatement("SELECT id FROM scooters WHERE code = ? FOR UPDATE")) {
                lock.setString(1, code);
                lock.executeQuery().close();
            }
            try {
                Future<Integer> status =
                        pool.submit(() -> retire(code).returnResult().getStatus().value());

                assertThat(status.get(15, TimeUnit.SECONDS)).isEqualTo(409);
            } finally {
                holder.rollback();
            }
        } finally {
            pool.shutdownNow();
        }
        assertThat(data.stateOf(code)).isEqualTo(ScooterState.INACTIVE);
    }
}
