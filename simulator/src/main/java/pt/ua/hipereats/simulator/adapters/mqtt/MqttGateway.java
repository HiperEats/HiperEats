package pt.ua.hipereats.simulator.adapters.mqtt;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
import pt.ua.hipereats.simulator.SimulatorProperties.Mqtt;
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
    private final Mqtt properties;
    private final JsonMapper json;
    // Paho must not publish from inside its own callback (it can deadlock), so commands are handled here.
    // A failed subscription is tried again here too.
    private final ScheduledThreadPoolExecutor commandExecutor = new ScheduledThreadPoolExecutor(1);
    private volatile boolean subscribed;

    public MqttGateway(Mqtt properties, JsonMapper json) throws MqttException {
        this.client = new MqttClient(properties.brokerUrl(), properties.clientId(), new MemoryPersistence());
        this.properties = properties;
        this.json = json;
        // After close(), a pending attempt to subscribe is dropped.
        commandExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    }

    /** Connects to the broker and passes every command received to {@code commandHandler(scooterCode, command)}. */
    public void connect(BiConsumer<String, ScooterCommand> commandHandler) throws MqttException {
        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverUri) {
                // Also called after an automatic reconnect: the clean session lost the subscription.
                subscribe();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                String scooterCode = MqttTopics.scooterCodeOf(topic);
                try {
                    ScooterCommand command = json.readValue(message.getPayload(), ScooterCommand.class);
                    if (command == null) {
                        throw new IllegalArgumentException("the payload is null");
                    }
                    commandExecutor.execute(() -> handle(commandHandler, scooterCode, command));
                } catch (RuntimeException e) {
                    log.warn("Invalid command on {} ignored: {}", topic,
                            new String(message.getPayload(), StandardCharsets.UTF_8));
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                subscribed = false;
                log.warn("Connection to the broker lost, reconnecting: {}", cause.getMessage());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // Nothing to do.
            }
        });
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setMaxReconnectDelay(properties.maxReconnectDelayMillis());
        options.setCleanSession(true);
        client.connect(options);
    }

    private void subscribe() {
        if (!client.isConnected()) {
            return; // connectComplete subscribes when the connection is back
        }
        try {
            client.subscribe(MqttTopics.ALL_COMMANDS, COMMAND_QOS);
            subscribed = true;
            log.info("Connected to {}, listening on {}", client.getServerURI(), MqttTopics.ALL_COMMANDS);
        } catch (MqttException e) {
            // Connected but not subscribed would mean no commands at all: keep trying.
            log.error("Could not subscribe to {}, retrying in {}", MqttTopics.ALL_COMMANDS,
                    properties.reconnectInterval(), e);
            try {
                commandExecutor.schedule(this::subscribe, properties.reconnectInterval().toMillis(),
                        TimeUnit.MILLISECONDS);
            } catch (RejectedExecutionException closed) {
                // close() was called meanwhile: no more attempts.
            }
        }
    }

    private static void handle(BiConsumer<String, ScooterCommand> commandHandler, String scooterCode,
                               ScooterCommand command) {
        try {
            commandHandler.accept(scooterCode, command);
        } catch (RuntimeException e) {
            log.error("Command {} for {} failed", command.commandId(), scooterCode, e);
        }
    }

    /** Whether the gateway is connected and subscribed, so it receives commands. */
    public boolean isSubscribed() {
        return subscribed && client.isConnected();
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
            if (qos == TELEMETRY_QOS) {
                // A lost reading is replaced by the next one.
                log.debug("Not connected, message to {} dropped", topic);
            } else {
                // The platform must resend the command with the same commandId to get the ack.
                log.warn("Not connected, acknowledgement to {} lost: {}", topic, payload);
            }
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
