package pt.ua.hipereats.adapters.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository of the entity; used only by {@link JpaUserAccountRepository}. */
interface UserAccountJpaRepository extends JpaRepository<UserAccountEntity, Long> {

    Optional<UserAccountEntity> findByEmail(String email);

    List<UserAccountEntity> findAllByOrderByEmail();
}
