package pt.ua.hipereats.application.port.in;

import java.util.List;
import pt.ua.hipereats.domain.account.UserAccount;

/** Inbound port: the accounts of the platform, for the admin. */
public interface ListAccountsQuery {

    List<UserAccount> listAccounts();
}
