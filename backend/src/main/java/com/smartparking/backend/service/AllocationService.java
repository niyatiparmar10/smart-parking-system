package com.smartparking.backend.service;

import com.smartparking.backend.ds.*;
import com.smartparking.backend.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.List;

/**
 * AllocationService is the core pipeline.
 * It wires all 8 DS together to process a booking request.
 *
 * Pipeline:
 * 1. KDTree  → find K nearest candidate slots
 * 2. RTree   → find all slots within radius (browse mode)
 * 3. SegmentTree → filter congested zones
 * 4. Graph + Dijkstra → reroute if nearest zone is congested
 * 5. IntervalTree + SkipList → remove slots with booking conflicts
 * 6. Vehicle type filter
 * 7. MinHeap → rank remaining candidates, pick best
 * 8. HashMap → update slot status
 */
@Service
public class AllocationService {

    @Autowired
    private DataLoader data;

    // How many nearest slots KDTree fetches as candidates
    private static final int K_NEAREST = 10;

    // Radius for browse mode (meters)
    private static final double BROWSE_RADIUS_METERS = 1500;

    // -------------------------------------------------------------------
    // MAIN BOOKING METHOD
    // -------------------------------------------------------------------
    public BookingResult allocate(BookingRequest request) {

        System.out.println("\n--- New Booking Request ---");
        System.out.println("Location: " + request.lat + ", " + request.lng);
        System.out.println("Time: " + IntervalTree.toTimeString(request.startMin)
                + " - " + IntervalTree.toTimeString(request.endMin));
        System.out.println("Vehicle: " + request.vehicleType);

        // STEP 1: KDTree → get K nearest candidate slots
        List<ParkingSlot> candidates = data.kdTree.findKNearest(
                request.lat, request.lng, K_NEAREST);
        System.out.println("KDTree found: " + candidates.size() + " nearest slots");

        if (candidates.isEmpty()) {
            return new BookingResult(null, null, 0, 0, 0, 0,
                    "No parking slots found near your location.");
        }

        // STEP 2: Check congestion using SegmentTree
        // Find which zones are congested among candidates
        Set<String> congestedZones = new HashSet<>();
        for (ParkingSlot slot : candidates) {
            int total = data.zoneTotalSlots.getOrDefault(slot.zone, 1);
            if (data.segmentTree.isCongested(slot.zone, total)) {
                congestedZones.add(slot.zone);
                System.out.println("SegmentTree: " + slot.zone + " is congested");
            }
        }

        // STEP 3: Graph + Dijkstra → if nearest zone congested, pull slots from alternative zone
        String preferredZone = candidates.get(0).zone;
           if (congestedZones.contains(preferredZone)) {
    String alternativeZone = data.graph.findNearestAvailableZone(
            preferredZone, congestedZones);
    if (alternativeZone != null) {
        System.out.println("Graph rerouted from " + preferredZone
                + " to " + alternativeZone);
        // Pull ALL slots from the alternative zone directly
        for (ParkingSlot s : data.allSlots) {
            if (s.zone.equals(alternativeZone) && !s.isOccupied) {
                candidates.add(s);
            }
        }
    }
}

        // STEP 4: SegmentTree filter → remove slots from fully congested zones
        List<ParkingSlot> afterCongestion = new ArrayList<>();
        for (ParkingSlot slot : candidates) {
            if (!slot.isOccupied) {
                // Only hard-reject if zone has ZERO free slots
                if (data.segmentTree.getFreeCount(slot.zone) > 0) {
                    afterCongestion.add(slot);
                }
            }
        }
        System.out.println("After congestion filter: " + afterCongestion.size()
                + " slots remain");

        if (afterCongestion.isEmpty()) {
            return new BookingResult(null, null, 0, 0, 0, 0,
                    "All nearby zones are full. Try a different time.");
        }

        // STEP 5: IntervalTree + SkipList → remove slots with booking conflicts
        List<ParkingSlot> afterConflict = new ArrayList<>();
        for (ParkingSlot slot : afterCongestion) {
            boolean itConflict = data.intervalTree.hasConflict(
                    slot.slotId, request.startMin, request.endMin);
            boolean slConflict = data.skipList.hasConflict(
                    slot.slotId, request.startMin, request.endMin);

            if (!itConflict && !slConflict) {
                afterConflict.add(slot);
            } else {
                System.out.println("Conflict detected on slot " + slot.slotId);
            }
        }
        System.out.println("After conflict filter: " + afterConflict.size()
                + " slots remain");

        if (afterConflict.isEmpty()) {
            return new BookingResult(null, null, 0, 0, 0, 0,
                    "No slots available for the requested time.");
        }

        // STEP 6: Vehicle type filter
        List<ParkingSlot> afterTypeFilter = new ArrayList<>();
        for (ParkingSlot slot : afterConflict) {
            if (slot.type.equals(request.vehicleType)
                    || slot.type.equals("NORMAL")) {
                afterTypeFilter.add(slot);
            }
        }
        // If no type match at all, fall back to all conflict-free slots
        if (afterTypeFilter.isEmpty()) {
            afterTypeFilter = afterConflict;
        }
        System.out.println("After type filter: " + afterTypeFilter.size()
                + " slots remain");

        // STEP 7: MinHeap → score and rank all remaining candidates
        // First find max distance for normalization
        double maxDist = 0;
        for (ParkingSlot slot : afterTypeFilter) {
            double d = distanceMeters(slot.lat, slot.lng, request.lat, request.lng);
            if (d > maxDist) maxDist = d;
        }

        MinHeap heap = new MinHeap();
        for (ParkingSlot slot : afterTypeFilter) {
            double distM = distanceMeters(
                    slot.lat, slot.lng, request.lat, request.lng);
            int total = data.zoneTotalSlots.getOrDefault(slot.zone, 1);
            boolean congested = data.segmentTree.isCongested(slot.zone, total);

            double score = MinHeap.computeScore(
                    distM, maxDist, slot, request.vehicleType, congested);

            heap.insert(new MinHeap.HeapEntry(slot, score, distM));
        }

        // Extract the best slot
        MinHeap.HeapEntry best = heap.extractMin();
        ParkingSlot winner = best.slot;
        System.out.println("MinHeap selected: " + winner.slotId
                + " (score: " + String.format("%.3f", best.score) + ")");

        // STEP 8: Update all DS to reflect this booking
        confirmBooking(winner, request);

        return new BookingResult(
                winner.slotId,
                winner.zone,
                winner.lat,
                winner.lng,
                winner.pricePerHour,
                Math.round(best.distanceMeters),
                "success"
        );
    }

    // -------------------------------------------------------------------
    // BROWSE MODE — R-Tree radius search
    // -------------------------------------------------------------------
    public List<ParkingSlot> browse(double lat, double lng) {
        List<ParkingSlot> nearby = data.rTree.findWithinRadius(
                lat, lng, BROWSE_RADIUS_METERS);

        // Filter out occupied slots
        List<ParkingSlot> available = new ArrayList<>();
        for (ParkingSlot slot : nearby) {
            if (!slot.isOccupied) available.add(slot);
        }

        System.out.println("RTree browse: " + available.size()
                + " available slots within "
                + BROWSE_RADIUS_METERS + "m");
        return available;
    }

    // -------------------------------------------------------------------
    // CONFIRM BOOKING — update all DS after a slot is assigned
    // -------------------------------------------------------------------
    private void confirmBooking(ParkingSlot slot, BookingRequest request) {
        // Mark slot as occupied in HashMap
        slot.isOccupied = true;

        // Add booking to IntervalTree
        data.intervalTree.insert(
                slot.slotId, request.startMin, request.endMin);

        // Add booking to SkipList
        data.skipList.insert(
                request.startMin, request.endMin, slot.slotId);

        // Update SegmentTree (one less free slot in this zone)
        data.segmentTree.update(slot.zone, -1);

        System.out.println("Booking confirmed: slot " + slot.slotId
                + " in " + slot.zone
                + " | Free slots in zone: "
                + data.segmentTree.getFreeCount(slot.zone));
    }

    // -------------------------------------------------------------------
    // CANCEL BOOKING — free up a slot
    // -------------------------------------------------------------------
    public boolean cancelBooking(String slotId, int startMin, int endMin) {
        ParkingSlot slot = data.slotMap.get(slotId);
        if (slot == null) return false;

        slot.isOccupied = false;
        data.intervalTree.remove(slotId, startMin, endMin);
        data.skipList.delete(startMin, slotId);
        data.segmentTree.update(slot.zone, +1);

        System.out.println("Booking cancelled: slot " + slotId);
        return true;
    }

    // -------------------------------------------------------------------
    // TRIE — zone autocomplete
    // -------------------------------------------------------------------
    public List<String> autocomplete(String prefix) {
        return data.trie.autocomplete(prefix);
    }

    // -------------------------------------------------------------------
    // HELPER — distance between two lat/lng points in meters
    // -------------------------------------------------------------------
    private double distanceMeters(double lat1, double lng1,
                                   double lat2, double lng2) {
        double dLat = lat1 - lat2;
        double dLng = lng1 - lng2;
        return Math.sqrt(dLat * dLat + dLng * dLng) * 111000;
    }

    // -------------------------------------------------------------------
    // GET ALL SLOTS — for map display on frontend
    // -------------------------------------------------------------------
    public List<ParkingSlot> getAllSlots() {
        return data.allSlots;
    }
    public void congestZoneForDemo(String zone) {
    data.segmentTree.update(zone, -1);
}

public void resetAllSlots() {
    for (ParkingSlot slot : data.allSlots) {
        slot.isOccupied = false;
        slot.bookings.clear();
    }
    // Rebuild segment tree counts
    for (String zone : List.of(
            "Koregaon Park", "Shivajinagar", "FC Road", "Kothrud",
            "Hadapsar", "Viman Nagar", "Aundh", "Baner", "Camp", "Deccan")) {
        long free = data.allSlots.stream()
            .filter(s -> s.zone.equals(zone) && !s.isOccupied)
            .count();
        data.segmentTree.setZoneCount(zone, (int) free);
    }
    data.segmentTree.build();
    // Rebuild interval tree and skip list
    data.intervalTree = new com.smartparking.backend.ds.IntervalTree();
    data.skipList     = new com.smartparking.backend.ds.SkipList();
    System.out.println("All slots reset");
}
}