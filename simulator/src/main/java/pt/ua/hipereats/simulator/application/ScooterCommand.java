package pt.ua.hipereats.simulator.application;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * A command from the platform, received on {@code hipereats/scooters/{scooterCode}/commands}.
 * Only {@code UNLOCK} exists for now (BR12).
 *
 * @param commandId id chosen by the platform; the acknowledgement repeats it
 * @param type      {@code UNLOCK}
 * @param sentAt    ISO-8601 with offset (informative)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ScooterCommand(String commandId, String type, String sentAt) {

    public static final String UNLOCK = "UNLOCK";
}
