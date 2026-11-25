package pt.ua.hipereats.adapters.out.telemetry;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import pt.ua.hipereats.application.port.in.RecordTelemetryUseCase;
import tools.jackson.databind.json.JsonMapper;

/**
 * Telemetry adapter: subscribes to the telemetry of every scooter on the MQTT broker and hands each
 * reading to {@link RecordTelemetryUseCase}. Invalid messages are logged and dropped.
 *
 * <p>The backend starts even if the broker is down: it retries the connection every
 * {@code reconnect-interval}, and Paho reconnects by itself if the connection drops later.
 */
@Component
@EnableConfigurationProperties(TelemetryProperties.class)
@ConditionalOnProperty(name = "hipereats.telemetry.enabled", havingValue = "true", matchIfMissing = true)
public class MqttTelemetrySubscriber implements SmartLifecycle {

    /** Telemetry of every scooter; "+" is the MQTT single-level wildcard. */
    static final String TOPIC = "hipereats/scooters/+/telemetry";
    private static final int QOS = 0;
    private static final Logger log = LoggerFactory.getLogger(MqttTelemetrySubscriber.class);

    private final TelemetryProperties properties;
    private final RecordTelemetryUseCase recordTelemetry;
    private final JsonMapper json;
    private volatile ScheduledExecutorService connector;
    private volatile MqttClient client;
    private volatile boolean running;

    public MqttTelemetrySubscriber(TelemetryProperties properties, RecordTelemetryUseCase recordTelemetry,
                                   JsonMapper json) {
        this.properties = properties;
        this.recordTelemetry = recordTelemetry;
        this.json = json;
    }

    /** Can run again after {@link #stop()}: Spring stops and restarts the beans of a paused test context. */
    @Override
    public void start() {
        running = true;
        connector = Executors.newSingleThreadScheduledExecutor();
        connector.execute(this::connect);
    }

    private void connect() {
        try {
            String clientId = properties.clientId() + "-" + UUID.randomUUID().toString().substring(0, 8);
            MqttClient newClient = new MqttClient(properties.brokerUrl(), clientId, new MemoryPersistence());
            newClient.setCallback(new Callback(newClient));
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            newClient.connect(options);
            client = newClient;
        } catch (MqttException e) {
            log.warn("Telemetry broker {} unreachable ({}), retrying in {}", properties.brokerUrl(),
                    e.getMessage(), properties.reconnectInterval());
            try {
                connector.schedule(this::connect, properties.reconnectInterval().toMillis(), TimeUnit.MILLISECONDS);
            } catch (RejectedExecutionException stopped) {
                // stop() was called meanwhile: no more attempts.
            }
        }
    }

    /** Converts one MQTT message and records it; never throws, so one bad message does not stop the stream. */
    void handle(String topic, byte[] payload) {
        try {
            TelemetryMessage message = json.readValue(payload, TelemetryMessage.class);
            recordTelemetry.record(message.toReading());
        } catch (RuntimeException e) {
            log.warn("Invalid telemetry on {} dropped ({}): {}", topic, e.getMessage(),
                    new String(payload, StandardCharsets.UTF_8));
        }
    }

    @Override
    public void stop() {
        running = false;
        if (connector != null) {
            connector.shutdownNow();
        }
        MqttClient current = client;
        client = null;
        if (current != null) {
            try {
                if (current.isConnected()) {
                    current.disconnect();
                }
                current.close();
            } catch (MqttException e) {
                log.debug("Error closing the MQTT client", e);
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /** Whether the subscriber is connected to the broker (used by the integration test). */
    public boolean isConnected() {
        MqttClient current = client;
        return current != null && current.isConnected();
    }

    private final class Callback implements MqttCallbackExtended {

        private final MqttClient mqtt;

        Callback(MqttClient mqtt) {
            this.mqtt = mqtt;
        }

        @Override
        public void connectComplete(boolean reconnect, String serverUri) {
            // Also called after an automatic reconnect: the clean session lost the subscription.
            try {
                mqtt.subscribe(TOPIC, QOS);
                log.info("Connected to {}, subscribed to {}", serverUri, TOPIC);
            } catch (MqttException e) {
                log.error("Could not subscribe to {}", TOPIC, e);
            }
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            handle(topic, message.getPayload());
        }

        @Override
        public void connectionLost(Throwable cause) {
            log.warn("Connection to the telemetry broker lost, reconnecting: {}", cause.getMessage());
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            // The backend does not publish on this client.
        }
    }
}
