package pt.ua.hipereats.application.port.out;

import java.time.Instant;

/** An access token and the moment it stops being accepted. */
public record IssuedToken(String value, Instant expiresAt) {
}
