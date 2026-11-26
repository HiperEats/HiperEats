package pt.ua.hipereats.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import pt.ua.hipereats.domain.fleet.Scooter;
import pt.ua.hipereats.domain.fleet.ScooterState;
import pt.ua.hipereats.domain.fleet.ScooterType;

/** A row of {@code scooters}. */
@Entity
@Table(name = "scooters")
class ScooterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScooterType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScooterState state;

    @Column(name = "store_id", nullable = false)
    private long storeId;

    protected ScooterEntity() {
        // for JPA
    }

    static ScooterEntity from(Scooter scooter) {
        ScooterEntity entity = new ScooterEntity();
        entity.code = scooter.code();
        entity.type = scooter.type();
        entity.state = scooter.state();
        entity.storeId = scooter.storeId();
        return entity;
    }

    Scooter toScooter() {
        return new Scooter(code, type, state, storeId);
    }
}
