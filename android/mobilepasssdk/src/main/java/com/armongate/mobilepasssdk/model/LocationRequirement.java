package com.armongate.mobilepasssdk.model;

/**
 * Location requirements for verification
 * Provided when remote access requires location check
 */
public class LocationRequirement {
    private final double latitude;
    private final double longitude;
    private final int radius;

    public LocationRequirement(double latitude, double longitude, int radius) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.radius = radius;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public int getRadius() {
        return radius;
    }
}

