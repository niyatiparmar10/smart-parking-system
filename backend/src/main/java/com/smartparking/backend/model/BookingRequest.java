package com.smartparking.backend.model;

public class BookingRequest {
    public double lat;
    public double lng;
    public int startMin;   // time as minutes from midnight e.g. 11:30 = 690
    public int endMin;
    public String vehicleType; // "NORMAL", "EV", "DISABLED"
}
