package pt.ua.hipereats.adapters.out.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.ua.hipereats.domain.fleet.ScooterState;

interface ScooterJpaRepository extends JpaRepository<ScooterEntity, Long> {

    long countByStoreId(long storeId);

    Optional<ScooterEntity> findByCode(String code);

    /** {@code SELECT ... FOR UPDATE}: the row stays locked until the transaction ends. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ScooterEntity s where s.code = :code")
    Optional<ScooterEntity> findByCodeForUpdate(@Param("code") String code);

    @Modifying(clearAutomatically = true)
    @Query("update ScooterEntity s set s.state = :state where s.code = :code")
    int updateState(@Param("code") String code, @Param("state") ScooterState state);
}
