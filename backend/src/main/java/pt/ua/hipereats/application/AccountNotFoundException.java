package pt.ua.hipereats.application;

/** No account has this id. */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(long accountId) {
        super("No account with id " + accountId);
    }
}
