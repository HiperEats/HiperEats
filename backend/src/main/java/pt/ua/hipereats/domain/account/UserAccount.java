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
 * @param active       false once an admin deactivates the account: it can no longer log in (HE-36)
 */
public record UserAccount(Long id, String email, String passwordHash, Role role, boolean active) {

    /** Something before and after one "@"; the real check is that the person receives mail there. */
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+");

    public UserAccount {
        email = normalizeEmail(email);
        if (!EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("Not an email address: " + email);
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash is required");
        }
        if (role == null) {
            throw new IllegalArgumentException("role is required");
        }
    }

    /** A new account, not saved yet. It starts active. */
    public static UserAccount newAccount(String email, String passwordHash, Role role) {
        return new UserAccount(null, email, passwordHash, role, true);
    }

    /** The same account, activated or deactivated. */
    public UserAccount withActive(boolean newActive) {
        return new UserAccount(id, email, passwordHash, role, newActive);
    }

    /** Trims and lower-cases an email, as it is stored; null becomes "". */
    public static String normalizeEmail(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }

    /** Never prints the password hash (it would end up in logs). */
    @Override
    public String toString() {
        return "UserAccount[id=" + id + ", email=" + email + ", role=" + role + ", active=" + active + "]";
    }
}
