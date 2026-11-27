package pt.ua.hipereats.adapters.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pt.ua.hipereats.domain.fleet.ScooterState;

interface ScooterJpaRepository extends JpaRepository<ScooterEntity, Long> {

    long countByStoreId(long storeId);

    Optional<ScooterEntity> findByCode(String code);

    List<ScooterEntity> findByStoreIdAndState(long storeId, ScooterState state);
}
