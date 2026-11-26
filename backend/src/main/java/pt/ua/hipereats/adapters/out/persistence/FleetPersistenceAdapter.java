package pt.ua.hipereats.adapters.out.persistence;

import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import pt.ua.hipereats.application.port.out.ScooterRepository;
import pt.ua.hipereats.application.port.out.StoreRepository;
import pt.ua.hipereats.domain.fleet.Scooter;
import pt.ua.hipereats.domain.fleet.ScooterCodeAlreadyUsedException;
import pt.ua.hipereats.domain.fleet.ScooterState;
import pt.ua.hipereats.domain.fleet.Store;

/** Stores and scooters in PostgreSQL, through JPA. */
@Component
class FleetPersistenceAdapter implements StoreRepository, ScooterRepository {

    /** Unique constraint on {@code scooters.code}, named in the migration. */
    static final String UNIQUE_CODE_CONSTRAINT = "scooters_code_unique";

    private final StoreJpaRepository stores;
    private final ScooterJpaRepository scooters;

    FleetPersistenceAdapter(StoreJpaRepository stores, ScooterJpaRepository scooters) {
        this.stores = stores;
        this.scooters = scooters;
    }

    /**
     * Locks the store row first and counts its scooters in a second statement. In PostgreSQL's
     * default isolation (read committed) each statement sees what was committed before it started,
     * so the count includes a scooter that a registration which held the lock just committed.
     */
    @Override
    public Optional<Store> findAndLock(long storeId) {
        return stores.findByIdForUpdate(storeId).map(store -> new Store(
                store.getId(), store.getFleetCapacity(),
                Math.toIntExact(scooters.countByStoreId(storeId))));
    }

    @Override
    public void add(Scooter scooter) {
        try {
            scooters.saveAndFlush(ScooterEntity.from(scooter));
        } catch (DataIntegrityViolationException e) {
            if (e.getCause() instanceof ConstraintViolationException violation
                    && UNIQUE_CODE_CONSTRAINT.equalsIgnoreCase(violation.getConstraintName())) {
                throw new ScooterCodeAlreadyUsedException(scooter.code());
            }
            throw e;
        }
    }

    @Override
    public Optional<Scooter> findByCode(String code) {
        return scooters.findByCode(code).map(ScooterEntity::toScooter);
    }

    @Override
    public Optional<Scooter> findAndLock(String code) {
        return scooters.findByCodeForUpdate(code).map(ScooterEntity::toScooter);
    }

    @Override
    public void update(Scooter scooter, ScooterState from) {
        scooters.updateState(scooter.code(), scooter.state());
    }
}
