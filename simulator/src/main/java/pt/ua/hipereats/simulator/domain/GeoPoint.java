package pt.ua.hipereats.simulator.domain;

/** A position on Earth, in decimal degrees (WGS84, as GPS reports it). */
public record GeoPoint(double latitude, double longitude) {

    private static final double EARTH_RADIUS_KM = 6371.0;

    /** Great-circle distance to another point (haversine formula), in km. */
    public double distanceKm(GeoPoint other) {
        double lat1 = Math.toRadians(latitude);
        double lat2 = Math.toRadians(other.latitude);
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(other.longitude - longitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(a));
    }

    /** The point reached by travelling {@code km} from here in the direction {@code bearingDegrees} (0 = north). */
    public GeoPoint destination(double bearingDegrees, double km) {
        double angular = km / EARTH_RADIUS_KM;
        double bearing = Math.toRadians(bearingDegrees);
        double lat1 = Math.toRadians(latitude);
        double lon1 = Math.toRadians(longitude);
        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(angular)
                + Math.cos(lat1) * Math.sin(angular) * Math.cos(bearing));
        double lon2 = lon1 + Math.atan2(Math.sin(bearing) * Math.sin(angular) * Math.cos(lat1),
                Math.cos(angular) - Math.sin(lat1) * Math.sin(lat2));
        return new GeoPoint(Math.toDegrees(lat2), Math.toDegrees(lon2));
    }

    /**
     * The point at {@code fraction} (0 to 1) of the straight line from here to {@code to}.
     * Linear in degrees, which is accurate enough for the few km of a delivery.
     */
    public GeoPoint towards(GeoPoint to, double fraction) {
        return new GeoPoint(
                latitude + (to.latitude - latitude) * fraction,
                longitude + (to.longitude - longitude) * fraction);
    }
}
