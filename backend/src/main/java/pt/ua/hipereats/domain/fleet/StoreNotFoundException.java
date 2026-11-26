package pt.ua.hipereats.domain.fleet;

/** No store has the given id. */
public class StoreNotFoundException extends RuntimeException {

    public StoreNotFoundException(long storeId) {
        super("Store " + storeId + " does not exist");
    }
}
