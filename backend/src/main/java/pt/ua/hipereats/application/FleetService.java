package pt.ua.hipereats.application;

import java.util.Optional;
import pt.ua.hipereats.application.port.in.RegisterScooterCommand;
import pt.ua.hipereats.application.port.in.RegisterScooterUseCase;
import pt.ua.hipereats.application.port.in.ScooterQuery;
import pt.ua.hipereats.application.port.out.ScooterRepository;
import pt.ua.hipereats.application.port.out.StoreRepository;
import pt.ua.hipereats.domain.fleet.Scooter;

/** Fleet management use cases. */
public class FleetService implements RegisterScooterUseCase, ScooterQuery {

    private final StoreRepository stores;
    private final ScooterRepository scooters;

    public FleetService(StoreRepository stores, ScooterRepository scooters) {
        this.stores = stores;
        this.scooters = scooters;
    }

    @Override
    public Scooter register(RegisterScooterCommand command) {
        throw new UnsupportedOperationException("HE-29: not implemented yet");
    }

    @Override
    public Optional<Scooter> findByCode(String code) {
        throw new UnsupportedOperationException("HE-29: not implemented yet");
    }
}
