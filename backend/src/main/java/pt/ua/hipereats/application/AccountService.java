package pt.ua.hipereats.application;

import java.util.List;
import org.springframework.stereotype.Service;
import pt.ua.hipereats.application.port.in.AccountStatusQuery;
import pt.ua.hipereats.application.port.in.ChangeAccountStatusUseCase;
import pt.ua.hipereats.application.port.in.ListAccountsQuery;
import pt.ua.hipereats.application.port.in.LogInUseCase;
import pt.ua.hipereats.application.port.in.Session;
import pt.ua.hipereats.application.port.out.IssuedToken;
import pt.ua.hipereats.application.port.out.PasswordHasher;
import pt.ua.hipereats.application.port.out.TokenIssuer;
import pt.ua.hipereats.application.port.out.UserAccountRepository;
import pt.ua.hipereats.domain.account.UserAccount;

/** Use cases of the accounts: logging in (HE-28), listing them, and deactivating them (HE-36). */
@Service
public class AccountService
        implements LogInUseCase, ListAccountsQuery, ChangeAccountStatusUseCase, AccountStatusQuery {

    private final UserAccountRepository accounts;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokenIssuer;

    public AccountService(UserAccountRepository accounts, PasswordHasher passwordHasher, TokenIssuer tokenIssuer) {
        this.accounts = accounts;
        this.passwordHasher = passwordHasher;
        this.tokenIssuer = tokenIssuer;
    }

    @Override
    public Session logIn(String email, String password) {
        if (password == null) {
            throw new InvalidCredentialsException();
        }
        UserAccount account = accounts.findByEmail(UserAccount.normalizeEmail(email))
                .filter(found -> passwordHasher.matches(password, found.passwordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        if (!account.active()) {
            throw new AccountDeactivatedException();
        }
        IssuedToken token = tokenIssuer.issue(account);
        return new Session(token.value(), token.expiresAt(), account.email(), account.role());
    }

    @Override
    public List<UserAccount> listAccounts() {
        return accounts.findAll();
    }

    @Override
    public UserAccount setActive(long accountId, boolean active, String requestedBy) {
        UserAccount account = accounts.findById(accountId).orElseThrow(() -> new AccountNotFoundException(accountId));
        if (!active && account.email().equals(UserAccount.normalizeEmail(requestedBy))) {
            throw new CannotDeactivateOwnAccountException();
        }
        return accounts.save(account.withActive(active));
    }

    @Override
    public boolean isActive(String email) {
        return accounts.findByEmail(UserAccount.normalizeEmail(email)).map(UserAccount::active).orElse(false);
    }
}
