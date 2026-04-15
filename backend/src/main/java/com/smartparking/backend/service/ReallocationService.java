package com.smartparking.backend.service;

import com.smartparking.backend.model.ParkingSlot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;

/**
 * Runs every 60 seconds.
 * Checks if any booking's end time has passed — if yes, frees the slot.
 * This simulates real-world slot expiry without a database.
 */
@EnableScheduling
@Service
public class ReallocationService {

    @Autowired
    private DataLoader data;

    @Scheduled(fixedRate = 60000) // every 60 seconds
    public void expireOldBookings() {
        // int nowMinutes = LocalTime.now().getHour() * 60
        //                + LocalTime.now().getMinute();

        // for (ParkingSlot slot : data.allSlots) {
        //     if (!slot.isOccupied) continue;

        //     // Check if ALL bookings for this slot have ended
        //     boolean allExpired = slot.bookings.stream()
        //         .allMatch(b -> b.get(1) <= nowMinutes);

        //     if (allExpired && !slot.bookings.isEmpty()) {
        //         slot.isOccupied = false;
        //         data.segmentTree.update(slot.zone, +1);
        //         System.out.println("Slot " + slot.slotId
        //             + " expired and is now available");
        //     }
        // }
    }
}