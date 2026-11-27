package pt.ua.hipereats.application.port.in;

import java.math.BigDecimal;
import java.util.List;

/** Inbound port: the scooters of a store that a courier can take for a delivery (HE-31). */
public interface AvailableScootersQuery {

    /**
     * The available scooters of the store whose battery covers a delivery at this distance (BR1),
     * ordered by code. A scooter that has not reported its battery yet is not listed.
     */
    List<AvailableScooter> availableFor(long storeId, BigDecimal deliveryDistanceKm);
}
