package pt.ua.hipereats.domain.fleet;

/** No scooter has the given code. */
public class ScooterNotFoundException extends RuntimeException {

    public ScooterNotFoundException(String code) {
        super("Scooter " + code + " does not exist");
    }
}
