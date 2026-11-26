package pt.ua.hipereats.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pt.ua.hipereats.application.port.in.Session;
import pt.ua.hipereats.application.port.out.IssuedToken;
import pt.ua.hipereats.application.port.out.PasswordHasher;
import pt.ua.hipereats.application.port.out.TokenIssuer;
import pt.ua.hipereats.application.port.out.UserAccountRepository;
import pt.ua.hipereats.domain.account.Role;
import pt.ua.hipereats.domain.account.UserAccount;

class AccountServiceTest {

    private static final Instant EXPIRES_AT = Instant.parse("2026-11-26T18:00:00Z");

    /** Fake of the repository port, in memory. */
    private static class FakeAccounts implements UserAccountRepository {
        final List<UserAccount> accounts = new ArrayList<>();

        @Override
        public Optional<UserAccount> findByEmail(String email) {
            return accounts.stream().filter(a -> a.email().equals(email)).findFirst();
        }

        @Override
        public UserAccount save(UserAccount account) {
            UserAccount saved = new UserAccount((long) accounts.size() + 1, account.email(),
                    account.passwordHash(), account.role());
            accounts.add(saved);
            return saved;
        }

        @Override
        public List<UserAccount> findAll() {
            return List.copyOf(accounts);
        }
    }

    /** Fake hasher: the "hash" of a password is the password with a prefix. */
    private static class FakeHasher implements PasswordHasher {
        @Override
        public String hash(String rawPassword) {
            return "hashed:" + rawPassword;
        }

        @Override
        public boolean matches(String rawPassword, String hash) {
            return hash.equals("hashed:" + rawPassword);
        }
    }

    /** Fake token issuer: the token names the account and its role. */
    private static class FakeTokens implements TokenIssuer {
        @Override
        public IssuedToken issue(UserAccount account) {
            return new IssuedToken("token-of-" + account.email() + "-" + account.role(), EXPIRES_AT);
        }
    }

    private final FakeAccounts accounts = new FakeAccounts();
    private final AccountService service = new AccountService(accounts, new FakeHasher(), new FakeTokens());

    private void givenAccount(String email, String password, Role role) {
        accounts.save(UserAccount.newAccount(email, "hashed:" + password, role));
    }

    @Test
    void whenPasswordIsRight_thenASessionWithTheRoleIsOpened() {
        givenAccount("rita@example.test", "Secret-123", Role.COURIER);

        Session session = service.logIn("rita@example.test", "Secret-123");

        assertThat(session.role()).isEqualTo(Role.COURIER);
        assertThat(session.email()).isEqualTo("rita@example.test");
        assertThat(session.accessToken()).isEqualTo("token-of-rita@example.test-COURIER");
        assertThat(session.expiresAt()).isEqualTo(EXPIRES_AT);
    }

    @Test
    void whenPasswordIsWrong_thenLogInIsRefused() {
        givenAccount("rita@example.test", "Secret-123", Role.COURIER);

        assertThatThrownBy(() -> service.logIn("rita@example.test", "wrong-password"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void whenEmailIsUnknown_thenLogInIsRefusedTheSameWay() {
        assertThatThrownBy(() -> service.logIn("nobody@example.test", "Secret-123"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage(new InvalidCredentialsException().getMessage());
    }

    @Test
    void whenEmailIsTypedWithCapitals_thenLogInSucceeds() {
        givenAccount("rita@example.test", "Secret-123", Role.COURIER);

        Session session = service.logIn(" Rita@Example.test", "Secret-123");

        assertThat(session.email()).isEqualTo("rita@example.test");
    }

    @Test
    void whenEmailOrPasswordIsMissing_thenLogInIsRefused() {
        givenAccount("rita@example.test", "Secret-123", Role.COURIER);

        assertThatThrownBy(() -> service.logIn(null, "Secret-123")).isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> service.logIn("rita@example.test", null))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void whenAccountsAreListed_thenEveryAccountIsReturned() {
        givenAccount("rita@example.test", "Secret-123", Role.COURIER);
        givenAccount("sofia@example.test", "Secret-456", Role.ADMIN);

        assertThat(service.listAccounts()).extracting(UserAccount::email)
                .containsExactly("rita@example.test", "sofia@example.test");
    }
}
