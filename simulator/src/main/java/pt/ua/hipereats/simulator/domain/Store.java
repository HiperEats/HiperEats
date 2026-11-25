package pt.ua.hipereats.simulator.domain;

/**
 * A store of the chain and its fixed fleet (BR6, BR13).
 *
 * @param code     short code, used as the prefix of the scooter codes (store "AV" has "AV-01", "AV-02", ...)
 * @param name     store name
 * @param location where the scooters are parked
 * @param standardScooters  number of standard scooters
 * @param longRangeScooters number of long-range scooters
 */
public record Store(String code, String name, GeoPoint location, int standardScooters, int longRangeScooters) {
}
