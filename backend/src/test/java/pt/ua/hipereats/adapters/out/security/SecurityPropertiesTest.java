package pt.ua.hipereats.adapters.out.security;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/** A missing or weak signing secret must stop the backend at startup. */
class SecurityPropertiesTest {

    @Test
    void whenSecretHas32Bytes_thenItIsAccepted() {
        assertThatNoException().isThrownBy(
                () -> new SecurityProperties("a".repeat(32), Duration.ofHours(8)));
    }

    @Test
    void whenSecretIsMissingOrShorterThan32Bytes_thenStartupFails() {
        assertThatIllegalArgumentException().isThrownBy(() -> new SecurityProperties(null, Duration.ofHours(8)));
        assertThatIllegalArgumentException().isThrownBy(() -> new SecurityProperties("", Duration.ofHours(8)));
        assertThatIllegalArgumentException().isThrownBy(
                () -> new SecurityProperties("a".repeat(31), Duration.ofHours(8)));
    }

    @Test
    void whenTokenValidityIsNotPositive_thenStartupFails() {
        assertThatIllegalArgumentException().isThrownBy(
                () -> new SecurityProperties("a".repeat(32), Duration.ZERO));
    }
}
