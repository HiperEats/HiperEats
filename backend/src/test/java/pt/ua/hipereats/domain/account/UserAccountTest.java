package pt.ua.hipereats.domain.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UserAccountTest {

    private static final String HASH = "{bcrypt}$2a$10$abcdefghijklmnopqrstuv";

    @Test
    void whenEmailHasCapitalsAndSpaces_thenItIsStoredTrimmedAndInLowerCase() {
        UserAccount account = UserAccount.newAccount("  Rita@Example.TEST ", HASH, Role.COURIER);

        assertThat(account.email()).isEqualTo("rita@example.test");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "rita", "rita@", "@example.test"})
    void whenEmailIsNotAnEmail_thenAccountIsRefused(String email) {
        assertThatIllegalArgumentException().isThrownBy(() -> UserAccount.newAccount(email, HASH, Role.COURIER));
    }

    @Test
    void whenPasswordHashOrRoleIsMissing_thenAccountIsRefused() {
        assertThatIllegalArgumentException().isThrownBy(
                () -> UserAccount.newAccount("rita@example.test", " ", Role.COURIER));
        assertThatIllegalArgumentException().isThrownBy(
                () -> UserAccount.newAccount("rita@example.test", HASH, null));
    }

    @Test
    void whenAccountIsCreated_thenItIsActive() {
        assertThat(UserAccount.newAccount("rita@example.test", HASH, Role.COURIER).active()).isTrue();
    }

    @Test
    void whenAccountIsDeactivated_thenOnlyItsStatusChanges() {
        UserAccount rita = new UserAccount(7L, "rita@example.test", HASH, Role.COURIER, true);

        UserAccount deactivated = rita.withActive(false);

        assertThat(deactivated.active()).isFalse();
        assertThat(deactivated).usingRecursiveComparison().ignoringFields("active").isEqualTo(rita);
    }

    @Test
    void whenAccountIsPrinted_thenThePasswordHashIsHidden() {
        UserAccount account = UserAccount.newAccount("rita@example.test", HASH, Role.COURIER);

        assertThat(account.toString()).doesNotContain(HASH).contains("rita@example.test");
    }
}
