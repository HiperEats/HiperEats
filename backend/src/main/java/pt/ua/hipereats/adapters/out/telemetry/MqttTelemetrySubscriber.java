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
import pt.ua.hipereats.domain.telemetry.TelemetryReading;

/**
 * Telemetry adapter: subscribes to the telemetry of every scooter on the MQTT broker and hands each
 * reading to {@link RecordTelemetryUseCase}. Invalid messages are logged and dropped.
 *
 * <p>The backend starts even if the broker is down: it retries the connection every
 * {@code reconnect-interval}, and Paho reconnects by itself (waiting at most that interval) if the
 * connection drops later.
 */
@Component
@EnableConfigurationProperties(TelemetryProperties.class)
@ConditionalOnProperty(name = "hipereats.telemetry.enabled", havingValue = "true", matchIfMissing = true)
public class MqttTelemetrySubscriber implements SmartLifecycle {

    /** Telemetry of every scooter; "+" is the MQTT single-level wildcard. */
    static final String TOPIC = "hipereats/scooters/+/telemetry";
    private static final String TOPIC_PREFIX = "hipereats/scooters/";
    private static final String TOPIC_SUFFIX = "/telemetry";
    private static final int QOS = 0;
    private static final Logger log = LoggerFactory.getLogger(MqttTelemetrySubscriber.class);

    private final TelemetryProperties properties;
    private final RecordTelemetryUseCase recordTelemetry;
    private volatile ScheduledExecutorService connector;
    private volatile MqttClient client;
    private volatile boolean running;
    private volatile boolean subscribed;

    public MqttTelemetrySubscriber(TelemetryProperties properties, RecordTelemetryUseCase recordTelemetry) {
        this.properties = properties;
        this.recordTelemetry = recordTelemetry;
    }

    /** Can run again after {@link #stop()}: Spring stops and restarts the beans of a paused test context. */
    @Override
    public synchronized void start() {
        running = true;
        connector = Executors.newSingleThreadScheduledExecutor();
        connector.execute(this::connect);
    }

    private void connect() {
        MqttClient newClient = null;
        try {
            String clientId = properties.clientId() + "-" + UUID.randomUUID().toString().substring(0, 8);
            newClient = new MqttClient(properties.brokerUrl(), clientId, new MemoryPersistence());
            newClient.setCallback(new Callback(newClient));
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setMaxReconnectDelay((int) properties.reconnectInterval().toMillis());
            options.setCleanSession(true);
            newClient.connect(options);
            synchronized (this) {
                if (running) {
                    client = newClient;
                    return;
                }
            }
            // stop() ran while connecting: nobody else will close this client.
            closeQuietly(newClient);
        } catch (MqttException e) {
            closeQuietly(newClient);
            log.warn("Telemetry broker {} unreachable ({}), retrying in {}", properties.brokerUrl(),
                    e.getMessage(), properties.reconnectInterval());
            retryLater(this::connect);
        } catch (RuntimeException e) {
            // Not expected (the URL is validated at startup), but never fail silently on the executor.
            closeQuietly(newClient);
            log.error("Telemetry subscriber could not start, retrying in {}", properties.reconnectInterval(), e);
            retryLater(this::connect);
        }
    }

    /** Called on every (re)connection: the clean session loses the subscription. */
    private void subscribe(MqttClient mqtt) {
        if (!running || !mqtt.isConnected()) {
            return;
        }
        try {
            mqtt.subscribe(TOPIC, QOS);
            subscribed = true;
            log.info("Connected to {}, subscribed to {}", mqtt.getServerURI(), TOPIC);
        } catch (MqttException e) {
            // Connected but not subscribed would mean no telemetry at all: keep trying.
            log.error("Could not subscribe to {}, retrying in {}", TOPIC, properties.reconnectInterval(), e);
            retryLater(() -> subscribe(mqtt));
        }
    }

    private void retryLater(Runnable task) {
        try {
            connector.schedule(task, properties.reconnectInterval().toMillis(), TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException stopped) {
            // stop() was called meanwhile: no more attempts.
        }
    }

    /**
     * Converts one MQTT message and records it; never throws, so one bad message does not stop the
     * stream. A message about another scooter than the one in its topic is dropped.
     */
    void handle(String topic, byte[] payload) {
        try {
            TelemetryReading reading = TelemetryMessage.parse(payload).toReading();
            String topicCode = scooterCodeOf(topic);
            if (!reading.scooterCode().equals(topicCode)) {
                throw new IllegalArgumentException("scooterCode " + reading.scooterCode()
                        + " does not match the topic");
            }
            recordTelemetry.record(reading);
        } catch (RuntimeException e) {
            log.warn("Invalid telemetry on {} dropped ({}): {}", topic, e.getMessage(),
                    new String(payload, StandardCharsets.UTF_8));
        }
    }

    /** "hipereats/scooters/AV-01/telemetry" gives "AV-01". */
    private static String scooterCodeOf(String topic) {
        if (!topic.startsWith(TOPIC_PREFIX) || !topic.endsWith(TOPIC_SUFFIX)) {
            throw new IllegalArgumentException("unexpected topic");
        }
        return topic.substring(TOPIC_PREFIX.length(), topic.length() - TOPIC_SUFFIX.length());
    }

    @Override
    public void stop() {
        MqttClient current;
        synchronized (this) {
            running = false;
            subscribed = false;
            current = client;
            client = null;
        }
        if (connector != null) {
            connector.shutdownNow();
        }
        closeQuietly(current);
    }

    private static void closeQuietly(MqttClient mqtt) {
        if (mqtt == null) {
            return;
        }
        try {
            if (mqtt.isConnected()) {
                mqtt.disconnect();
            }
            mqtt.close();
        } catch (MqttException e) {
            log.debug("Error closing the MQTT client", e);
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /** Whether the subscriber is connected and subscribed, so it receives telemetry. */
    public boolean isSubscribed() {
        MqttClient current = client;
        return subscribed && current != null && current.isConnected();
    }

    private final class Callback implements MqttCallbackExtended {

        private final MqttClient mqtt;

        Callback(MqttClient mqtt) {
            this.mqtt = mqtt;
        }

        @Override
        public void connectComplete(boolean reconnect, String serverUri) {
            subscribe(mqtt);
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            handle(topic, message.getPayload());
        }

        @Override
        public void connectionLost(Throwable cause) {
            subscribed = false;
            log.warn("Connection to the telemetry broker lost, reconnecting: {}", cause.getMessage());
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            // The backend does not publish on this client.
        }
    }
}
