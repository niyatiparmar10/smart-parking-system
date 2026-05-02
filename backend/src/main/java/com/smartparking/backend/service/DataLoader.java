package com.smartparking.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartparking.backend.ds.*;
import com.smartparking.backend.model.ParkingSlot;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.io.InputStream;
import java.util.*;

@Component
public class DataLoader {

    // --- All Data Structures ---
    public TrafficTracker trafficTracker;
    public SegmentTree segmentTree;
    public IntervalTree intervalTree   = new IntervalTree();
    public SkipList skipList           = new SkipList();
    public Trie trie                   = new Trie();

    // Master list of all slots + fast lookup by ID
    public List<ParkingSlot> allSlots       = new ArrayList<>();
    public Map<String, ParkingSlot> slotMap = new HashMap<>();

    // Zone metadata: how many total slots per zone
    public Map<String, Integer> zoneTotalSlots = new HashMap<>();

    // Fixed zone list (matches Graph.java)
    private static final List<String> ZONES = Arrays.asList(
        "Zone A", "Zone B", "Zone C", "Zone D", "Zone E", "Zone F"
    );

    @PostConstruct  // Spring calls this automatically after app starts
    public void load() {
        System.out.println("=== DataLoader: Starting DS initialization ===");

        try {
            // Step 1: Read slots.json from resources folder
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = getClass()
                .getClassLoader()
                .getResourceAsStream("slots.json");

            ParkingSlot[] slotsArray = mapper.readValue(is, ParkingSlot[].class);
            allSlots = Arrays.asList(slotsArray);

            System.out.println("Loaded " + allSlots.size() + " slots from slots.json");

            // Step 2: Build HashMap (O(1) slot lookup by ID)
            for (ParkingSlot slot : allSlots) {
                slotMap.put(slot.slotId, slot);
            }

            // Step 3: Count total slots per zone
            for (ParkingSlot slot : allSlots) {
                zoneTotalSlots.merge(slot.zone, 1, Integer::sum);
            }

            // Step 4: Build Segment Tree (needs zone list first)
            segmentTree = new SegmentTree(ZONES);
            for (String zone : ZONES) {
                long freeCount = allSlots.stream()
                    .filter(s -> s.zone.equals(zone) && !s.isOccupied)
                    .count();
                segmentTree.setZoneCount(zone, (int) freeCount);
            }
            segmentTree.build();
            System.out.println("SegmentTree built. Total free slots: "
                + segmentTree.getTotalFree());

            // Step 5: Build TrafficTracker
            trafficTracker = new TrafficTracker(ZONES);
            System.out.println("TrafficTracker initialized for " + ZONES.size() + " zones");

            // Step 6: Empty - Removing KDTree, RTree, Graph
            
            // Step 7: Build Interval Tree + Skip List
            // Insert all existing bookings into both
            for (ParkingSlot slot : allSlots) {
              for (List<Integer> booking : slot.bookings) {
                intervalTree.insert(slot.slotId, booking.get(0), booking.get(1));
                skipList.insert(booking.get(0), booking.get(1), slot.slotId);
               }
            }
            System.out.println("IntervalTree and SkipList built with existing bookings");

            // Step 8: Insert all zone names into Trie
            for (String zone : ZONES) {
                trie.insert(zone);
            }
            System.out.println("Trie built with " + ZONES.size() + " zones");

            System.out.println("=== DataLoader: All DS ready ===");

        } catch (Exception e) {
            System.err.println("DataLoader failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}