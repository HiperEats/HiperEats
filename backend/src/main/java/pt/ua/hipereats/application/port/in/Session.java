package pt.ua.hipereats.application.port.in;

import java.time.Instant;
import pt.ua.hipereats.domain.account.Role;

/**
 * The result of logging in: the access token the client sends in every request, until it expires.
 */
public record Session(String accessToken, Instant expiresAt, String email, Role role) {
}
