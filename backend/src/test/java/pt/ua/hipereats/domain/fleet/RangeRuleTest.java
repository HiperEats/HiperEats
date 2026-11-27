package pt.ua.hipereats.domain.fleet;

import static org.assertj.core.api.Assertions.assertThat;

import app.getxray.xray.junit.customjunitxml.annotations.Requirement;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** BR1 with the values of the Product Specification (3.2): 25 km, 50 km and a 20 % margin. */
class RangeRuleTest {

    private final RangeRule rule =
            new RangeRule(new BigDecimal("25"), new BigDecimal("50"), new BigDecimal("1.2"));

    private static BigDecimal km(String value) {
        return new BigDecimal(value);
    }

    @Test
    @Requirement("HE-31")
    @DisplayName("BR1: a delivery at 3 km needs a range of 7.2 km")
    void whenDeliveryIsAt3Km_thenRequiredRangeIs7Point2Km() {
        assertThat(rule.requiredRangeKm(km("3"))).isEqualByComparingTo("7.2");
    }

    @Test
    @Requirement("HE-31")
    @DisplayName("BR1: a long-range scooter at 15 % has an estimated range of 7.5 km")
    void whenLongRangeScooterIsAt15Percent_thenEstimatedRangeIs7Point5Km() {
        assertThat(rule.estimatedRangeKm(ScooterType.LONG_RANGE, 15)).isEqualByComparingTo("7.5");
    }

    @Test
    @Requirement("HE-31")
    @DisplayName("BR1: a standard scooter at full battery has an estimated range of 25 km")
    void whenStandardScooterIsFull_thenEstimatedRangeIs25Km() {
        assertThat(rule.estimatedRangeKm(ScooterType.STANDARD, 100)).isEqualByComparingTo("25");
    }

    @Test
    @Requirement("HE-31")
    @DisplayName("BR1: a long-range scooter at 15 % covers a delivery at 3 km (7.5 km of 7.2 km)")
    void whenLongRangeAt15PercentAndDeliveryAt3Km_thenCovered() {
        assertThat(rule.covers(ScooterType.LONG_RANGE, 15, km("3"))).isTrue();
    }

    @Test
    @Requirement("HE-31")
    @DisplayName("BR1: a long-range scooter at 14 % does not cover a delivery at 3 km (7.0 km of 7.2 km)")
    void whenLongRangeAt14PercentAndDeliveryAt3Km_thenNotCovered() {
        assertThat(rule.covers(ScooterType.LONG_RANGE, 14, km("3"))).isFalse();
    }

    @Test
    @Requirement("HE-31")
    @DisplayName("BR1: an estimated range equal to the required range is accepted")
    void whenEstimatedRangeEqualsRequiredRange_thenCovered() {
        // Standard at 24 % gives 6.0 km; a delivery at 2.5 km needs 2 x 2.5 x 1.2 = 6.0 km.
        assertThat(rule.covers(ScooterType.STANDARD, 24, km("2.5"))).isTrue();
    }

    @Test
    @Requirement("HE-31")
    @DisplayName("BR1: an estimated range just below the required range is refused")
    void whenEstimatedRangeIsJustBelowRequiredRange_thenNotCovered() {
        // Standard at 23 % gives 5.75 km, below the 6.0 km a delivery at 2.5 km needs.
        assertThat(rule.covers(ScooterType.STANDARD, 23, km("2.5"))).isFalse();
    }

    @Test
    @Requirement("HE-31")
    @DisplayName("BR1: the scooter type decides the range, so the same battery can serve one and not the other")
    void whenSameBatteryAndDistance_thenLongRangeCoversAndStandardDoesNot() {
        assertThat(rule.covers(ScooterType.LONG_RANGE, 15, km("3"))).isTrue();
        assertThat(rule.covers(ScooterType.STANDARD, 15, km("3"))).isFalse();
    }
}
