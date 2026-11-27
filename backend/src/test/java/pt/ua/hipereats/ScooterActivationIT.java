package pt.ua.hipereats;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
import pt.ua.hipereats.application.port.out.TokenIssuer;
import pt.ua.hipereats.application.port.out.UserAccountRepository;
import pt.ua.hipereats.domain.account.Role;
import pt.ua.hipereats.domain.account.UserAccount;
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

    @Autowired
    private UserAccountRepository accounts;

    @Autowired
    private TokenIssuer tokenIssuer;

    private FleetTestData data;
    private long storeId;

    /** "Bearer ..." of an operator, the role that activates and deactivates scooters. */
    private String operator;

    @BeforeEach
    void setUp() {
        data = new FleetTestData(dataSource);
        storeId = data.createStore(10);
        UserAccount account = accounts.save(UserAccount.newAccount(
                "operator-" + UUID.randomUUID() + "@example.test", "{noop}unused", Role.OPERATOR));
        operator = "Bearer " + tokenIssuer.issue(account).value();
    }

    private ResponseSpec changeState(String code, ScooterState state) {
        return client.patch().uri("/api/v1/scooters/{code}", code)
                .header("Authorization", operator)
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
     * The lock on the scooter row makes the concurrent deactivations wait for one another, so after
     * the first one the others read INACTIVE and the scooter lifecycle refuses them ("Scooter state
     * change refused"). Without the lock, all of them would read AVAILABLE and pass the lifecycle;
     * the conditional update would still refuse all but one, but as "Concurrent change". The titles
     * tell the two apart, so this test fails if the lock is removed.
     */
    @Test
    void whenDeactivationsRaceForTheSameScooter_thenOneIsAcceptedAndTheOthersSeeItInactive()
            throws Exception {
        String code = data.addScooter(storeId, ScooterState.AVAILABLE);
        // 8 requests at once, fewer than the 10 connections of the default Hikari pool, so that no
        // request waits for a connection and all of them reach the lock on the scooter together.
        int attempts = 8;
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> outcomes = new ArrayList<>();
        List<String> results = new ArrayList<>();

        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        try {
            for (int i = 0; i < attempts; i++) {
                outcomes.add(pool.submit(() -> {
                    start.await();
                    return outcome(changeState(code, ScooterState.INACTIVE)
                            .returnResult(String.class));
                }));
            }
            start.countDown();
            for (Future<String> outcome : outcomes) {
                results.add(outcome.get(30, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(results).containsOnlyOnce("200")
                .containsOnly("200", "409 Scooter state change refused");
        assertThat(data.stateOf(code)).isEqualTo(ScooterState.INACTIVE);
    }

    /** "200", or the status and the ProblemDetail title of a refusal: "409 Concurrent change". */
    private static String outcome(EntityExchangeResult<String> result) {
        int status = result.getStatus().value();
        if (status == 200) {
            return "200";
        }
        return status + " " + JsonPath.read(result.getResponseBody(), "$.title");
    }
}
