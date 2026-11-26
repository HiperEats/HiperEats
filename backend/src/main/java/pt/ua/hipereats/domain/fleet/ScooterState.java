package pt.ua.hipereats.domain.fleet;

/** States of the scooter lifecycle (Product Specification, 3.3). */
public enum ScooterState {
    INACTIVE,
    AVAILABLE,
    RESERVED,
    IN_USE,
    MAINTENANCE,
    RETIRED
}
