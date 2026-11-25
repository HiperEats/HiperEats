package pt.ua.hipereats.simulator.adapters.mqtt;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.hipereats.simulator.application.CommandAck;
import pt.ua.hipereats.simulator.application.FleetPublisher;
import pt.ua.hipereats.simulator.application.ScooterCommand;
import pt.ua.hipereats.simulator.application.TelemetryMessage;
import tools.jackson.databind.json.JsonMapper;

/**
 * MQTT adapter: publishes telemetry and acknowledgements, and receives the platform's commands.
 *
 * <p>Telemetry goes with QoS 0 (a lost reading is replaced 5 s later); commands and acknowledgements
 * with QoS 1 (delivered at least once, so duplicates are possible and handled).
 */
public class MqttGateway implements FleetPublisher, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(MqttGateway.class);
    private static final int TELEMETRY_QOS = 0;
    private static final int COMMAND_QOS = 1;

    private final MqttClient client;
    private final JsonMapper json;
    // Paho must not publish from inside its own callback (it can deadlock), so commands are handled here.
    private final ExecutorService commandExecutor = Executors.newSingleThreadExecutor();

    public MqttGateway(String brokerUrl, String clientId, JsonMapper json) throws MqttException {
        this.client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
        this.json = json;
    }

    /** Connects to the broker and passes every command received to {@code commandHandler(scooterCode, command)}. */
    public void connect(BiConsumer<String, ScooterCommand> commandHandler) throws MqttException {
        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverUri) {
                // Also called after an automatic reconnect: the clean session lost the subscription.
                try {
                    client.subscribe(MqttTopics.ALL_COMMANDS, COMMAND_QOS);
                    log.info("Connected to {}, listening on {}", serverUri, MqttTopics.ALL_COMMANDS);
                } catch (MqttException e) {
                    log.error("Could not subscribe to {}", MqttTopics.ALL_COMMANDS, e);
                }
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                String scooterCode = MqttTopics.scooterCodeOf(topic);
                try {
                    ScooterCommand command = json.readValue(message.getPayload(), ScooterCommand.class);
                    commandExecutor.execute(() -> commandHandler.accept(scooterCode, command));
                } catch (RuntimeException e) {
                    log.warn("Invalid command on {} ignored: {}", topic,
                            new String(message.getPayload(), StandardCharsets.UTF_8));
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                log.warn("Connection to the broker lost, reconnecting: {}", cause.getMessage());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // Nothing to do.
            }
        });
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        client.connect(options);
    }

    @Override
    public void publishTelemetry(TelemetryMessage message) {
        publish(MqttTopics.telemetry(message.scooterCode()), message, TELEMETRY_QOS);
    }

    @Override
    public void publishAck(CommandAck ack) {
        publish(MqttTopics.acks(ack.scooterCode()), ack, COMMAND_QOS);
    }

    private void publish(String topic, Object payload, int qos) {
        if (!client.isConnected()) {
            log.debug("Not connected, message to {} dropped", topic);
            return;
        }
        try {
            MqttMessage message = new MqttMessage(json.writeValueAsBytes(payload));
            message.setQos(qos);
            client.publish(topic, message);
        } catch (MqttException e) {
            log.warn("Could not publish to {}: {}", topic, e.getMessage());
        }
    }

    @Override
    public void close() throws MqttException {
        commandExecutor.shutdown();
        if (client.isConnected()) {
            client.disconnect();
        }
        client.close();
    }
}
