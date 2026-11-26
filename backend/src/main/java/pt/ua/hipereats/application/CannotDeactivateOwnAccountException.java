package pt.ua.hipereats.application;

/**
 * An admin tried to deactivate their own account. Refused so that the platform is never left
 * without anyone who can manage the accounts (assumption: the specification does not say it).
 */
public class CannotDeactivateOwnAccountException extends RuntimeException {

    public CannotDeactivateOwnAccountException() {
        super("You cannot deactivate your own account");
    }
}
