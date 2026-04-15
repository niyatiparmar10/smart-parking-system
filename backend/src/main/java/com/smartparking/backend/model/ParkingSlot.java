package com.smartparking.backend.model;

import java.util.ArrayList;
import java.util.List;

public class ParkingSlot {

    public String slotId;
    public double lat;
    public double lng;
    public String zone;
    public String type;
    public int pricePerHour;
    public boolean isOccupied;
    public List<List<Integer>> bookings;  // changed from int[][] for JSON parsing

    public ParkingSlot() {
        this.bookings = new ArrayList<>();
    }
}