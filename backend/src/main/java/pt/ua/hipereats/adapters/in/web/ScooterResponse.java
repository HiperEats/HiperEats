package pt.ua.hipereats.adapters.in.web;

import pt.ua.hipereats.domain.fleet.Scooter;
import pt.ua.hipereats.domain.fleet.ScooterState;
import pt.ua.hipereats.domain.fleet.ScooterType;

/** A scooter as the API shows it. */
public record ScooterResponse(String code, ScooterType type, ScooterState state, long storeId) {

    static ScooterResponse from(Scooter scooter) {
        return new ScooterResponse(scooter.code(), scooter.type(), scooter.state(),
                scooter.storeId());
    }
}
