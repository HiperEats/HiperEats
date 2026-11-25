package pt.ua.hipereats.simulator.application;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.hipereats.simulator.domain.SimulatedScooter;

/**
 * The simulation loop. Every tick moves the scooters that are on a trip and publishes one telemetry
 * reading per scooter; unlock commands from the platform start a trip and are acknowledged.
 *
 * <p>Ticks (scheduler thread) and commands (MQTT thread) can arrive at the same time, so the public
 * methods are synchronized.
 */
public class FleetSimulator {

    private static final Logger log = LoggerFactory.getLogger(FleetSimulator.class);

    private final Map<String, SimulatedScooter> fleet = new LinkedHashMap<>();
    /** Commands already handled, as "scooterCode/commandId": an id only identifies a command per scooter. */
    private final Set<String> handledCommands = new HashSet<>();
    private final TripPlanner tripPlanner;
    private final FleetPublisher publisher;
    private final Clock clock;
    private final Duration tickInterval;
    private final double speedKmh;

    public FleetSimulator(List<SimulatedScooter> scooters, TripPlanner tripPlanner, FleetPublisher publisher,
                          Clock clock, Duration tickInterval, double speedKmh) {
        scooters.forEach(scooter -> fleet.put(scooter.code(), scooter));
        this.tripPlanner = tripPlanner;
        this.publisher = publisher;
        this.clock = clock;
        this.tickInterval = tickInterval;
        this.speedKmh = speedKmh;
    }

    /** One simulation step: advance every scooter by one interval, then publish its telemetry. */
    public synchronized void tick() {
        String now = now();
        for (SimulatedScooter scooter : fleet.values()) {
            scooter.advance(tickInterval, speedKmh);
            publisher.publishTelemetry(new TelemetryMessage(
                    scooter.code(),
                    now,
                    (int) Math.floor(scooter.batteryPercent()),
                    scooter.position().latitude(),
                    scooter.position().longitude(),
                    scooter.locked(),
                    scooter.fault()));
        }
    }

    /**
     * Handles a command sent to one scooter. Unknown scooters, command types and commands without
     * id are ignored (no acknowledgement, so the platform can time out). The same command received
     * twice by the same scooter is acknowledged again but starts no second trip, because MQTT may
     * deliver a message twice.
     */
    public synchronized void handleCommand(String scooterCode, ScooterCommand command) {
        SimulatedScooter scooter = fleet.get(scooterCode);
        if (scooter == null) {
            log.warn("Command {} for unknown scooter {} ignored", command.commandId(), scooterCode);
            return;
        }
        if (!ScooterCommand.UNLOCK.equals(command.type())) {
            log.warn("Unknown command type {} for {} ignored", command.type(), scooterCode);
            return;
        }
        if (command.commandId() == null || command.commandId().isBlank()) {
            // Without an id, a repeated command cannot be told from a new one: refuse it.
            log.warn("Command without commandId for {} ignored", scooterCode);
            return;
        }
        if (handledCommands.add(scooterCode + "/" + command.commandId())) {
            boolean started = scooter.unlock(tripPlanner.plan(scooter));
            log.info("Unlock {} for {}: {}", command.commandId(), scooterCode,
                    started ? "trip started" : "already on a trip");
        }
        publisher.publishAck(new CommandAck(command.commandId(), scooterCode, command.type(), now()));
    }

    public synchronized List<SimulatedScooter> scooters() {
        return List.copyOf(fleet.values());
    }

    private String now() {
        // Milliseconds: with an interval below 1 s, readings of the same second must stay in order.
        return OffsetDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
