package pt.ua.hipereats.domain.fleet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Retiring a scooter (HE-37; Product Specification, 3.3: inactive to retired, final). */
class ScooterRetirementTest {

    private static Scooter scooterIn(ScooterState state) {
        return new Scooter("AV-01", ScooterType.STANDARD, state, 1L);
    }

    @Test
    void whenInactiveScooterIsRetired_thenItIsRetired() {
        Scooter retired = scooterIn(ScooterState.INACTIVE).retire();

        assertThat(retired)
                .isEqualTo(new Scooter("AV-01", ScooterType.STANDARD, ScooterState.RETIRED, 1L));
    }

    /** Includes RETIRED: a retired scooter is not retired again, its state never changes. */
    @ParameterizedTest
    @EnumSource(value = ScooterState.class, names = "INACTIVE", mode = EnumSource.Mode.EXCLUDE)
    void whenScooterIsNotInactive_thenRetirementIsRefused(ScooterState state) {
        Scooter scooter = scooterIn(state);

        assertThatThrownBy(scooter::retire)
                .isInstanceOf(ScooterStateChangeRefusedException.class)
                .hasMessage("Scooter AV-01 cannot go from " + state + " to RETIRED");
    }
}
