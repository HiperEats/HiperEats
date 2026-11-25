package pt.ua.hipereats.simulator.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pt.ua.hipereats.simulator.domain.GeoPoint;
import pt.ua.hipereats.simulator.domain.ScooterType;
import pt.ua.hipereats.simulator.domain.SimulatedScooter;
import pt.ua.hipereats.simulator.domain.Store;
import pt.ua.hipereats.simulator.domain.Trip;

class FleetSimulatorTest {

    private static final Store STORE = new Store("AV", "Aveiro", new GeoPoint(40.6405, -8.6538), 1, 1);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-25T19:40:00Z"), ZoneId.of("Europe/Lisbon"));

    /** In-memory fake of the outbound port: records what would go to MQTT. */
    private static class RecordingPublisher implements FleetPublisher {
        final List<TelemetryMessage> telemetry = new ArrayList<>();
        final List<CommandAck> acks = new ArrayList<>();

        @Override
        public void publishTelemetry(TelemetryMessage message) {
            telemetry.add(message);
        }

        @Override
        public void publishAck(CommandAck ack) {
            acks.add(ack);
        }
    }

    private RecordingPublisher publisher;
    private SimulatedScooter av01;
    private SimulatedScooter av02;
    private FleetSimulator simulator;

    @BeforeEach
    void setUp() {
        publisher = new RecordingPublisher();
        av01 = new SimulatedScooter("AV-01", ScooterType.STANDARD, STORE, 80.7, 4);
        av02 = new SimulatedScooter("AV-02", ScooterType.LONG_RANGE, STORE, 50, 2);
        TripPlanner planner = new TripPlanner(new Random(42), 0.60, 0.35, 0.0, 2.0, 3.0, 1.2);
        simulator = new FleetSimulator(List.of(av01, av02), planner, publisher, CLOCK, Duration.ofSeconds(5), 20);
    }

    private static ScooterCommand unlock(String id) {
        return new ScooterCommand(id, ScooterCommand.UNLOCK, "2026-11-25T19:40:00Z");
    }

    @Test
    void whenTick_thenOneReadingPerScooter() {
        simulator.tick();

        assertThat(publisher.telemetry).extracting(TelemetryMessage::scooterCode)
                .containsExactly("AV-01", "AV-02");
    }

    @Test
    void whenTick_thenReadingDescribesAParkedScooter() {
        simulator.tick();

        TelemetryMessage reading = publisher.telemetry.getFirst();
        assertThat(reading.timestamp()).isEqualTo("2026-11-25T19:40:00.000Z");
        assertThat(reading.batteryPercent()).isEqualTo(80); // rounded down
        assertThat(reading.latitude()).isEqualTo(40.6405);
        assertThat(reading.longitude()).isEqualTo(-8.6538);
        assertThat(reading.locked()).isTrue();
        assertThat(reading.fault()).isNull();
    }

    @Test
    @DisplayName("BR12: an unlock command unlocks the scooter and is acknowledged")
    void whenUnlockCommand_thenScooterUnlockedAndAckPublished() {
        simulator.handleCommand("AV-01", unlock("cmd-1"));

        assertThat(av01.locked()).isFalse();
        assertThat(publisher.acks).containsExactly(
                new CommandAck("cmd-1", "AV-01", "UNLOCK", "2026-11-25T19:40:00.000Z"));
    }

    @Test
    void whenUnlockedScooterTicks_thenReadingShowsItUnlockedAndMoving() {
        simulator.handleCommand("AV-01", unlock("cmd-1"));

        simulator.tick();

        TelemetryMessage reading = publisher.telemetry.getFirst();
        assertThat(reading.locked()).isFalse();
        assertThat(new GeoPoint(reading.latitude(), reading.longitude())).isNotEqualTo(STORE.location());
    }

    @Test
    void whenSameCommandArrivesTwice_thenAckedTwiceButOneTrip() {
        simulator.handleCommand("AV-01", unlock("cmd-1"));
        simulator.tick();
        GeoPoint afterFirstTick = av01.position();

        simulator.handleCommand("AV-01", unlock("cmd-1"));

        assertThat(publisher.acks).hasSize(2);
        assertThat(av01.position()).isEqualTo(afterFirstTick);
        assertThat(av01.onTrip()).isTrue();
    }

    @Test
    @DisplayName("BR12: the same command id sent to two scooters unlocks both")
    void whenSameCommandIdGoesToTwoScooters_thenBothAreUnlockedAndAcked() {
        simulator.handleCommand("AV-01", unlock("cmd-1"));
        simulator.handleCommand("AV-02", unlock("cmd-1"));

        assertThat(av01.locked()).isFalse();
        assertThat(av02.locked()).isFalse();
        assertThat(publisher.acks).extracting(CommandAck::scooterCode).containsExactly("AV-01", "AV-02");
    }

    @Test
    @DisplayName("BR12: a command without id is refused, so no ack is sent for a locked scooter")
    void whenCommandHasNoId_thenIgnoredWithoutAck() {
        simulator.handleCommand("AV-01", unlock(null));
        simulator.handleCommand("AV-02", unlock(" "));

        assertThat(publisher.acks).isEmpty();
        assertThat(av01.locked()).isTrue();
        assertThat(av02.locked()).isTrue();
    }

    @Test
    void whenClockHasMilliseconds_thenTimestampsKeepThem() {
        Clock withMillis = Clock.fixed(Instant.parse("2026-11-25T19:40:00.123456Z"), ZoneId.of("Europe/Lisbon"));
        FleetSimulator precise = new FleetSimulator(List.of(av01), new TripPlanner(new Random(42), 0.60, 0.35, 0.0,
                2.0, 3.0, 1.2), publisher, withMillis, Duration.ofMillis(200), 20);

        precise.tick();
        precise.handleCommand("AV-01", unlock("cmd-1"));

        assertThat(publisher.telemetry.getFirst().timestamp()).isEqualTo("2026-11-25T19:40:00.123Z");
        assertThat(publisher.acks.getFirst().acknowledgedAt()).isEqualTo("2026-11-25T19:40:00.123Z");
    }

    @Test
    void whenCommandForUnknownScooter_thenIgnoredWithoutAck() {
        simulator.handleCommand("XX-99", unlock("cmd-1"));

        assertThat(publisher.acks).isEmpty();
    }

    @Test
    void whenUnknownCommandType_thenIgnoredWithoutAck() {
        simulator.handleCommand("AV-01", new ScooterCommand("cmd-1", "SELF_DESTRUCT", null));

        assertThat(publisher.acks).isEmpty();
        assertThat(av01.locked()).isTrue();
    }

    @Test
    @DisplayName("BR12: a command that failed is not acknowledged when it is sent again and fails again")
    void whenUnlockFails_thenItsIdIsNotTakenAsHandled() {
        TripPlanner failing = new TripPlanner(new Random(42), 0.60, 0.35, 0.0, 2.0, 3.0, 1.2) {
            @Override
            public Trip plan(SimulatedScooter scooter) {
                throw new IllegalStateException("cannot plan");
            }
        };
        FleetSimulator broken = new FleetSimulator(List.of(av01), failing, publisher, CLOCK,
                Duration.ofSeconds(5), 20);

        assertThatIllegalStateException().isThrownBy(() -> broken.handleCommand("AV-01", unlock("cmd-1")));
        assertThatIllegalStateException().isThrownBy(() -> broken.handleCommand("AV-01", unlock("cmd-1")));

        assertThat(publisher.acks).isEmpty();
        assertThat(av01.locked()).isTrue();
    }

    private FleetSimulator withBattery(double battery) {
        SimulatedScooter scooter = new SimulatedScooter("AV-01", ScooterType.STANDARD, STORE, battery, 4);
        return new FleetSimulator(List.of(scooter), new TripPlanner(new Random(42), 0.60, 0.35, 0.0, 2.0, 3.0, 1.2),
                publisher, CLOCK, Duration.ofSeconds(5), 20);
    }

    @ParameterizedTest
    @DisplayName("BR12: a scooter that reports 0 % does not unlock, so it sends no acknowledgement")
    @ValueSource(doubles = {0, 0.05, 0.99})
    void whenBatteryIsEmpty_thenUnlockIsIgnoredWithoutAck(double battery) {
        FleetSimulator empty = withBattery(battery);

        empty.handleCommand("AV-01", unlock("cmd-1"));
        empty.tick();

        assertThat(publisher.acks).isEmpty();
        assertThat(publisher.telemetry).extracting(TelemetryMessage::locked).containsExactly(true);
    }

    @Test
    @DisplayName("BR12: with 1 % the scooter unlocks and its next reading shows it")
    void whenBatteryIsAlmostEmpty_thenAckedAndReadingShowsItUnlocked() {
        FleetSimulator almostEmpty = withBattery(1);

        almostEmpty.handleCommand("AV-01", unlock("cmd-1"));
        almostEmpty.tick();

        assertThat(publisher.acks).hasSize(1);
        assertThat(publisher.telemetry).extracting(TelemetryMessage::locked).containsExactly(false);
    }

    @Test
    void whenBatteryEmptiesDuringTheTrip_thenCommandsAreStillAcked() {
        FleetSimulator almostEmpty = withBattery(1);
        almostEmpty.handleCommand("AV-01", unlock("cmd-1"));
        almostEmpty.tick(); // now below 1 %, reported as 0 %, but unlocked

        almostEmpty.handleCommand("AV-01", unlock("cmd-1"));
        almostEmpty.handleCommand("AV-01", unlock("cmd-2"));

        assertThat(publisher.telemetry.getFirst().batteryPercent()).isZero();
        assertThat(publisher.acks).extracting(CommandAck::commandId).containsExactly("cmd-1", "cmd-1", "cmd-2");
    }

    @Test
    void whenClockHasTrailingZeros_thenTimestampStillHasThreeDecimals() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-25T19:40:00.370Z"), ZoneId.of("Europe/Lisbon"));
        FleetSimulator precise = new FleetSimulator(List.of(av01), new TripPlanner(new Random(42), 0.60, 0.35, 0.0,
                2.0, 3.0, 1.2), publisher, clock, Duration.ofSeconds(5), 20);

        precise.tick();

        assertThat(publisher.telemetry.getFirst().timestamp()).isEqualTo("2026-11-25T19:40:00.370Z");
    }

    @Test
    void whenSummerTime_thenTimestampHasTheLisbonOffset() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-01T10:00:00Z"), ZoneId.of("Europe/Lisbon"));
        FleetSimulator summer = new FleetSimulator(List.of(av01), new TripPlanner(new Random(42), 0.60, 0.35, 0.0,
                2.0, 3.0, 1.2), publisher, clock, Duration.ofSeconds(5), 20);

        summer.tick();

        assertThat(publisher.telemetry.getFirst().timestamp()).isEqualTo("2026-07-01T11:00:00.000+01:00");
    }
}
