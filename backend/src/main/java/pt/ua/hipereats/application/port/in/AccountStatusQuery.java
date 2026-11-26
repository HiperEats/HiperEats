package pt.ua.hipereats.application.port.in;

/**
 * Inbound port: whether an account may still use the platform. Checked on every request, so a
 * deactivated person loses access at once, not when their token expires.
 */
public interface AccountStatusQuery {

    /** False for a deactivated account and for an email with no account. */
    boolean isActive(String email);
}
