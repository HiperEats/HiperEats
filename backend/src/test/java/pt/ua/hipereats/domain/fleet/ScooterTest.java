package pt.ua.hipereats.domain.fleet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

/** The operator activates and deactivates a scooter (HE-30; Product Specification, 3.3). */
class ScooterTest {

    private static Scooter scooterIn(ScooterState state) {
        return new Scooter("AV-01", ScooterType.LONG_RANGE, state, 7L);
    }

    @Test
    void whenInactiveScooterIsActivated_thenItIsAvailable() {
        Scooter activated = scooterIn(ScooterState.INACTIVE).activate();

        assertThat(activated.state()).isEqualTo(ScooterState.AVAILABLE);
    }

    @Test
    void whenScooterIsActivated_thenItKeepsItsCodeTypeAndStore() {
        Scooter activated = scooterIn(ScooterState.INACTIVE).activate();

        assertThat(activated)
                .isEqualTo(new Scooter("AV-01", ScooterType.LONG_RANGE, ScooterState.AVAILABLE, 7L));
    }

    @ParameterizedTest
    @EnumSource(value = ScooterState.class, mode = Mode.EXCLUDE, names = "INACTIVE")
    void whenScooterIsNotInactive_thenActivationIsRefused(ScooterState state) {
        Scooter scooter = scooterIn(state);

        assertThatThrownBy(scooter::activate)
                .isInstanceOf(ScooterStateChangeRefusedException.class)
                .hasMessage("Scooter AV-01 cannot go from " + state + " to AVAILABLE");
    }

    @ParameterizedTest
    @EnumSource(value = ScooterState.class, names = {"AVAILABLE", "MAINTENANCE"})
    void whenScooterIsAvailableOrInMaintenance_thenDeactivationMakesItInactive(
            ScooterState state) {
        Scooter deactivated = scooterIn(state).deactivate();

        assertThat(deactivated)
                .isEqualTo(new Scooter("AV-01", ScooterType.LONG_RANGE, ScooterState.INACTIVE, 7L));
    }

    @Test
    void whenScooterIsInUse_thenDeactivationIsRefused() {
        Scooter scooter = scooterIn(ScooterState.IN_USE);

        assertThatThrownBy(scooter::deactivate)
                .isInstanceOf(ScooterStateChangeRefusedException.class)
                .hasMessage("Scooter AV-01 cannot go from IN_USE to INACTIVE");
    }

    /** Only available and maintenance go to inactive (3.3); a reserved scooter is promised to a courier. */
    @ParameterizedTest
    @EnumSource(value = ScooterState.class, names = {"INACTIVE", "RESERVED", "RETIRED"})
    void whenScooterIsInactiveReservedOrRetired_thenDeactivationIsRefused(ScooterState state) {
        Scooter scooter = scooterIn(state);

        assertThatThrownBy(scooter::deactivate)
                .isInstanceOf(ScooterStateChangeRefusedException.class)
                .hasMessage("Scooter AV-01 cannot go from " + state + " to INACTIVE");
    }
}
