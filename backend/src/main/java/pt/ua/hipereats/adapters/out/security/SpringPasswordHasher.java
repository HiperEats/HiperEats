package pt.ua.hipereats.adapters.out.security;

import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pt.ua.hipereats.application.port.out.PasswordHasher;

/**
 * Hashes passwords with bcrypt. The hash starts with "{bcrypt}", so a stronger algorithm can be
 * adopted later without breaking the hashes already stored.
 */
@Component
public class SpringPasswordHasher implements PasswordHasher {

    private final PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        return encoder.matches(rawPassword, hash);
    }
}
