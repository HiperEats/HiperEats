package pt.ua.hipereats.application.port.in;

import pt.ua.hipereats.domain.fleet.ScooterType;

/** What the operator gives to register a scooter. */
public record RegisterScooterCommand(String code, ScooterType type, long storeId) {
}
