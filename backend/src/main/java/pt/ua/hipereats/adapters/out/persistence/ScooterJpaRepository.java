package pt.ua.hipereats.adapters.out.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface ScooterJpaRepository extends JpaRepository<ScooterEntity, Long> {

    long countByStoreId(long storeId);

    Optional<ScooterEntity> findByCode(String code);
}
