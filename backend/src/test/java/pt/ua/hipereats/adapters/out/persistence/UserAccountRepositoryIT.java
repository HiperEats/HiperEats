package pt.ua.hipereats.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import pt.ua.hipereats.IntegrationTest;
import pt.ua.hipereats.application.port.out.UserAccountRepository;
import pt.ua.hipereats.domain.account.Role;
import pt.ua.hipereats.domain.account.UserAccount;

/** The accounts table on the real PostgreSQL, created by the Flyway migration. */
@IntegrationTest
class UserAccountRepositoryIT {

    @Autowired
    private UserAccountRepository accounts;

    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void emptyTheTable() {
        jdbc.sql("DELETE FROM user_accounts").update();
    }

    @Test
    void whenAccountIsSaved_thenItIsFoundByEmailWithItsRole() {
        UserAccount saved = accounts.save(UserAccount.newAccount("rita@example.test", "{noop}x", Role.COURIER));

        assertThat(saved.id()).isNotNull();
        assertThat(accounts.findByEmail("rita@example.test")).get()
                .extracting(UserAccount::role).isEqualTo(Role.COURIER);
    }

    @Test
    void whenEmailIsAlreadyUsed_thenTheDatabaseRefusesTheSecondAccount() {
        accounts.save(UserAccount.newAccount("rita@example.test", "{noop}x", Role.COURIER));

        assertThatThrownBy(() -> accounts.save(UserAccount.newAccount("rita@example.test", "{noop}y", Role.ADMIN)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void whenAccountIsDeactivated_thenTheChangeIsStored() {
        UserAccount rita = accounts.save(UserAccount.newAccount("rita@example.test", "{noop}x", Role.COURIER));

        accounts.save(rita.withActive(false));

        assertThat(accounts.findById(rita.id())).get().extracting(UserAccount::active).isEqualTo(false);
    }

    @Test
    void whenEmailIsUnknown_thenNothingIsFound() {
        assertThat(accounts.findByEmail("nobody@example.test")).isEmpty();
    }
}
