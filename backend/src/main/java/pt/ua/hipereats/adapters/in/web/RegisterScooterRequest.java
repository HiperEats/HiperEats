package pt.ua.hipereats.adapters.in.web;

import java.util.regex.Pattern;
import pt.ua.hipereats.application.port.in.RegisterScooterCommand;
import pt.ua.hipereats.domain.fleet.ScooterType;

/**
 * Body of {@code POST /api/v1/scooters}. An invalid body is refused while it is read, with
 * {@code 400 Bad Request}.
 *
 * @param code letters, digits and hyphens, 1 to 32 characters: it is part of the scooter's URL and
 *             of its MQTT topic (an assumption of HE-29; the specification sets no format)
 */
public record RegisterScooterRequest(String code, ScooterType type, Long storeId) {

    private static final Pattern CODE_FORMAT = Pattern.compile("[A-Za-z0-9-]{1,32}");

    public RegisterScooterRequest {
        if (code == null || !CODE_FORMAT.matcher(code).matches()) {
            throw new IllegalArgumentException(
                    "code must have 1 to 32 letters, digits or hyphens");
        }
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        if (storeId == null) {
            throw new IllegalArgumentException("storeId is required");
        }
    }

    RegisterScooterCommand toCommand() {
        return new RegisterScooterCommand(code, type, storeId);
    }
}
