package pt.ua.hipereats;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.client.RestTestClient;
import pt.ua.hipereats.adapters.out.security.JwtKeys;
import pt.ua.hipereats.adapters.out.security.SecurityProperties;
import pt.ua.hipereats.application.port.out.UserAccountRepository;
import pt.ua.hipereats.domain.account.Role;
import pt.ua.hipereats.domain.account.UserAccount;

/** What the API does with requests that carry no token, a forged token or an expired one. */
@IntegrationTest
class AuthenticationIT {

    @Autowired
    private RestTestClient client;

    @Autowired
    private JwtEncoder encoder;

    @Autowired
    private UserAccountRepository accounts;

    @Autowired
    private JdbcClient jdbc;

    private UserAccount sofia;

    /** The tokens below are for Sofia, an active admin. */
    @BeforeEach
    void createSofia() {
        jdbc.sql("DELETE FROM user_accounts").update();
        sofia = accounts.save(UserAccount.newAccount("sofia@example.test", "{noop}x", Role.ADMIN));
    }

    private String token(JwtEncoder signer, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("sofia@example.test").claim("role", "ADMIN")
                .issuedAt(expiresAt.minus(Duration.ofHours(8))).expiresAt(expiresAt).build();
        return signer.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    @Test
    void whenNoTokenIsSent_thenTheApiAnswers401WithAProblemDetail() {
        client.get().uri("/api/v1/users").exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody().jsonPath("$.status").isEqualTo(401);
    }

    @Test
    void whenTokenIsSignedWithAnotherKey_thenItIsRefused() {
        NimbusJwtEncoder forger = JwtKeys.encoder(
                new SecurityProperties("a-different-secret-of-at-least-32-bytes", Duration.ofHours(8)));

        client.get().uri("/api/v1/users")
                .header("Authorization", "Bearer " + token(forger, Instant.now().plusSeconds(600)))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void whenTokenHasExpired_thenItIsRefused() {
        client.get().uri("/api/v1/users")
                .header("Authorization", "Bearer " + token(encoder, Instant.now().minusSeconds(600)))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void whenTokenIsValid_thenTheRequestGoesThrough() {
        client.get().uri("/api/v1/users")
                .header("Authorization", "Bearer " + token(encoder, Instant.now().plusSeconds(600)))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void whenTheAccountWasDeactivatedAfterLoggingIn_thenItsTokenIsRefused() {
        String token = token(encoder, Instant.now().plusSeconds(600));
        accounts.save(sofia.withActive(false));

        client.get().uri("/api/v1/users")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void whenPublicEndpointsAreCalledWithoutToken_thenTheyAnswer() {
        client.get().uri("/api/status").exchange().expectStatus().isOk();
        client.get().uri("/actuator/health").exchange().expectStatus().isOk();
    }
}
