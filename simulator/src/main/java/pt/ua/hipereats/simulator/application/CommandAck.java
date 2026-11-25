package pt.ua.hipereats.simulator.application;

/**
 * Acknowledgement of a command, published on {@code hipereats/scooters/{scooterCode}/acks}
 * once the scooter has carried it out (for UNLOCK: the scooter is unlocked).
 *
 * @param acknowledgedAt ISO-8601 with offset
 */
public record CommandAck(String commandId, String scooterCode, String type, String acknowledgedAt) {
}
