package pt.ua.hipereats.application.port.out;

/** Outbound port: one-way hashing of passwords, so the platform never stores a password. */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String hash);
}
