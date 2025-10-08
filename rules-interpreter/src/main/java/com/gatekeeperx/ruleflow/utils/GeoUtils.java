package com.gatekeeperx.ruleflow.utils;

/**
 * Utility methods for geohash encoding/decoding and location calculations.
 */
public class GeoUtils {
    /**
     * Encodes latitude and longitude into a geohash string.
     * @param lat Latitude
     * @param lon Longitude
     * @param precision Number of characters in geohash (optional, default 12)
     * @return Geohash string
     */
    public static String encodeGeohash(double lat, double lon, int precision) {
        // Simple geohash implementation (base32)
        final String BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";
        double[] latInterval = {-90.0, 90.0};
        double[] lonInterval = {-180.0, 180.0};
        StringBuilder geohash = new StringBuilder();
        boolean isEven = true;
        int bit = 0, ch = 0;
        int geohashLength = precision > 0 ? precision : 12;
        while (geohash.length() < geohashLength) {
            double mid;
            if (isEven) {
                mid = (lonInterval[0] + lonInterval[1]) / 2D;
                if (lon > mid) {
                    ch |= 1 << (4 - bit);
                    lonInterval[0] = mid;
                } else {
                    lonInterval[1] = mid;
                }
            } else {
                mid = (latInterval[0] + latInterval[1]) / 2D;
                if (lat > mid) {
                    ch |= 1 << (4 - bit);
                    latInterval[0] = mid;
                } else {
                    latInterval[1] = mid;
                }
            }
            isEven = !isEven;
            if (bit < 4) {
                bit++;
            } else {
                geohash.append(BASE32.charAt(ch));
                bit = 0;
                ch = 0;
            }
        }
        return geohash.toString();
    }

    /**
     * Decodes a geohash string into latitude and longitude.
     * @param geohash Geohash string
     * @return double[]{lat, lon}
     */
    public static double[] decodeGeohash(String geohash) {
        final String BASE32 = "0123456789bcdefghjkmnpqrstuvwxyz";
        double[] latInterval = {-90.0, 90.0};
        double[] lonInterval = {-180.0, 180.0};
        boolean isEven = true;
        for (int i = 0; i < geohash.length(); i++) {
            int cd = BASE32.indexOf(Character.toLowerCase(geohash.charAt(i)));
            for (int mask = 16; mask != 0; mask >>= 1) {
                if (isEven) {
                    if ((cd & mask) != 0) {
                        lonInterval[0] = (lonInterval[0] + lonInterval[1]) / 2D;
                    } else {
                        lonInterval[1] = (lonInterval[0] + lonInterval[1]) / 2D;
                    }
                } else {
                    if ((cd & mask) != 0) {
                        latInterval[0] = (latInterval[0] + latInterval[1]) / 2D;
                    } else {
                        latInterval[1] = (latInterval[0] + latInterval[1]) / 2D;
                    }
                }
                isEven = !isEven;
            }
        }
        double lat = (latInterval[0] + latInterval[1]) / 2D;
        double lon = (lonInterval[0] + lonInterval[1]) / 2D;
        return new double[]{lat, lon};
    }

    /**
     * Calculates the distance in kilometers between two lat/lon points.
     * @param lat1 Latitude of point 1
     * @param lon1 Longitude of point 1
     * @param lat2 Latitude of point 2
     * @param lon2 Longitude of point 2
     * @return Distance in kilometers
     */
    public static double distance(double lat1, double lon1, double lat2, double lon2) {
        // Haversine formula
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Calculates the distance in kilometers between two geohashes.
     * @param geohash1 Geohash string 1
     * @param geohash2 Geohash string 2
     * @return Distance in kilometers
     */
    public static double distance(String geohash1, String geohash2) {
        double[] coords1 = decodeGeohash(geohash1);
        double[] coords2 = decodeGeohash(geohash2);
        return distance(coords1[0], coords1[1], coords2[0], coords2[1]);
    }

    /**
     * Checks if two points are within a given radius (in kilometers).
     * @param lat1 Latitude of point 1
     * @param lon1 Longitude of point 1
     * @param lat2 Latitude of point 2
     * @param lon2 Longitude of point 2
     * @param radiusKm Radius in kilometers
     * @return true if within radius, false otherwise
     */
    public static boolean withinRadius(double lat1, double lon1, double lat2, double lon2, double radiusKm) {
        return distance(lat1, lon1, lat2, lon2) <= radiusKm;
    }
} 