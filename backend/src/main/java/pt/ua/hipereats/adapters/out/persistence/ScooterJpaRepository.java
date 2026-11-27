package pt.ua.hipereats.adapters.out.persistence;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import pt.ua.hipereats.domain.fleet.ScooterState;

interface ScooterJpaRepository extends JpaRepository<ScooterEntity, Long> {

    /** How long a change of state waits for another change of the same scooter, in milliseconds. */
    String LOCK_TIMEOUT_MS = "3000";

    long countByStoreId(long storeId);

    Optional<ScooterEntity> findByCode(String code);

    /**
     * {@code SELECT ... FOR UPDATE}: the row stays locked until the transaction ends. Waiting for a
     * lock held by another transaction fails after {@link #LOCK_TIMEOUT_MS}.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = LOCK_TIMEOUT_MS))
    @Query("select s from ScooterEntity s where s.code = :code")
    Optional<ScooterEntity> findByCodeForUpdate(@Param("code") String code);

    /** Changes the state only if it is still {@code from}; returns the number of rows changed. */
    @Modifying(clearAutomatically = true)
    @Query("update ScooterEntity s set s.state = :state where s.code = :code and s.state = :from")
    int updateState(@Param("code") String code, @Param("from") ScooterState from,
            @Param("state") ScooterState state);
}
