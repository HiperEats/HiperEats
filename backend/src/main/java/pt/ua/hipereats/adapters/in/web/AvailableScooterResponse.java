package pt.ua.hipereats.adapters.in.web;

import pt.ua.hipereats.application.port.in.AvailableScooter;
import pt.ua.hipereats.domain.fleet.ScooterType;

/** A scooter in the list a courier chooses from (HE-31). */
public record AvailableScooterResponse(String code, ScooterType type, int batteryPercent) {

    static AvailableScooterResponse from(AvailableScooter scooter) {
        return new AvailableScooterResponse(scooter.code(), scooter.type(), scooter.batteryPercent());
    }
}
