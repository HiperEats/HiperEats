package pt.ua.hipereats.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pt.ua.hipereats.FleetTestData;
import pt.ua.hipereats.IntegrationTest;
import pt.ua.hipereats.application.port.out.ScooterRepository;
import pt.ua.hipereats.domain.fleet.Scooter;
import pt.ua.hipereats.domain.fleet.ScooterState;
import pt.ua.hipereats.domain.fleet.ScooterType;

/**
 * The new state of a scooter is written only over the state the change started from, so a change
 * never overwrites another one it did not see.
 */
@IntegrationTest
class ScooterStateUpdateIT {

    @Autowired
    private ScooterRepository scooters;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private FleetTestData data;
    private TransactionTemplate transaction;
    private long storeId;

    @BeforeEach
    void setUp() {
        data = new FleetTestData(dataSource);
        transaction = new TransactionTemplate(transactionManager);
        storeId = data.createStore(10);
    }

    private void retireFrom(String code, ScooterState from) {
        Scooter retired = new Scooter(code, ScooterType.STANDARD, ScooterState.RETIRED, storeId);
        transaction.executeWithoutResult(status -> scooters.update(retired, from));
    }

    @Test
    void whenStoredStateIsTheOneTheChangeStartedFrom_thenTheNewStateIsStored() {
        String code = data.addScooter(storeId, ScooterState.INACTIVE);

        retireFrom(code, ScooterState.INACTIVE);

        assertThat(data.stateOf(code)).isEqualTo(ScooterState.RETIRED);
    }

    @Test
    void whenStoredStateChangedMeanwhile_thenTheUpdateIsRefusedAndTheStateStays() {
        String code = data.addScooter(storeId, ScooterState.AVAILABLE);

        assertThatThrownBy(() -> retireFrom(code, ScooterState.INACTIVE))
                .isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(data.stateOf(code)).isEqualTo(ScooterState.AVAILABLE);
    }

    @Test
    void whenScooterDoesNotExist_thenTheUpdateIsRefused() {
        String code = FleetTestData.uniqueCode();

        assertThatThrownBy(() -> retireFrom(code, ScooterState.INACTIVE))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }
}
