package pt.ua.hipereats.simulator.adapters.mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import pt.ua.hipereats.simulator.SimulatorProperties.Mqtt;
import pt.ua.hipereats.simulator.application.ScooterCommand;
import tools.jackson.databind.json.JsonMapper;

/** Subscribing and receiving commands, when the broker refuses or a message is bad. */
@ExtendWith(OutputCaptureExtension.class)
class MqttGatewayTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(6);
    private static final String TOPIC = "hipereats/scooters/AV-01/commands";
    private static final String UNLOCK = """
            {"commandId":"cmd-1","type":"UNLOCK"}""";

    private FakeMqttBroker broker;
    private MqttGateway gateway;
    private final List<String> handled = new CopyOnWriteArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        broker = new FakeMqttBroker();
        gateway = new MqttGateway(new Mqtt(broker.url(), "test-simulator", Duration.ofMillis(200)),
                JsonMapper.builder().build());
    }

    @AfterEach
    void tearDown() throws Exception {
        gateway.close();
        broker.close();
    }

    private void handle(String scooterCode, ScooterCommand command) {
        handled.add(scooterCode + "/" + command.commandId());
        if (command.commandId().equals("broken")) {
            throw new IllegalStateException("cannot carry out");
        }
    }

    private void connectAndSubscribe() throws Exception {
        gateway.connect(this::handle);
        await().atMost(TIMEOUT).until(gateway::isSubscribed);
    }

    @Test
    void whenCommandArrives_thenItIsHandled() throws Exception {
        connectAndSubscribe();

        broker.publish(TOPIC, UNLOCK);

        await().atMost(TIMEOUT).untilAsserted(() -> assertThat(handled).containsExactly("AV-01/cmd-1"));
    }

    @Test
    void whenSubscriptionIsRefused_thenItIsTriedAgain() throws Exception {
        broker.refuseNextSubscription();

        gateway.connect(this::handle);

        await().atMost(TIMEOUT).untilAsserted(() -> assertThat(broker.subscribes()).isEqualTo(2));
        await().atMost(TIMEOUT).until(gateway::isSubscribed);
        assertThat(broker.connects()).as("connections asked for").isEqualTo(1);
    }

    @Test
    void whenClosedAfterARefusedSubscription_thenItIsNotTriedAgain() throws Exception {
        broker.refuseNextSubscription();
        gateway.connect(this::handle);
        await().atMost(TIMEOUT).until(() -> broker.subscribes() == 1);

        gateway.close();

        await().during(Duration.ofMillis(800)).atMost(TIMEOUT).untilAsserted(
                () -> assertThat(broker.subscribes()).isEqualTo(1));
    }

    @Test
    void whenPayloadIsNull_thenItIsLoggedAsInvalid(CapturedOutput output) throws Exception {
        connectAndSubscribe();

        broker.publish(TOPIC, "null");

        await().atMost(TIMEOUT).untilAsserted(
                () -> assertThat(output).contains("Invalid command on " + TOPIC));
        assertThat(output).doesNotContain("NullPointerException");
    }

    @Test
    void whenACommandFails_thenTheErrorIsLoggedAndTheNextOneIsHandled(CapturedOutput output) throws Exception {
        connectAndSubscribe();

        broker.publish(TOPIC, """
                {"commandId":"broken","type":"UNLOCK"}""");
        broker.publish(TOPIC, UNLOCK);

        await().atMost(TIMEOUT).untilAsserted(
                () -> assertThat(handled).containsExactly("AV-01/broken", "AV-01/cmd-1"));
        assertThat(output).contains("Command broken for AV-01 failed").doesNotContain("Exception in thread");
    }
}
