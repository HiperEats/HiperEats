package pt.ua.hipereats;

import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Arranges stores and scooters directly in the database, for the integration and acceptance tests.
 *
 * <p>All tests share one database, so every store is new and every scooter code is unique.
 *
 * <p>It writes SQL directly, so it fixes the schema that the HE-29 migration must create:
 * {@code stores(name, fleet_capacity)} returning a generated {@code id}, and
 * {@code scooters(code, type, state, store_id)}.
 */
public final class FleetTestData {

    private final JdbcClient jdbc;

    public FleetTestData(DataSource dataSource) {
        this.jdbc = JdbcClient.create(dataSource);
    }

    /** A scooter code that no other test uses, within the code format ([A-Za-z0-9-]{1,32}). */
    public static String uniqueCode() {
        return "T-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Creates a store with the given fleet capacity and returns its id. */
    public long createStore(int fleetCapacity) {
        return jdbc.sql("INSERT INTO stores (name, fleet_capacity) VALUES (:name, :capacity)"
                        + " RETURNING id")
                .param("name", "Test store " + uniqueCode())
                .param("capacity", fleetCapacity)
                .query(Long.class)
                .single();
    }

    /** Registers the given number of standard scooters at the store, bypassing the API. */
    public void addScooters(long storeId, int count) {
        for (int i = 0; i < count; i++) {
            jdbc.sql("INSERT INTO scooters (code, type, state, store_id)"
                            + " VALUES (:code, 'STANDARD', 'INACTIVE', :storeId)")
                    .param("code", uniqueCode())
                    .param("storeId", storeId)
                    .update();
        }
    }

    public int countScootersWithCode(String code) {
        return jdbc.sql("SELECT count(*) FROM scooters WHERE code = :code")
                .param("code", code)
                .query(Integer.class)
                .single();
    }

    public int countScooters(long storeId) {
        return jdbc.sql("SELECT count(*) FROM scooters WHERE store_id = :storeId")
                .param("storeId", storeId)
                .query(Integer.class)
                .single();
    }
}
