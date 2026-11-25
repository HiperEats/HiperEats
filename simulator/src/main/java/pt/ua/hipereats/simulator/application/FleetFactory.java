package pt.ua.hipereats.simulator.application;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import pt.ua.hipereats.simulator.domain.ScooterType;
import pt.ua.hipereats.simulator.domain.SimulatedScooter;
import pt.ua.hipereats.simulator.domain.Store;

/**
 * Builds the fleet of every store: first the standard scooters, then the long-range ones,
 * coded {@code <store code>-01}, {@code -02}, ... (store "AV" with 7 + 3 gives AV-01 to AV-10,
 * AV-08 to AV-10 being long range).
 */
public class FleetFactory {

    private final RandomGenerator random;
    private final double standardBatteryUsePerKm;
    private final double longRangeBatteryUsePerKm;
    private final double initialBatteryMin;
    private final double initialBatteryMax;

    public FleetFactory(RandomGenerator random, double standardBatteryUsePerKm, double longRangeBatteryUsePerKm,
                        double initialBatteryMin, double initialBatteryMax) {
        this.random = random;
        this.standardBatteryUsePerKm = standardBatteryUsePerKm;
        this.longRangeBatteryUsePerKm = longRangeBatteryUsePerKm;
        this.initialBatteryMin = initialBatteryMin;
        this.initialBatteryMax = initialBatteryMax;
    }

    public List<SimulatedScooter> create(List<Store> stores) {
        List<SimulatedScooter> fleet = new ArrayList<>();
        for (Store store : stores) {
            int number = 1;
            for (int i = 0; i < store.standardScooters(); i++) {
                fleet.add(scooter(store, number++, ScooterType.STANDARD, standardBatteryUsePerKm));
            }
            for (int i = 0; i < store.longRangeScooters(); i++) {
                fleet.add(scooter(store, number++, ScooterType.LONG_RANGE, longRangeBatteryUsePerKm));
            }
        }
        return fleet;
    }

    private SimulatedScooter scooter(Store store, int number, ScooterType type, double batteryUsePerKm) {
        String code = "%s-%02d".formatted(store.code(), number);
        double battery = initialBatteryMin + random.nextDouble() * (initialBatteryMax - initialBatteryMin);
        return new SimulatedScooter(code, type, store, battery, batteryUsePerKm);
    }
}
