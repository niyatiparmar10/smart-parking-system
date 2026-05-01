package com.smartparking.backend.ds;

import java.util.*;

public class TrafficTracker {
    private static final long WINDOW_MS = 10 * 60 * 1000; // 10 minutes
    private static final int THRESHOLD = 10; // bookings

    private Map<String, LinkedList<Long>> zoneTraffic = new HashMap<>();

    public TrafficTracker(List<String> zones) {
        for (String zone : zones) {
            zoneTraffic.put(zone, new LinkedList<>());
        }
    }

    public synchronized void recordBooking(String zone) {
        if (!zoneTraffic.containsKey(zone)) return;
        zoneTraffic.get(zone).addLast(System.currentTimeMillis());
    }

    public synchronized boolean isTrafficCongested(String zone) {
        if (!zoneTraffic.containsKey(zone)) return false;
        
        LinkedList<Long> queue = zoneTraffic.get(zone);
        long now = System.currentTimeMillis();
        
        // Clean up old bookings
        while (!queue.isEmpty() && now - queue.getFirst() > WINDOW_MS) {
            queue.removeFirst();
        }
        
        return queue.size() >= THRESHOLD;
    }
    
    public synchronized void resetAll() {
        for (LinkedList<Long> queue : zoneTraffic.values()) {
            queue.clear();
        }
    }

    // For Demo: Immediately fill the zone with 10 fake recent timestamps to cause a jam
    public synchronized void congestZoneForDemo(String zone) {
        if (!zoneTraffic.containsKey(zone)) return;
        LinkedList<Long> queue = zoneTraffic.get(zone);
        long now = System.currentTimeMillis();
        for (int i = 0; i < THRESHOLD; i++) {
            queue.addLast(now); 
        }
    }
}
