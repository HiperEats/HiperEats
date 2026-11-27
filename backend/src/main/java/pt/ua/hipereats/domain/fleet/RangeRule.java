package pt.ua.hipereats.domain.fleet;

import java.math.BigDecimal;

/**
 * BR1: a scooter serves a delivery only if its estimated range covers the round trip plus a margin.
 *
 * <p>Estimated range = battery level x full range of the scooter type. Required range = 2 x
 * delivery distance x margin. The values come from configuration (Product Specification, 3.2).
 * Kilometres are {@code BigDecimal}, so that a boundary such as 7.2 km is compared exactly.
 *
 * @param standardFullRangeKm  range of a standard scooter at full battery (25 km)
 * @param longRangeFullRangeKm range of a long-range scooter at full battery (50 km)
 * @param margin               factor over the round trip (1.2, a 20 % margin)
 */
public record RangeRule(BigDecimal standardFullRangeKm, BigDecimal longRangeFullRangeKm,
                        BigDecimal margin) {

    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    /** The distance the scooter can still ride with this battery level. */
    public BigDecimal estimatedRangeKm(ScooterType type, int batteryPercent) {
        BigDecimal fullRange = switch (type) {
            case STANDARD -> standardFullRangeKm;
            case LONG_RANGE -> longRangeFullRangeKm;
        };
        return fullRange.multiply(BigDecimal.valueOf(batteryPercent)).movePointLeft(2);
    }

    /** The range a delivery at this distance needs: there and back, plus the margin. */
    public BigDecimal requiredRangeKm(BigDecimal deliveryDistanceKm) {
        return TWO.multiply(deliveryDistanceKm).multiply(margin);
    }

    /** True if a scooter of this type and battery level can serve a delivery at this distance. */
    public boolean covers(ScooterType type, int batteryPercent, BigDecimal deliveryDistanceKm) {
        return estimatedRangeKm(type, batteryPercent)
                .compareTo(requiredRangeKm(deliveryDistanceKm)) >= 0;
    }
}
