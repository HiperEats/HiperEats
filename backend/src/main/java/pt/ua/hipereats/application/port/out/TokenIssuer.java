package pt.ua.hipereats.application.port.out;

import pt.ua.hipereats.domain.account.UserAccount;

/** Outbound port: issues the signed access token of a logged-in account. */
public interface TokenIssuer {

    IssuedToken issue(UserAccount account);
}
