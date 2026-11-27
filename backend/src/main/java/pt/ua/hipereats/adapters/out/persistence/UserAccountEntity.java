package pt.ua.hipereats.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import pt.ua.hipereats.domain.account.Role;
import pt.ua.hipereats.domain.account.UserAccount;

/** JPA mapping of the table {@code user_accounts}; never leaves the persistence adapter. */
@Entity
@Table(name = "user_accounts")
class UserAccountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    protected UserAccountEntity() {
        // For JPA.
    }

    static UserAccountEntity from(UserAccount account) {
        UserAccountEntity entity = new UserAccountEntity();
        entity.id = account.id();
        entity.email = account.email();
        entity.passwordHash = account.passwordHash();
        entity.role = account.role();
        return entity;
    }

    UserAccount toDomain() {
        return new UserAccount(id, email, passwordHash, role);
    }
}
