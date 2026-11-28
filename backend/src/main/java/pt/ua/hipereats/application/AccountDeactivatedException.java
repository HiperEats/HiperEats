package pt.ua.hipereats.application;

/** The email and password are right, but an admin deactivated the account (HE-36). */
public class AccountDeactivatedException extends RuntimeException {

    public AccountDeactivatedException() {
        super("This account has been deactivated");
    }
}
