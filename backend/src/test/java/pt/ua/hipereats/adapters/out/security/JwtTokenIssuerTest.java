package pt.ua.hipereats.adapters.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import pt.ua.hipereats.application.port.out.IssuedToken;
import pt.ua.hipereats.domain.account.Role;
import pt.ua.hipereats.domain.account.UserAccount;

class JwtTokenIssuerTest {

    private static final Instant NOW = Instant.parse("2026-11-26T10:00:00Z");
    private static final SecurityProperties PROPERTIES =
            new SecurityProperties("unit-test-secret-of-at-least-32-bytes!!", Duration.ofHours(8));

    private final JwtTokenIssuer issuer = new JwtTokenIssuer(
            JwtKeys.encoder(PROPERTIES), PROPERTIES, Clock.fixed(NOW, ZoneId.of("Europe/Lisbon")));

    private static Jwt decode(String token) {
        NimbusJwtDecoder decoder = JwtKeys.decoder(PROPERTIES);
        // The clock of the test is fixed in the past: check the claims, not the expiry.
        decoder.setJwtValidator(jwt -> OAuth2TokenValidatorResult.success());
        return decoder.decode(token);
    }

    @Test
    void whenTokenIsIssued_thenItNamesTheAccountAndItsRole() {
        UserAccount rita = new UserAccount(7L, "rita@example.test", "{noop}x", Role.COURIER, true);

        Jwt jwt = decode(issuer.issue(rita).value());

        assertThat(jwt.getSubject()).isEqualTo("rita@example.test");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("COURIER");
        assertThat(jwt.getClaimAsString("accountId")).isEqualTo("7");
    }

    @Test
    void whenTokenIsIssued_thenItExpiresAfterTheConfiguredValidity() {
        UserAccount rita = new UserAccount(7L, "rita@example.test", "{noop}x", Role.COURIER, true);

        IssuedToken token = issuer.issue(rita);

        assertThat(token.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(8)));
        assertThat(decode(token.value()).getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(8)));
        assertThat(decode(token.value()).getIssuedAt()).isEqualTo(NOW);
    }
}
