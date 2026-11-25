package pt.ua.hipereats.adapters.out.telemetry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import pt.ua.hipereats.application.port.in.RecordTelemetryUseCase;
import pt.ua.hipereats.application.port.in.RecordTelemetryUseCase.Result;

/** The subscriber as a Spring bean: when it exists, and which broker it uses. */
class MqttTelemetrySubscriberConfigurationTest {

    private FakeMqttBroker broker;
    private ApplicationContextRunner application;

    @BeforeEach
    void startBroker() throws IOException {
        broker = new FakeMqttBroker();
        application = new ApplicationContextRunner()
                .withBean(RecordTelemetryUseCase.class, () -> reading -> Result.RECORDED)
                .withUserConfiguration(MqttTelemetrySubscriber.class)
                .withPropertyValues("hipereats.telemetry.broker-url=" + broker.url());
    }

    @AfterEach
    void stopBroker() throws IOException {
        broker.close();
    }

    @Test
    void whenApplicationStarts_thenItSubscribesOnTheConfiguredBroker() {
        application.run(context -> {
            MqttTelemetrySubscriber subscriber = context.getBean(MqttTelemetrySubscriber.class);

            await().atMost(Duration.ofSeconds(6)).until(subscriber::isSubscribed);
            assertThat(broker.subscribes()).isEqualTo(1);
        });
    }

    @Test
    void whenTelemetryIsDisabled_thenThereIsNoSubscriber() {
        application.withPropertyValues("hipereats.telemetry.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(MqttTelemetrySubscriber.class));
    }

    @Test
    void whenBrokerUrlIsInvalid_thenApplicationDoesNotStart() {
        application.withPropertyValues("hipereats.telemetry.broker-url=mqtt://mosquitto:1883")
                .run(context -> assertThat(context).hasFailed());
    }
}
