package pt.ua.hipereats.application.port.out;

import java.util.List;
import java.util.Optional;
import pt.ua.hipereats.domain.account.UserAccount;

/** Outbound port: where the accounts are kept. */
public interface UserAccountRepository {

    /** @param email already normalized ({@link UserAccount#normalizeEmail}) */
    Optional<UserAccount> findByEmail(String email);

    Optional<UserAccount> findById(long id);

    /** Saves a new account, or the changes of an existing one (same id), and returns it with its id. */
    UserAccount save(UserAccount account);

    /** Every account, ordered by email. */
    List<UserAccount> findAll();
}
