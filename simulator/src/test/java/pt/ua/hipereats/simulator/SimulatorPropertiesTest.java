package pt.ua.hipereats.simulator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
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
                () -> new StoreProperties(null, null, 0.0, 0.0, 20, 0));
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

    /** Only the properties, read from application.yml and the given values, as at startup. */
    @EnableConfigurationProperties(SimulatorProperties.class)
    static class PropertiesOnly {
    }

    private static ApplicationContextRunner withStoreFields(List<String> fields) {
        return new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(PropertiesOnly.class)
                .withPropertyValues(fields.stream().map(f -> "simulator.stores[0]." + f).toArray(String[]::new));
    }

    private static final List<String> STORE_FIELDS = List.of("code=AV", "name=Aveiro", "latitude=40.6405",
            "longitude=-8.6538", "standard-scooters=7", "long-range-scooters=3");

    @Test
    void whenStoreHasEveryField_thenApplicationStarts() {
        withStoreFields(STORE_FIELDS).run(context -> assertThat(context).hasNotFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"code", "name", "latitude", "longitude", "standard-scooters", "long-range-scooters"})
    void whenAFieldOfAStoreIsMissing_thenStartupFails(String missing) {
        List<String> fields = STORE_FIELDS.stream().filter(f -> !f.startsWith(missing + "=")).toList();

        withStoreFields(fields).run(context -> assertThat(context).hasFailed());
    }

    @ParameterizedTest
    @CsvSource({"PT5S, 4000", "PT4S, 4000", "PT3.999S, 2000", "PT1S, 1000", "PT0.2S, 1000", "PT2M, 64000",
            "PT24H, 128000"})
    void whenReconnectIntervalIsGiven_thenPahoNeverWaitsLongerThanIt(Duration interval, int expectedMillis) {
        // Paho starts at 1 s and doubles while it is below the maximum, so only 1, 2, 4, ... s are exact.
        Mqtt mqtt = new Mqtt("tcp://localhost:1883", "test", interval);

        assertThat(mqtt.maxReconnectDelayMillis()).isEqualTo(expectedMillis);
    }
}
