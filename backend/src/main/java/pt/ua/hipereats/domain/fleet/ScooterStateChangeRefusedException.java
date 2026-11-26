package pt.ua.hipereats.domain.fleet;

/** The scooter lifecycle (Product Specification, 3.3) does not allow the change from its current state. */
public class ScooterStateChangeRefusedException extends RuntimeException {

    public ScooterStateChangeRefusedException(String code, ScooterState current,
            ScooterState target) {
        super("Scooter " + code + " cannot go from " + current + " to " + target);
    }
}
