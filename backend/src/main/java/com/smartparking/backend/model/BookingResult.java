package com.smartparking.backend.model;

public class BookingResult {
    public String slotId;
    public String zone;
    public double lat;
    public double lng;
    public int pricePerHour;
    public double distanceMeters;
    public String message;

    public BookingResult(String slotId, String zone, double lat, double lng,
                         int pricePerHour, double distanceMeters, String message) {
        this.slotId = slotId;
        this.zone = zone;
        this.lat = lat;
        this.lng = lng;
        this.pricePerHour = pricePerHour;
        this.distanceMeters = distanceMeters;
        this.message = message;
    }
}