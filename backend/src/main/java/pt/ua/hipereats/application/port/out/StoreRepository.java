package pt.ua.hipereats.application.port.out;

import java.util.Optional;
import pt.ua.hipereats.domain.fleet.Store;

/** Outbound port: the stores and the size of their fleets. */
public interface StoreRepository {

    /**
     * Loads the store with its current number of scooters and locks it until the transaction ends,
     * so that registrations at the same store run one after the other (BR13 under concurrency).
     */
    Optional<Store> findAndLock(long storeId);
}
