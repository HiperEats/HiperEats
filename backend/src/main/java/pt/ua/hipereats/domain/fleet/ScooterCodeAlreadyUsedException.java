package pt.ua.hipereats.domain.fleet;

/** Another scooter already has the given code. */
public class ScooterCodeAlreadyUsedException extends RuntimeException {

    public ScooterCodeAlreadyUsedException(String code) {
        super("A scooter with code " + code + " already exists");
    }
}
