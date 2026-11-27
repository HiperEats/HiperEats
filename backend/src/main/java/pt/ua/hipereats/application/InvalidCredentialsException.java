package pt.ua.hipereats.application;

/**
 * Wrong email or password. The message is the same in both cases, so nobody can find out which
 * emails have an account by trying to log in.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Wrong email or password");
    }
}
