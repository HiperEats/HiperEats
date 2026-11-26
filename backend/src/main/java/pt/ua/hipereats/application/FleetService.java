package pt.ua.hipereats.application;

import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.ua.hipereats.application.port.in.RegisterScooterCommand;
import pt.ua.hipereats.application.port.in.RegisterScooterUseCase;
import pt.ua.hipereats.application.port.in.RetireScooterUseCase;
import pt.ua.hipereats.application.port.in.ScooterQuery;
import pt.ua.hipereats.application.port.out.ScooterRepository;
import pt.ua.hipereats.application.port.out.StoreRepository;
import pt.ua.hipereats.domain.fleet.Scooter;
import pt.ua.hipereats.domain.fleet.Store;
import pt.ua.hipereats.domain.fleet.StoreNotFoundException;

/** Fleet management use cases. */
@Service
public class FleetService implements RegisterScooterUseCase, RetireScooterUseCase, ScooterQuery {

    private final StoreRepository stores;
    private final ScooterRepository scooters;

    public FleetService(StoreRepository stores, ScooterRepository scooters) {
        this.stores = stores;
        this.scooters = scooters;
    }

    /**
     * The store stays locked until the transaction ends, so two registrations for the last free
     * place of a store run one after the other and the second one sees the first (BR13).
     */
    @Override
    @Transactional
    public Scooter register(RegisterScooterCommand command) {
        Store store = stores.findAndLock(command.storeId())
                .orElseThrow(() -> new StoreNotFoundException(command.storeId()));
        Scooter scooter = store.registerScooter(command.code(), command.type());
        scooters.add(scooter);
        return scooter;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Scooter> findByCode(String code) {
        return scooters.findByCode(code);
    }

    @Override
    @Transactional
    public Scooter retire(String code) {
        throw new UnsupportedOperationException("HE-37: not implemented yet");
    }
}
