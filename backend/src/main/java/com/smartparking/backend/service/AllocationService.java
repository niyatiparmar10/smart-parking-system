package com.smartparking.backend.service;

import com.smartparking.backend.ds.*;
import com.smartparking.backend.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.*;

@Service
@EnableScheduling
public class AllocationService {

    @Autowired
    private DataLoader data;

    private static final List<String> ZONE_PRIORITY = Arrays.asList(
            "Zone A", "Zone B", "Zone C", "Zone D", "Zone E", "Zone F"
    );

    // -------------------------------------------------------------------
    // MAIN BOOKING METHOD
    // -------------------------------------------------------------------
    public BookingResult allocate(BookingRequest request) {

        System.out.println("\n--- New Booking Request ---");
        System.out.println("Duration Hours: " + request.durationHours);
        System.out.println("Vehicle: " + request.vehicleType);

        int startMin = (int) (System.currentTimeMillis() / 60000);
        int endMin = startMin + request.durationHours * 60;

        ParkingSlot winner = null;
        boolean allTrafficCongested = true;

        // Pass 1: Try to find a slot respecting Traffic Tracker rules
        for (String zone : ZONE_PRIORITY) {
            boolean isFull = data.segmentTree.getFreeCount(zone) == 0;
            if (isFull) continue;

            boolean isCongested = data.trafficTracker.isTrafficCongested(zone);
            if (isCongested) {
                System.out.println("Zone " + zone + " skipped (High Traffic in last 10m)");
                continue;
            }

            allTrafficCongested = false;
            winner = findBestSlotInZone(zone, request.vehicleType, startMin, endMin);
            if (winner != null) break;
        }

        // Pass 2: Fallback — If all zones with space are traffic-congested, ignore traffic
        if (winner == null && allTrafficCongested) {
            System.out.println("Fallback: All zones traffic-jammed. Ignoring traffic lock.");
            for (String zone : ZONE_PRIORITY) {
                boolean isFull = data.segmentTree.getFreeCount(zone) == 0;
                if (isFull) continue;

                winner = findBestSlotInZone(zone, request.vehicleType, startMin, endMin);
                if (winner != null) break;
            }
        }

        if (winner == null) {
            return new BookingResult(null, null, 0, 0, 0, 0,
                    "No slots available. The mall parking is completely full or no spot matches your vehicle type.");
        }

        System.out.println("Assigned slot: " + winner.slotId + " in " + winner.zone);
        
        confirmBooking(winner, startMin, endMin);

        return new BookingResult(
                winner.slotId,
                winner.zone,
                winner.lat,
                winner.lng,
                winner.pricePerHour,
                0, // Distance no longer matters
                "success"
        );
    }

    private ParkingSlot findBestSlotInZone(String zone, String vType, int startMin, int endMin) {
        com.smartparking.backend.ds.MinHeap heap = new com.smartparking.backend.ds.MinHeap();

        for (ParkingSlot slot : data.allSlots) {
            if (slot.zone.equals(zone) && !slot.isOccupied) {
                // Vehicle type match
                if (!slot.type.equals("NORMAL") && !slot.type.equals(vType)) continue;
                
                // Interval/Time conflict match
                boolean itConflict = data.intervalTree.hasConflict(slot.slotId, startMin, endMin);
                if (itConflict) continue;

                // Rank slots by their ID number so lower IDs (e.g. A-1) are preferred over higher IDs (e.g. A-10)
                int idNum = 999;
                try {
                    String[] parts = slot.slotId.split("-");
                    idNum = Integer.parseInt(parts[1]);
                } catch (Exception e) {}

                heap.insert(new com.smartparking.backend.ds.MinHeap.HeapEntry(slot, idNum, 0.0));
            }
        }

        com.smartparking.backend.ds.MinHeap.HeapEntry best = heap.extractMin();
        return best != null ? best.slot : null;
    }

    // -------------------------------------------------------------------
    // AUTO EXPIRY (Runs every minute)
    // -------------------------------------------------------------------
    @Scheduled(fixedRate = 60000)
    public void autoExpireSlots() {
        int currentMin = (int) (System.currentTimeMillis() / 60000);
        
        // SkipList is sorted by endTime. It allows us to pluck out expired bookings in O(1) time
        // without looping through the other 100+ active slots!
        List<String[]> expiredBookings = data.skipList.getExpired(currentMin);
        
        for (String[] exp : expiredBookings) {
            String slotId = exp[0];
            int s = Integer.parseInt(exp[1]);
            int e = Integer.parseInt(exp[2]);
            
            System.out.println("Auto-expiring slot " + slotId + " (Time up)");
            cancelBooking(slotId, s, e);
        }
    }

    // -------------------------------------------------------------------
    // DYNAMIC REALLOCATION (Triggered explicitly on cancellation)
    // -------------------------------------------------------------------
    public String triggerReallocation() {
        // Look for the "worst" placed vehicle (e.g. Zone F, E, D, C, B) and try to upgrade them
        int currentMin = (int) (System.currentTimeMillis() / 60000);
        
        for (int i = ZONE_PRIORITY.size() - 1; i >= 1; i--) {
            String worseZone = ZONE_PRIORITY.get(i);
            
            for (ParkingSlot occupiedSlot : data.allSlots) {
                if (occupiedSlot.isOccupied && occupiedSlot.zone.equals(worseZone)) {
                    // Only reallocate if booked within the last 40 seconds
                    long now = System.currentTimeMillis();
                    if (now - occupiedSlot.lastAssignedTimeMs > 40000) {
                        continue;
                    }

                    // Try to reallocate this person to a better zone (e.g. Zone A -> current zone - 1)
                    int startM = occupiedSlot.bookings.get(occupiedSlot.bookings.size() - 1).get(0);
                    int endM = occupiedSlot.bookings.get(occupiedSlot.bookings.size() - 1).get(1);
                    String vType = occupiedSlot.type;
                    
                    // Search purely for a better spot in a higher priority zone
                    for (int j = 0; j < i; j++) {
                        String betterZone = ZONE_PRIORITY.get(j);
                        boolean isFull = data.segmentTree.getFreeCount(betterZone) == 0;
                        if (isFull) continue;

                        ParkingSlot betterSpot = findBestSlotInZone(betterZone, vType, startM, endM);
                        if (betterSpot != null) {
                            // Upgrade!
                            System.out.println("Dynamic Reallocation: Moved " + occupiedSlot.slotId + " to " + betterSpot.slotId);
                            cancelBooking(occupiedSlot.slotId, startM, endM);
                            confirmBooking(betterSpot, startM, endM);
                            return "Reallocated booking from " + occupiedSlot.slotId + " to " + betterSpot.slotId;
                        }
                    }
                }
            }
        }
        return "No reallocation needed.";
    }

    // -------------------------------------------------------------------
    // CONFIRM BOOKING — update all DS after a slot is assigned
    // -------------------------------------------------------------------
    private void confirmBooking(ParkingSlot slot, int startMin, int endMin) {
        slot.isOccupied = true;
        slot.lastAssignedTimeMs = System.currentTimeMillis();
        slot.bookings.add(Arrays.asList(startMin, endMin));
        
        data.intervalTree.insert(slot.slotId, startMin, endMin);
        data.skipList.insert(startMin, endMin, slot.slotId);
        data.segmentTree.update(slot.zone, -1);
        data.trafficTracker.recordBooking(slot.zone);

        System.out.println("Booking confirmed: slot " + slot.slotId + " in " + slot.zone);
    }

    // -------------------------------------------------------------------
    // CANCEL BOOKING — free up a slot
    // -------------------------------------------------------------------
    public boolean cancelBooking(String slotId, int startMin, int endMin) {
        ParkingSlot slot = data.slotMap.get(slotId);
        if (slot == null || !slot.isOccupied) return false;

        slot.isOccupied = false;
        // Clean booking internally
        List<List<Integer>> updatedBookings = new ArrayList<>();
        for (List<Integer> b : slot.bookings) {
            int s = b.get(0);
            int e = b.get(1);
            if (s == startMin && e == endMin) {
                data.intervalTree.remove(slotId, s, e);
                data.skipList.delete(e, slotId); // SkipList deletes using endMin now
            } else {
                updatedBookings.add(b);
            }
        }
        slot.bookings = updatedBookings;
        data.segmentTree.update(slot.zone, +1);

        System.out.println("Booking cancelled: slot " + slotId);
        
        return true;
    }

    // -------------------------------------------------------------------
    // GET ALL SLOTS — for map display on frontend
    // -------------------------------------------------------------------
    public List<ParkingSlot> getAllSlots() {
        return data.allSlots;
    }

    // -------------------------------------------------------------------
    // TRIE — zone autocomplete
    // -------------------------------------------------------------------
    public List<String> autocomplete(String prefix) {
        if (data.trie == null) return new ArrayList<>();
        return data.trie.autocomplete(prefix);
    }

    // -------------------------------------------------------------------
    // SEGMENT TREE QUERIES
    // -------------------------------------------------------------------
    public int getTotalFreeSlots() {
        return data.segmentTree.getTotalFree();
    }

    public int getFreeSlotsInRange(String startZone, String endZone) {
        return data.segmentTree.getRangeFree(startZone, endZone);
    }

    // -------------------------------------------------------------------
    // DEMO CONTROLS
    // -------------------------------------------------------------------
    public void congestZoneForDemo(String zone) {
        int count = 0;
        int startMin = (int) (System.currentTimeMillis() / 60000);
        int endMin = startMin + 120; // Book for 2 hours

        for (ParkingSlot slot : data.allSlots) {
            if (slot.zone.equals(zone) && !slot.isOccupied && count < 10) {
                confirmBooking(slot, startMin, endMin);
                count++;
            }
        }
    }
    
    public void resetAllSlots() {
        for (ParkingSlot slot : data.allSlots) {
            slot.isOccupied = false;
            slot.bookings.clear();
        }
        for (String zone : ZONE_PRIORITY) {
            long free = data.allSlots.stream()
                .filter(s -> s.zone.equals(zone) && !s.isOccupied)
                .count();
            data.segmentTree.setZoneCount(zone, (int) free);
        }
        data.segmentTree.build();
        data.intervalTree = new com.smartparking.backend.ds.IntervalTree();
        data.skipList     = new com.smartparking.backend.ds.SkipList();
        data.trafficTracker.resetAll();
        System.out.println("All slots reset");
    }
}