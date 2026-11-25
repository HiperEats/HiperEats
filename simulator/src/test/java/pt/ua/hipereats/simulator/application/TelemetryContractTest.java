package pt.ua.hipereats.simulator.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Contract tests: the JSON on the wire must match the telemetry contract in AGENTS.md, because the
 * backend (and any replacement simulator) relies on these exact field names.
 */
class TelemetryContractTest {

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void whenTelemetrySerialized_thenItHasExactlyTheContractFields() {
        TelemetryMessage message = new TelemetryMessage("AV-01", "2026-11-25T19:40:00Z", 80,
                40.6405, -8.6538, true, null);

        JsonNode node = json.readTree(json.writeValueAsString(message));

        assertThat(node.propertyNames()).containsExactlyInAnyOrder(
                "scooterCode", "timestamp", "batteryPercent", "latitude", "longitude", "locked", "fault");
        assertThat(node.get("scooterCode").asString()).isEqualTo("AV-01");
        assertThat(node.get("batteryPercent").isInt()).isTrue();
        assertThat(node.get("locked").isBoolean()).isTrue();
        assertThat(node.get("fault").isNull()).isTrue();
    }

    @Test
    void whenAckSerialized_thenItRepeatsTheCommandId() {
        CommandAck ack = new CommandAck("cmd-1", "AV-01", "UNLOCK", "2026-11-25T19:40:00Z");

        JsonNode node = json.readTree(json.writeValueAsString(ack));

        assertThat(node.propertyNames()).containsExactlyInAnyOrder(
                "commandId", "scooterCode", "type", "acknowledgedAt");
        assertThat(node.get("commandId").asString()).isEqualTo("cmd-1");
    }

    @Test
    void whenCommandHasExtraFields_thenTheyAreIgnored() {
        String payload = """
                {"commandId":"cmd-1","type":"UNLOCK","sentAt":"2026-11-25T19:40:00Z","bookingId":42}
                """;

        ScooterCommand command = json.readValue(payload, ScooterCommand.class);

        assertThat(command).isEqualTo(new ScooterCommand("cmd-1", "UNLOCK", "2026-11-25T19:40:00Z"));
    }
}
