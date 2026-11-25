package pt.ua.hipereats.simulator.adapters.mqtt;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Runs the whole simulator against a real Mosquitto broker (Testcontainers) and checks, as the
 * backend would see it, that telemetry arrives and that unlock commands are acknowledged.
 */
@Testcontainers
@SpringBootTest(properties = "simulator.telemetry-interval=200ms")
class MqttSimulatorIT {

    private static final long TIMEOUT_SECONDS = 10;

    @Container
    static final GenericContainer<?> MOSQUITTO = new GenericContainer<>("eclipse-mosquitto:2")
            .withExposedPorts(1883)
            .withCopyFileToContainer(MountableFile.forClasspathResource("mosquitto.conf"),
                    "/mosquitto/config/mosquitto.conf");

    @DynamicPropertySource
    static void brokerUrl(DynamicPropertyRegistry registry) {
        registry.add("simulator.mqtt.broker-url", MqttSimulatorIT::brokerUrl);
    }

    private static String brokerUrl() {
        return "tcp://" + MOSQUITTO.getHost() + ":" + MOSQUITTO.getMappedPort(1883);
    }

    private final JsonMapper json = JsonMapper.builder().build();
    private MqttClient platform;

    /** A test client that plays the platform's side of the contract. */
    @BeforeEach
    void connectPlatform() throws MqttException {
        platform = new MqttClient(brokerUrl(), "test-platform-" + UUID.randomUUID(), new MemoryPersistence());
        platform.connect();
    }

    @AfterEach
    void disconnectPlatform() throws MqttException {
        platform.disconnect();
        platform.close();
    }

    private BlockingQueue<JsonNode> subscribe(String topic) throws MqttException {
        BlockingQueue<JsonNode> received = new LinkedBlockingQueue<>();
        platform.subscribe(topic, 1, (t, message) -> received.add(json.readTree(message.getPayload())));
        return received;
    }

    @Test
    void whenRunning_thenEveryScooterPublishesTelemetry() throws Exception {
        BlockingQueue<JsonNode> telemetry = subscribe("hipereats/scooters/+/telemetry");

        Set<String> codes = new HashSet<>();
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS);
        while (codes.size() < 30 && System.currentTimeMillis() < deadline) {
            JsonNode reading = telemetry.poll(1, TimeUnit.SECONDS);
            if (reading != null) {
                assertThat(reading.has("timestamp")).isTrue();
                assertThat(reading.get("batteryPercent").asInt()).isBetween(0, 100);
                codes.add(reading.get("scooterCode").asString());
            }
        }

        assertThat(codes).hasSize(30).contains("AV-01", "IL-10", "AG-05");
    }

    @Test
    @DisplayName("BR12: the simulator acknowledges an unlock and then reports the scooter unlocked")
    void whenUnlockCommandSent_thenAckedAndTelemetryShowsUnlocked() throws Exception {
        BlockingQueue<JsonNode> acks = subscribe("hipereats/scooters/AV-02/acks");
        BlockingQueue<JsonNode> telemetry = subscribe("hipereats/scooters/AV-02/telemetry");
        String command = """
                {"commandId":"it-1","type":"UNLOCK","sentAt":"2026-11-25T19:40:00Z"}
                """;

        platform.publish("hipereats/scooters/AV-02/commands",
                new MqttMessage(command.getBytes(StandardCharsets.UTF_8)));

        JsonNode ack = acks.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(ack).isNotNull();
        assertThat(ack.get("commandId").asString()).isEqualTo("it-1");
        assertThat(ack.get("scooterCode").asString()).isEqualTo("AV-02");

        telemetry.clear();
        JsonNode reading = telemetry.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        assertThat(reading).isNotNull();
        assertThat(reading.get("locked").asBoolean()).isFalse();
    }
}
