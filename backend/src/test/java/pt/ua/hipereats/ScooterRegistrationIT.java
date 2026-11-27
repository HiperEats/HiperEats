package pt.ua.hipereats;

import static org.assertj.core.api.Assertions.assertThat;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient.ResponseSpec;
import pt.ua.hipereats.application.port.out.TokenIssuer;
import pt.ua.hipereats.application.port.out.UserAccountRepository;
import pt.ua.hipereats.domain.account.Role;
import pt.ua.hipereats.domain.account.UserAccount;

/**
 * Registering a scooter through the REST API, on the whole application and a real PostgreSQL (HE-29).
 */
@IntegrationTest
class ScooterRegistrationIT {

    @Autowired
    private RestTestClient client;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserAccountRepository accounts;

    @Autowired
    private TokenIssuer tokenIssuer;

    private FleetTestData data;

    /** "Bearer ..." of an operator, the role that registers scooters. */
    private String operator;

    @BeforeEach
    void setUp() {
        data = new FleetTestData(dataSource);
        UserAccount account = accounts.save(UserAccount.newAccount(
                "operator-" + UUID.randomUUID() + "@example.test", "{noop}unused", Role.OPERATOR));
        operator = "Bearer " + tokenIssuer.issue(account).value();
    }

    private ResponseSpec register(String code, String type, long storeId) {
        return client.post().uri("/api/v1/scooters")
                .header("Authorization", operator)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"code": "%s", "type": "%s", "storeId": %d}
                        """.formatted(code, type, storeId))
                .exchange();
    }

    private ResponseSpec register(String code, long storeId) {
        return register(code, "STANDARD", storeId);
    }

    @Test
    @Requirement("HE-29")
    void whenScooterIsRegistered_thenItIsStoredInactiveAtThatStore() {
        long storeId = data.createStore(10);
        String code = FleetTestData.uniqueCode();

        register(code, "LONG_RANGE", storeId).expectStatus().isCreated();

        client.get().uri("/api/v1/scooters/{code}", code)
                .header("Authorization", operator)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.code").isEqualTo(code)
                .jsonPath("$.type").isEqualTo("LONG_RANGE")
                .jsonPath("$.state").isEqualTo("INACTIVE")
                .jsonPath("$.storeId").isEqualTo(storeId);
        assertThat(data.countScooters(storeId)).isEqualTo(1);
    }

    @Test
    @Requirement("HE-29")
    void whenCodeIsAlreadyUsedAtAnotherStore_thenConflictAndNothingIsStored() {
        long firstStore = data.createStore(10);
        long secondStore = data.createStore(10);
        String code = FleetTestData.uniqueCode();
        register(code, firstStore).expectStatus().isCreated();

        register(code, secondStore).expectStatus().isEqualTo(409);

        assertThat(data.countScooters(secondStore)).isZero();
    }

    @Test
    @Requirement("HE-29")
    void whenStoreDoesNotExist_thenNotFoundAndNothingIsStored() {
        String code = FleetTestData.uniqueCode();

        register(code, Long.MAX_VALUE).expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);

        assertThat(data.countScootersWithCode(code)).isZero();
    }

    @Test
    @Requirement("HE-29")
    @DisplayName("BR13: of concurrent registrations for the last free place, only one is accepted")
    void whenRegistrationsRaceForTheLastFreePlace_thenOnlyOneIsAccepted() throws Exception {
        long storeId = data.createStore(10);
        data.addScooters(storeId, 9);
        // 8 requests at once, fewer than the 10 connections of the default Hikari pool, so that no
        // request waits for a connection and all of them reach the lock on the store together.
        int attempts = 8;
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> statuses = new ArrayList<>();
        List<Integer> results = new ArrayList<>();

        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        try {
            for (int i = 0; i < attempts; i++) {
                statuses.add(pool.submit(() -> {
                    start.await();
                    return register(FleetTestData.uniqueCode(), storeId)
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

        assertThat(results).containsOnly(201, 422).containsOnlyOnce(201);
        assertThat(data.countScooters(storeId)).isEqualTo(10);
    }
}
