package pt.ua.hipereats.application;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.ua.hipereats.application.port.in.AvailableScooter;
import pt.ua.hipereats.application.port.in.AvailableScootersQuery;
import pt.ua.hipereats.application.port.out.ScooterRepository;
import pt.ua.hipereats.application.port.out.TelemetryRepository;
import pt.ua.hipereats.domain.fleet.RangeRule;

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
        throw new UnsupportedOperationException("HE-31: not implemented yet");
    }
}
