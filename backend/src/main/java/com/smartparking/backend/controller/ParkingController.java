package com.smartparking.backend.controller;

import com.smartparking.backend.model.*;
import com.smartparking.backend.service.AllocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public String cancel(@RequestParam String slotId,
                         @RequestParam int startMin,
                         @RequestParam int endMin) {
        boolean cancelled = allocationService.cancelBooking(slotId, startMin, endMin);
        if (!cancelled) {
            return "Failed to cancel or slot was not occupied.";
        }
        
        // As soon as a slot is cancelled, we try to reallocate someone from a worse zone into this newly freed slot!
        String reallocMsg = allocationService.triggerReallocation();
        return "Booking cancelled. " + reallocMsg;
    }

    // --- Trie autocomplete for zone search bar ---
    @GetMapping("/zones/autocomplete")
    public List<String> autocomplete(@RequestParam String prefix) {
        return allocationService.autocomplete(prefix);
    }

    // --- Segment Tree Range Queries ---
    @GetMapping("/zones/total-free")
    public int getTotalFree() {
        return allocationService.getTotalFreeSlots();
    }

    @GetMapping("/zones/range-free")
    public int getRangeFree(@RequestParam String startZone, @RequestParam String endZone) {
        return allocationService.getFreeSlotsInRange(startZone, endZone);
    }

    // --- Demo: simulate traffic congestion in a zone ---
    @PostMapping("/demo/congest")
    public String congestZone(@RequestParam String zone) {
        // By inserting 10 instant bookings into TrafficTracker for this zone,
        // we instantly trigger the "High Traffic" reroute protocol for the next visitor.
        allocationService.congestZoneForDemo(zone);
        return "Simulated massive traffic (10 simultaneous bookings) in " + zone;
    }

    // --- Demo: reset all slots ---
    @PostMapping("/demo/reset")
    public String resetAll() {
        allocationService.resetAllSlots();
        return "All slots reset";
    }
}