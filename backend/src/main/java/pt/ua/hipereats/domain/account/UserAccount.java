package pt.ua.hipereats.domain.account;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A person's account: they log in with the email and password and get the access of their role.
 * The email is the login name, stored trimmed and in lower case, so "Rita@Example.test" and
 * "rita@example.test" are the same account.
 *
 * @param id           null until the account is saved
 * @param passwordHash the password hashed by the password hasher; never the password itself
 */
public record UserAccount(Long id, String email, String passwordHash, Role role) {

    /** Something before and after one "@"; the real check is that the person receives mail there. */
    private static final Pattern EMAIL_FORMAT = Pattern.compile("[^@\\s]+@[^@\\s]+");

    public UserAccount {
        email = normalizeEmail(email);
        if (!EMAIL_FORMAT.matcher(email).matches()) {
            throw new IllegalArgumentException("Not an email address: " + email);
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash is required");
        }
        if (role == null) {
            throw new IllegalArgumentException("role is required");
        }
    }

    /** A new account, not saved yet. */
    public static UserAccount newAccount(String email, String passwordHash, Role role) {
        return new UserAccount(null, email, passwordHash, role);
    }

    /** Trims and lower-cases an email, as it is stored; null becomes "". */
    public static String normalizeEmail(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }

    /** Never prints the password hash (it would end up in logs). */
    @Override
    public String toString() {
        return "UserAccount[id=" + id + ", email=" + email + ", role=" + role + "]";
    }
}
