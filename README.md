# Smart Parking Allocation System

### Advanced Data Structures — Semester Project

A full-stack intelligent parking allocation system for mall/venue parking, built using 9 advanced data structures implemented from scratch in Java.

---

## Project Overview

This system simulates a real-world smart parking setup for a mall. A user walks up to a kiosk screen, selects their vehicle type, and clicks one button — the system instantly allocates the best available parking slot using multiple data structures working together.

**Two Novelty Features:**

1. **Congestion-Aware Zone Routing** — If the nearest zone is >80% full, Dijkstra's algorithm finds the next best zone
2. **Dynamic Reallocation** — If a closer slot frees up within 10 seconds of your booking, the system automatically reassigns you

---

## Data Structures Used

| #   | Data Structure   | Role                                              |
| --- | ---------------- | ------------------------------------------------- |
| 1   | KD-Tree          | Find K nearest parking slots to entry point       |
| 2   | R-Tree           | Find all slots within radius (browse mode)        |
| 3   | Segment Tree     | Track free slot count per zone, detect congestion |
| 4   | Interval Tree    | Detect booking time conflicts per slot            |
| 5   | Skip List        | Sorted global booking interval search             |
| 6   | Min-Heap         | Rank candidate slots by priority score            |
| 7   | Trie             | Zone name autocomplete search                     |
| 8   | HashMap          | O(1) slot lookup by Slot ID                       |
| 9   | Graph + Dijkstra | Find nearest non-congested zone                   |

All data structures are implemented from scratch — no built-in library DS used.

---

## Tech Stack

| Layer    | Technology                         |
| -------- | ---------------------------------- |
| Backend  | Java 17 + Spring Boot 3.2          |
| Frontend | React + Vite                       |
| Map      | Leaflet.js + OpenStreetMap         |
| Data     | JSON file (150 mall parking slots) |
| API      | REST (Spring Boot ↔ React)         |

---

## Project Structure

```
parking-system/
├── backend/
│   ├── src/main/java/com/smartparking/backend/
│   │   ├── ds/              ← All 9 DS implementations
│   │   ├── model/           ← ParkingSlot, BookingRequest, BookingResult
│   │   ├── service/         ← AllocationService, DataLoader, ReallocationService
│   │   └── controller/      ← ParkingController (REST API)
│   └── src/main/resources/
│       └── slots.json       ← 150 mall parking slots dataset
│
└── frontend/
    └── src/
        ├── components/      ← MapPanel, BookingForm, ResultCard, ZoneStats, MyBookings
        ├── App.jsx          ← Admin dashboard
        ├── KioskPage.jsx    ← User-facing kiosk screen
        └── index.css        ← Styling
```

---

## How to Run

### Backend

```bash
cd backend
mvnw.cmd spring-boot:run     # Windows
./mvnw spring-boot:run       # Mac/Linux
```

Backend starts on http://localhost:8080

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend starts on http://localhost:5173

---

## Pages

- **http://localhost:5173** → Admin dashboard (full map, zone stats, bookings management)
- Click **"Kiosk View"** button → User-facing screen (just a button + result)

---

## Demo Flow

### Normal Booking

1. Open Kiosk View
2. Select vehicle type
3. Click "Find Me a Parking Spot"
4. System assigns best slot in milliseconds

### Congestion Feature

1. Go to Admin View
2. Click "Simulate Congestion" → fills nearest zone
3. Go to Kiosk View → book a slot
4. System routes you to next available zone (Dijkstra)

### Dynamic Reallocation

1. Book a slot in Kiosk View
2. Go to Admin View → click "Free Closer Slot"
3. Wait 10 seconds → Kiosk shows "Better spot found!"

---

## Dataset

150 parking slots across 6 zones (Zone A to Zone F) in a mall:

- 25 slots per zone
- Mix of NORMAL, EV, and DISABLED slots
- Pricing: ₹20–₹30/hour
- All slots start empty (no pre-existing bookings)

---

## Why No Login/Signup?

This system is designed as a kiosk — the user physically walks up to a screen at the mall entrance. There's no need for user accounts. The admin panel (the full dashboard) would be accessible only to parking staff on a secured internal terminal. Adding authentication is straightforward with Spring Security + JWT and would not change any DS logic — it would simply sit in front of the controller layer.

---

_Built for Advanced Data Structures course, SY Semester 4_
