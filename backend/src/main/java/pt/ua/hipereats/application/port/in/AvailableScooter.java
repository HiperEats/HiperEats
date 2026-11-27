package pt.ua.hipereats.application.port.in;

import pt.ua.hipereats.domain.fleet.ScooterType;

/** A scooter a courier can pick for a delivery, with what the courier needs to choose it. */
public record AvailableScooter(String code, ScooterType type, int batteryPercent) {
}
