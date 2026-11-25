package pt.ua.hipereats.simulator;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import pt.ua.hipereats.simulator.SimulatorProperties.Delivery;
import pt.ua.hipereats.simulator.SimulatorProperties.Mqtt;
import pt.ua.hipereats.simulator.SimulatorProperties.StoreProperties;

/** A wrong configuration must stop the simulator at startup, not publish scooters named "null-01". */
class SimulatorPropertiesTest {

    private static final Mqtt MQTT = new Mqtt("tcp://localhost:1883", "hipereats-simulator", Duration.ofSeconds(5));
    private static final Delivery DELIVERY = new Delivery(0.60, 0.35, 2.0, 3.0, 1.2);
    private static final StoreProperties AVEIRO = new StoreProperties("AV", "Aveiro", 40.6405, -8.6538, 7, 3);

    private static SimulatorProperties withStores(List<StoreProperties> stores) {
        return new SimulatorProperties(MQTT, Duration.ofSeconds(5), 20, 42, 0.02, 4, 2, 20, 100, DELIVERY, stores);
    }

    @Test
    void whenConfigurationIsComplete_thenItIsAccepted() {
        assertThatNoException().isThrownBy(() -> withStores(List.of(AVEIRO)));
    }

    @Test
    void whenStoreHasNoCode_thenStartupFails() {
        // What SIMULATOR_STORES_0_STANDARDSCOOTERS=20 alone gives: the whole list is replaced.
        assertThatIllegalArgumentException().isThrownBy(
                () -> new StoreProperties(null, null, 0, 0, 20, 0));
    }

    @Test
    void whenStoreHasNegativeScooters_thenStartupFails() {
        assertThatIllegalArgumentException().isThrownBy(
                () -> new StoreProperties("AV", "Aveiro", 40.6405, -8.6538, -1, 3));
    }

    @Test
    void whenNoStores_thenStartupFails() {
        assertThatIllegalArgumentException().isThrownBy(() -> withStores(List.of()));
        assertThatIllegalArgumentException().isThrownBy(() -> withStores(null));
    }

    @Test
    void whenTwoStoresShareACode_thenStartupFails() {
        assertThatIllegalArgumentException().isThrownBy(() -> withStores(List.of(AVEIRO, AVEIRO)));
    }

    @Test
    void whenDeliveryLimitsAreInconsistent_thenStartupFails() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Delivery(0.60, 0.35, 3.0, 2.0, 1.2));
        assertThatIllegalArgumentException().isThrownBy(() -> new Delivery(0.80, 0.35, 2.0, 3.0, 1.2));
        assertThatIllegalArgumentException().isThrownBy(() -> new Delivery(0.60, 0.35, 2.0, 3.0, 0.9));
    }
}
