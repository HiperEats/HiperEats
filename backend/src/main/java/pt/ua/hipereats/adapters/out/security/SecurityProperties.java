package pt.ua.hipereats.adapters.out.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Access tokens. The signing secret comes from the environment variable {@code JWT_SECRET} (the
 * {@code .env} file in Docker Compose); it is never written in the repository.
 *
 * @param jwtSecret     key that signs and checks the tokens (HMAC-SHA256), at least 32 bytes
 * @param tokenValidity how long a token is accepted after logging in
 */
@ConfigurationProperties("hipereats.security")
public record SecurityProperties(String jwtSecret, @DefaultValue("8h") Duration tokenValidity) {

    /** HMAC-SHA256 needs a key of at least 256 bits. */
    static final int MIN_SECRET_BYTES = 32;

    public SecurityProperties {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("Set JWT_SECRET to a random value of at least "
                    + MIN_SECRET_BYTES + " characters (see .env.example)");
        }
        if (tokenValidity == null || !tokenValidity.isPositive()) {
            throw new IllegalArgumentException("hipereats.security.token-validity must be positive");
        }
    }
}
