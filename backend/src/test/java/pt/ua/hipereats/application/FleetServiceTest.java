package pt.ua.hipereats.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import pt.ua.hipereats.application.port.in.RegisterScooterCommand;
import pt.ua.hipereats.application.port.out.ScooterRepository;
import pt.ua.hipereats.application.port.out.StoreRepository;
import pt.ua.hipereats.domain.fleet.Scooter;
import pt.ua.hipereats.domain.fleet.ScooterNotFoundException;
import pt.ua.hipereats.domain.fleet.ScooterState;
import pt.ua.hipereats.domain.fleet.ScooterStateChangeRefusedException;
import pt.ua.hipereats.domain.fleet.ScooterType;
import pt.ua.hipereats.domain.fleet.Store;
import pt.ua.hipereats.domain.fleet.StoreAtCapacityException;
import pt.ua.hipereats.domain.fleet.StoreNotFoundException;

class FleetServiceTest {

    /** Stores kept in memory; locking is the persistence adapter's job (ScooterRegistrationIT). */
    private static final class InMemoryStores implements StoreRepository {
        private final Map<Long, Store> stores = new HashMap<>();

        @Override
        public Optional<Store> findAndLock(long storeId) {
            return Optional.ofNullable(stores.get(storeId));
        }
    }

    private static final class InMemoryScooters implements ScooterRepository {
        private final List<Scooter> added = new ArrayList<>();
        private final List<String> locked = new ArrayList<>();

        @Override
        public void add(Scooter scooter) {
            added.add(scooter);
        }

        @Override
        public Optional<Scooter> findByCode(String code) {
            return added.stream().filter(s -> s.code().equals(code)).findFirst();
        }

        @Override
        public Optional<Scooter> findAndLock(String code) {
            locked.add(code);
            return findByCode(code);
        }

        @Override
        public void update(Scooter scooter, ScooterState from) {
            if (stateOf(scooter.code()) != from) {
                throw new OptimisticLockingFailureException("state of " + scooter.code() + " changed");
            }
            added.replaceAll(s -> s.code().equals(scooter.code()) ? scooter : s);
        }

        void put(String code, ScooterState state) {
            added.add(new Scooter(code, ScooterType.STANDARD, state, 1L));
        }

        ScooterState stateOf(String code) {
            return findByCode(code).orElseThrow().state();
        }
    }

    private final InMemoryStores stores = new InMemoryStores();
    private final InMemoryScooters scooters = new InMemoryScooters();
    private final FleetService service = new FleetService(stores, scooters);

    @Test
    @DisplayName("BR13: a scooter registered at a store with a free place is stored inactive")
    void whenStoreHasAFreePlace_thenScooterIsStoredInactive() {
        stores.stores.put(1L, new Store(1L, 10, 9));
        Scooter expected = new Scooter("AV-10", ScooterType.STANDARD, ScooterState.INACTIVE, 1L);

        Scooter registered =
                service.register(new RegisterScooterCommand("AV-10", ScooterType.STANDARD, 1L));

        assertThat(registered).isEqualTo(expected);
        assertThat(scooters.added).containsExactly(expected);
    }

    @Test
    @DisplayName("BR13: nothing is stored when the store is at capacity")
    void whenStoreIsAtCapacity_thenNothingIsStored() {
        stores.stores.put(1L, new Store(1L, 10, 10));
        RegisterScooterCommand command =
                new RegisterScooterCommand("AV-11", ScooterType.STANDARD, 1L);

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(StoreAtCapacityException.class);
        assertThat(scooters.added).isEmpty();
    }

    @Test
    void whenStoreDoesNotExist_thenRegistrationIsRefused() {
        RegisterScooterCommand command =
                new RegisterScooterCommand("AV-01", ScooterType.STANDARD, 99L);

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOf(StoreNotFoundException.class);
        assertThat(scooters.added).isEmpty();
    }

    @Test
    void whenScooterWasRegistered_thenItIsFoundByItsCode() {
        stores.stores.put(1L, new Store(1L, 10, 0));
        service.register(new RegisterScooterCommand("AV-01", ScooterType.STANDARD, 1L));

        Optional<Scooter> found = service.findByCode("AV-01");

        assertThat(found).map(Scooter::storeId).contains(1L);
    }

    @Test
    void whenInactiveScooterIsActivated_thenItIsStoredAvailable() {
        scooters.put("AV-01", ScooterState.INACTIVE);

        Scooter activated = service.activate("AV-01");

        assertThat(activated.state()).isEqualTo(ScooterState.AVAILABLE);
        assertThat(scooters.stateOf("AV-01")).isEqualTo(ScooterState.AVAILABLE);
    }

    @Test
    void whenActivationIsRefused_thenTheStoredStateIsUnchanged() {
        scooters.put("AV-01", ScooterState.MAINTENANCE);

        assertThatThrownBy(() -> service.activate("AV-01"))
                .isInstanceOf(ScooterStateChangeRefusedException.class);
        assertThat(scooters.stateOf("AV-01")).isEqualTo(ScooterState.MAINTENANCE);
    }

    @Test
    void whenAvailableScooterIsDeactivated_thenItIsStoredInactive() {
        scooters.put("AV-01", ScooterState.AVAILABLE);

        Scooter deactivated = service.deactivate("AV-01");

        assertThat(deactivated.state()).isEqualTo(ScooterState.INACTIVE);
        assertThat(scooters.stateOf("AV-01")).isEqualTo(ScooterState.INACTIVE);
    }

    @Test
    void whenScooterIsDeactivated_thenItIsLockedWhileItChanges() {
        scooters.put("AV-01", ScooterState.AVAILABLE);

        service.deactivate("AV-01");

        assertThat(scooters.locked).containsExactly("AV-01");
    }

    @Test
    void whenScooterInUseIsDeactivated_thenItIsRefusedAndStaysInUse() {
        scooters.put("AV-01", ScooterState.IN_USE);

        assertThatThrownBy(() -> service.deactivate("AV-01"))
                .isInstanceOf(ScooterStateChangeRefusedException.class);
        assertThat(scooters.stateOf("AV-01")).isEqualTo(ScooterState.IN_USE);
    }

    @Test
    void whenScooterToActivateDoesNotExist_thenNotFound() {
        assertThatThrownBy(() -> service.activate("XX-99"))
                .isInstanceOf(ScooterNotFoundException.class);
    }

    @Test
    void whenScooterToDeactivateDoesNotExist_thenNotFound() {
        assertThatThrownBy(() -> service.deactivate("XX-99"))
                .isInstanceOf(ScooterNotFoundException.class);
    }
}
