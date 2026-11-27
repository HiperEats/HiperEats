package pt.ua.hipereats.application.port.in;

/** Inbound port: a person logs in with their email and password (HE-28). */
public interface LogInUseCase {

    /**
     * Checks the email and password and opens a session.
     *
     * @throws pt.ua.hipereats.application.InvalidCredentialsException when the email is unknown or
     *         the password is wrong (the caller cannot tell which, on purpose)
     */
    Session logIn(String email, String password);
}
