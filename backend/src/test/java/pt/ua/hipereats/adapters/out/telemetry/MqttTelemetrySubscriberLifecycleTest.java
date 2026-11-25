package pt.ua.hipereats.adapters.out.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pt.ua.hipereats.application.port.in.RecordTelemetryUseCase.Result;

/** Connecting, subscribing and stopping, when the broker is slow or refuses the subscription. */
class MqttTelemetrySubscriberLifecycleTest {

    private static final Duration SLOW_CONNECTION = Duration.ofMillis(800);
    /** Longer than the slow connection, so an answer that arrives after stop() is seen too. */
    private static final Duration LONG_ENOUGH = Duration.ofMillis(1_500);
    private static final Duration TIMEOUT = Duration.ofSeconds(6);

    private FakeMqttBroker broker;
    private MqttTelemetrySubscriber subscriber;

    @BeforeEach
    void setUp() throws IOException {
        broker = new FakeMqttBroker();
        subscriber = new MqttTelemetrySubscriber(
                new TelemetryProperties(true, broker.url(), "test", Duration.ofMillis(200)),
                reading -> Result.RECORDED);
    }

    @AfterEach
    void tearDown() throws IOException {
        subscriber.stop();
        broker.close();
    }

    private void awaitNoConnectionLeftOpen() {
        await().atMost(TIMEOUT).during(LONG_ENOUGH).untilAsserted(
                () -> assertThat(broker.openConnections()).as("connections left open").isZero());
    }

    @Test
    void whenStarted_thenSubscribed() {
        subscriber.start();

        await().atMost(TIMEOUT).until(subscriber::isSubscribed);
    }

    @Test
    void whenStopped_thenNoConnectionIsLeftOpen() {
        subscriber.start();
        await().atMost(TIMEOUT).until(subscriber::isSubscribed);

        subscriber.stop();

        assertThat(subscriber.isRunning()).isFalse();
        awaitNoConnectionLeftOpen();
    }

    @Test
    void whenStoppedWhileConnecting_thenNoConnectionIsLeftOpen() {
        broker.delayConnections(SLOW_CONNECTION);
        subscriber.start();
        await().atMost(TIMEOUT).until(() -> broker.connects() == 1);

        subscriber.stop();

        awaitNoConnectionLeftOpen();
    }

    @Test
    void whenRestartedWhileConnecting_thenOnlyOneConnectionIsKept() {
        broker.delayConnections(SLOW_CONNECTION);
        subscriber.start();
        await().atMost(TIMEOUT).until(() -> broker.connects() == 1);

        subscriber.stop();
        subscriber.start();

        await().atMost(TIMEOUT).until(subscriber::isSubscribed);
        await().atMost(TIMEOUT).during(LONG_ENOUGH).untilAsserted(
                () -> assertThat(broker.openConnections()).as("open connections").isEqualTo(1));
        assertThat(broker.connects()).as("connections asked for").isEqualTo(2);
        assertThat(broker.subscribes()).as("subscriptions").isEqualTo(1);
    }

    @Test
    void whenStoppedWhileReconnecting_thenNoConnectionIsLeftOpen() throws IOException {
        subscriber.start();
        await().atMost(TIMEOUT).until(subscriber::isSubscribed);
        broker.delayConnections(SLOW_CONNECTION);
        broker.dropConnections();
        await().atMost(TIMEOUT).until(() -> broker.connects() == 2);

        subscriber.stop();

        awaitNoConnectionLeftOpen();
    }

    @Test
    void whenSubscriptionIsRefused_thenItIsTriedAgain() {
        broker.refuseNextSubscription();

        subscriber.start();

        await().atMost(TIMEOUT).until(subscriber::isSubscribed);
        assertThat(broker.subscribes()).isEqualTo(2);
        assertThat(broker.connects()).as("connections asked for").isEqualTo(1);
    }

    @Test
    void whenStoppedAfterARefusedSubscription_thenItIsNotTriedAgain() {
        broker.refuseNextSubscription();
        subscriber.start();
        await().atMost(TIMEOUT).until(() -> broker.subscribes() == 1);

        subscriber.stop();

        await().atMost(TIMEOUT).during(LONG_ENOUGH).untilAsserted(
                () -> assertThat(broker.subscribes()).isEqualTo(1));
    }
}
