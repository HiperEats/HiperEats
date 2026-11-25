package pt.ua.hipereats.adapters.out.telemetry;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import pt.ua.hipereats.application.port.in.ScooterTelemetryQuery;
import pt.ua.hipereats.domain.telemetry.TelemetryReading;

/**
 * Runs the backend against a real Mosquitto broker (Testcontainers) and publishes telemetry as the
 * simulator does, then checks the scooter state through the query port.
 */
@Testcontainers
@SpringBootTest
class MqttTelemetryIT {

    private static final long TIMEOUT_MS = 10_000;

    @Container
    static final GenericContainer<?> MOSQUITTO = new GenericContainer<>("eclipse-mosquitto:2")
            .withExposedPorts(1883)
            .withCopyFileToContainer(MountableFile.forClasspathResource("mosquitto.conf"),
                    "/mosquitto/config/mosquitto.conf");

    @DynamicPropertySource
    static void brokerUrl(DynamicPropertyRegistry registry) {
        registry.add("hipereats.telemetry.broker-url", MqttTelemetryIT::brokerUrl);
    }

    private static String brokerUrl() {
        return "tcp://" + MOSQUITTO.getHost() + ":" + MOSQUITTO.getMappedPort(1883);
    }

    @Autowired
    private ScooterTelemetryQuery telemetry;

    @Autowired
    private MqttTelemetrySubscriber subscriber;

    /** A test client that plays the simulator. */
    private MqttClient simulator;

    @BeforeEach
    void connectSimulator() throws Exception {
        simulator = new MqttClient(brokerUrl(), "test-simulator-" + UUID.randomUUID(), new MemoryPersistence());
        simulator.connect();
        // The subscriber connects in the background: wait for it before publishing.
        waitUntil(subscriber::isConnected);
    }

    @AfterEach
    void disconnectSimulator() throws MqttException {
        simulator.disconnect();
        simulator.close();
    }

    private void publish(String scooterCode, String payload) throws MqttException {
        simulator.publish("hipereats/scooters/" + scooterCode + "/telemetry",
                new MqttMessage(payload.getBytes(StandardCharsets.UTF_8)));
    }

    private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TIMEOUT_MS;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
        }
        assertThat(condition.getAsBoolean()).as("condition met within %d ms", TIMEOUT_MS).isTrue();
    }

    @Test
    void whenSimulatorPublishesTelemetry_thenScooterStateIsUpdated() throws Exception {
        publish("AV-01", """
                {"scooterCode":"AV-01","timestamp":"2026-11-25T19:40:05Z","batteryPercent":76,
                 "latitude":40.6405,"longitude":-8.6538,"locked":false,"fault":null}
                """);

        waitUntil(() -> telemetry.latestFor("AV-01").isPresent());
        TelemetryReading reading = telemetry.latestFor("AV-01").orElseThrow();
        assertThat(reading.batteryPercent()).isEqualTo(76);
        assertThat(reading.locked()).isFalse();
        assertThat(reading.latitude()).isEqualTo(40.6405);
    }

    @Test
    void whenInvalidMessageArrives_thenItIsDroppedAndTheStreamGoesOn() throws Exception {
        publish("AV-02", "this is not json");
        publish("AV-02", """
                {"scooterCode":"AV-02","timestamp":"2026-11-25T19:40:05Z","batteryPercent":150,
                 "latitude":40.6405,"longitude":-8.6538,"locked":true,"fault":null}
                """);
        publish("AV-02", """
                {"scooterCode":"AV-02","timestamp":"2026-11-25T19:40:10Z","batteryPercent":42,
                 "latitude":40.6405,"longitude":-8.6538,"locked":true,"fault":null}
                """);

        waitUntil(() -> telemetry.latestFor("AV-02").isPresent());
        Optional<TelemetryReading> reading = telemetry.latestFor("AV-02");
        assertThat(reading).get().extracting(TelemetryReading::batteryPercent).isEqualTo(42);
    }
}
