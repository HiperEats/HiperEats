package pt.ua.hipereats.adapters.in.web;

import java.time.Clock;
import java.time.OffsetDateTime;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.ua.hipereats.application.port.in.LogInUseCase;
import pt.ua.hipereats.application.port.in.Session;
import pt.ua.hipereats.domain.account.Role;

/**
 * Logging in (HE-28): {@code POST /api/v1/sessions} with the email and password gives an access
 * token. The session is not stored: the token itself is the proof, until it expires.
 */
@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private final LogInUseCase logIn;
    private final Clock clock;

    public SessionController(LogInUseCase logIn, Clock clock) {
        this.logIn = logIn;
        this.clock = clock;
    }

    @PostMapping
    public SessionResponse logIn(@RequestBody LogInRequest request) {
        Session session = logIn.logIn(request.email(), request.password());
        return new SessionResponse(session.accessToken(), "Bearer",
                OffsetDateTime.ofInstant(session.expiresAt(), clock.getZone()), session.email(), session.role());
    }

    public record LogInRequest(String email, String password) {

        /** Never prints the password. */
        @Override
        public String toString() {
            return "LogInRequest[email=" + email + "]";
        }
    }

    public record SessionResponse(String accessToken, String tokenType, OffsetDateTime expiresAt, String email,
                                  Role role) {
    }
}
