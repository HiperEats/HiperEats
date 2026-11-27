package pt.ua.hipereats.application;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.ua.hipereats.application.port.in.AvailableScooter;
import pt.ua.hipereats.application.port.in.AvailableScootersQuery;
import pt.ua.hipereats.application.port.out.ScooterRepository;
import pt.ua.hipereats.application.port.out.TelemetryRepository;
import pt.ua.hipereats.domain.fleet.RangeRule;
import pt.ua.hipereats.domain.fleet.Scooter;
import pt.ua.hipereats.domain.fleet.ScooterState;

/** Vehicle discovery: what a courier sees when choosing a scooter (HE-31). */
@Service
public class ScooterDiscoveryService implements AvailableScootersQuery {

    private final ScooterRepository scooters;
    private final TelemetryRepository telemetry;
    private final RangeRule rangeRule;

    public ScooterDiscoveryService(ScooterRepository scooters, TelemetryRepository telemetry,
                                   RangeRule rangeRule) {
        this.scooters = scooters;
        this.telemetry = telemetry;
        this.rangeRule = rangeRule;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailableScooter> availableFor(long storeId, BigDecimal deliveryDistanceKm) {
        return scooters.findByStoreAndState(storeId, ScooterState.AVAILABLE).stream()
                .flatMap(scooter -> withBattery(scooter).stream())
                .filter(scooter -> rangeRule.covers(scooter.type(), scooter.batteryPercent(),
                        deliveryDistanceKm))
                .sorted(Comparator.comparing(AvailableScooter::code))
                .toList();
    }

    /** The scooter with its latest reported battery, or empty if it has not reported yet. */
    private Optional<AvailableScooter> withBattery(Scooter scooter) {
        return telemetry.findLatest(scooter.code()).map(reading ->
                new AvailableScooter(scooter.code(), scooter.type(), reading.batteryPercent()));
    }
}
