package pt.ua.hipereats.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A row of {@code stores}, with only the columns the fleet needs. */
@Entity
@Table(name = "stores")
class StoreEntity {

    @Id
    private Long id;

    @Column(name = "fleet_capacity", nullable = false)
    private int fleetCapacity;

    protected StoreEntity() {
        // for JPA
    }

    Long getId() {
        return id;
    }

    int getFleetCapacity() {
        return fleetCapacity;
    }
}
