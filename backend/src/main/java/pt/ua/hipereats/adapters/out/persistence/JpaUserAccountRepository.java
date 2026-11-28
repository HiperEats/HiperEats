package pt.ua.hipereats.adapters.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import pt.ua.hipereats.application.port.out.UserAccountRepository;
import pt.ua.hipereats.domain.account.UserAccount;

/** Persistence adapter of the accounts, on PostgreSQL through JPA. */
@Repository
public class JpaUserAccountRepository implements UserAccountRepository {

    private final UserAccountJpaRepository jpa;

    public JpaUserAccountRepository(UserAccountJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return jpa.findByEmail(email).map(UserAccountEntity::toDomain);
    }

    @Override
    public Optional<UserAccount> findById(long id) {
        return jpa.findById(id).map(UserAccountEntity::toDomain);
    }

    @Override
    public UserAccount save(UserAccount account) {
        // saveAndFlush: a repeated email fails here, not later at the end of the transaction.
        return jpa.saveAndFlush(UserAccountEntity.from(account)).toDomain();
    }

    @Override
    public List<UserAccount> findAll() {
        return jpa.findAllByOrderByEmail().stream().map(UserAccountEntity::toDomain).toList();
    }
}
