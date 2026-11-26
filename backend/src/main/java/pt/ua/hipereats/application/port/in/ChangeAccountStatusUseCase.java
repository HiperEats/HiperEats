package pt.ua.hipereats.application.port.in;

import pt.ua.hipereats.domain.account.UserAccount;

/** Inbound port: an admin deactivates an account, or activates it again (HE-36). */
public interface ChangeAccountStatusUseCase {

    /**
     * @param requestedBy email of the admin who asks
     * @throws pt.ua.hipereats.application.AccountNotFoundException if there is no such account
     * @throws pt.ua.hipereats.application.CannotDeactivateOwnAccountException if the admin tries to
     *         deactivate their own account
     */
    UserAccount setActive(long accountId, boolean active, String requestedBy);
}
