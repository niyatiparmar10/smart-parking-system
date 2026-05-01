# Smart Parking System Demo Script

Congratulations on completing the refactort! The system is now significantly cleaner, more deterministic, and precisely tailored to a real-world mall parking scenario.

## Key Changes Implemented
1. **Removed Geographic Graphs/KD-Trees**: Allocation now works sequentially (`Zone A` -> `Zone F`).
2. **True Traffic Congestion Tracking**: Added `TrafficTracker.java` (Queue Data Structure). If 10 cars enter a specific zone within 10 minutes, that zone legally blocks new admittances and routes cars to the *next* zone to prevent bottlenecking at the gate.
3. **Cancellation-Triggered Dynamic Reallocation**: The app no longer "polls" the server endlessly. Real reallocation only happens if a car leaves early, freeing up a better zone spot. The system detects this and instantly text-alerts (simulated via UI) a patron in a worse zone (e.g., Zone C) to take the free spot in Zone A.
4. **Auto-Expiry**: Kiosk now accepts `durationHours` (1-6 hours). A backend `@Scheduled(fixedRate = 60000)` Java Daemon runs constantly. The moment your configured time ends, the slot automatically frees itself.

---

## How to Demo to Your Professor

Here is a step-by-step script to perfectly demonstrate the "Novelties" of your project.

### Part 1: The Core Pipeline & Sequential Filling
1. Open the **Admin Panel**. Click `Reset Everything` just to be safe.
2. Click **Kiosk View**. 
3. Book a slot for a Normal Vehicle. Notice how it instantly places you into `Zone A` (slot `A-1`). Look at the "Duration" dropdown working perfectly.
4. Go back to the **Admin View** and show how the lot updated in real time. Explain that the KD-Tree was removed to make it "Mall Authentic"—we fill closest zones sequentially.

### Part 2: The "Novelty" of Traffic Congestion
> **Goal:** Show that even if Zone A has lots of empty space, an *influx* of sudden cars will cause a traffic jam, redirecting new arrivals to Zone B to spread out the load.
1. On the **Admin Panel**, click the purple button that says `Spam Traffic (Zone A)`.
2. An alert will pop up saying a traffic jam was simulated. Explain to the professor: *"I just simulated 10 cars arriving at the mall at the exact same minute and all rushing into Zone A. The entrance to Zone A is now congested."*
3. Immediately go to **Kiosk View** and book a new slot.
4. **BOOM!** Notice how the system assigned you to **Zone B**! Even though Zone A has space, the Traffic Tracker algorithm (Queue Sliding Window) rejected Zone A to prevent car pile-ups.

### Part 3: The "Novelty" of Dynamic Reallocation
> **Goal:** Show that if a prime spot opens up (Zone A), someone parked far away (Zone B) will get a VIP upgrade automatically!
1. Following Part 2, you now have a car parked in **Zone B** (as a result of the traffic rerouting).
2. Go back to the **Admin Panel**. Scroll down to your "Active Bookings" list.
3. Find the original car that was parked in **Zone A** (from Part 1). Click `Cancel` next to it.
4. Explain to the professor: *"A customer in Zone A left early. The system instantly detects this and realizes another customer was forced to park far away in Zone B. It automatically moves them!"*
5. Right as you click Cancel, an **Alert popup** will appear on the Admins screen: *"Booking cancelled. Reallocated booking from B-1 to A-1"*. 
6. Show the map updating! The car in Zone B has literally been moved back to the premium Zone A slot.

### Wrapping Up
You can briefly mention that behind the scenes, a Cron job runs every minute to automatically expire bookings when their duration hits zero (without anyone needing to press cancel).

Good luck with the presentation! The code is rock-solid and the flows are much more intuitive now!
