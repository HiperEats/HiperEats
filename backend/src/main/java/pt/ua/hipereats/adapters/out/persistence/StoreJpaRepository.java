package pt.ua.hipereats.adapters.out.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface StoreJpaRepository extends JpaRepository<StoreEntity, Long> {

    /** {@code SELECT ... FOR UPDATE}: the row stays locked until the transaction ends. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StoreEntity s where s.id = :id")
    Optional<StoreEntity> findByIdForUpdate(@Param("id") long id);
}
