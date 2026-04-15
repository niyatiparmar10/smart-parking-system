package com.smartparking.backend.controller;

import com.smartparking.backend.model.*;
import com.smartparking.backend.service.AllocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API controller.
 * Exposes endpoints that the React frontend will call.
 *
 * Base URL: http://localhost:8080/api
 *
 * Endpoints:
 *   GET  /api/slots              → all slots (for map display)
 *   POST /api/book               → book a slot
 *   POST /api/cancel             → cancel a booking
 *   GET  /api/browse             → radius search (R-Tree browse mode)
 *   GET  /api/zones/autocomplete → Trie autocomplete
 */
@RestController
@RequestMapping("/api")
public class ParkingController {

    @Autowired
    private AllocationService allocationService;

    // --- Get all slots (frontend uses this to draw map markers) ---
    @GetMapping("/slots")
    public List<ParkingSlot> getAllSlots() {
        return allocationService.getAllSlots();
    }

    // --- Book a slot ---
    @PostMapping("/book")
    public BookingResult book(@RequestBody BookingRequest request) {
        return allocationService.allocate(request);
    }

    // --- Cancel a booking ---
    @PostMapping("/cancel")
    public boolean cancel(@RequestParam String slotId,
                          @RequestParam int startMin,
                          @RequestParam int endMin) {
        return allocationService.cancelBooking(slotId, startMin, endMin);
    }

    // --- Browse mode: all available slots within radius (R-Tree) ---
    @GetMapping("/browse")
    public List<ParkingSlot> browse(@RequestParam double lat,
                                    @RequestParam double lng) {
        return allocationService.browse(lat, lng);
    }

    // --- Trie autocomplete for zone search bar ---
    @GetMapping("/zones/autocomplete")
    public List<String> autocomplete(@RequestParam String prefix) {
        return allocationService.autocomplete(prefix);
    }

    // --- Demo: simulate congestion in a zone ---
    @PostMapping("/demo/congest")
    public String congestZone(@RequestParam String zone) {
        int count = 0;
        for (ParkingSlot slot : allocationService.getAllSlots()) {
            if (slot.zone.equals(zone) && !slot.isOccupied && count < 13) {
                slot.isOccupied = true;
                // inject the DataLoader reference via AllocationService
                count++;
            }
        }
        // Update segment tree
        for (int i = 0; i < count; i++) {
            allocationService.congestZoneForDemo(zone);
        }
        return "Congested " + count + " slots in " + zone;
    }

    // Free up the closest occupied slot to a location (for reallocation demo)
    @PostMapping("/demo/free-closest")
    public String freeClosestSlot(@RequestParam double lat, @RequestParam double lng) {
        ParkingSlot closest = null;
        double minDist = Double.MAX_VALUE;

        for (ParkingSlot slot : allocationService.getAllSlots()) {
            if (!slot.isOccupied) continue;
            double d = Math.sqrt(Math.pow(slot.lat - lat, 2) + Math.pow(slot.lng - lng, 2));
            if (d < minDist) {
                minDist = d;
                closest = slot;
            }
        }

        if (closest == null) return "No occupied slots found";
        allocationService.cancelBooking(closest.slotId, 0, 1440);
        return "Freed slot " + closest.slotId + " in " + closest.zone;
    }

    // --- Demo: reset all slots ---
    @PostMapping("/demo/reset")
    public String resetAll() {
        allocationService.resetAllSlots();
        return "All slots reset";
    }
}