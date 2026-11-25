package pt.ua.hipereats.simulator.adapters.mqtt;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MqttTopicsTest {

    @Test
    void whenScooterCodeGiven_thenTopicsFollowTheContract() {
        assertThat(MqttTopics.telemetry("AV-01")).isEqualTo("hipereats/scooters/AV-01/telemetry");
        assertThat(MqttTopics.commands("AV-01")).isEqualTo("hipereats/scooters/AV-01/commands");
        assertThat(MqttTopics.acks("AV-01")).isEqualTo("hipereats/scooters/AV-01/acks");
    }

    @Test
    void whenCommandTopicReceived_thenScooterCodeIsExtracted() {
        assertThat(MqttTopics.scooterCodeOf("hipereats/scooters/IL-07/commands")).isEqualTo("IL-07");
    }
}
