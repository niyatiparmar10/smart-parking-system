import { useState, useEffect, useRef } from "react";
import axios from "axios";
import MapPanel from "./components/MapPanel";
import BookingForm from "./components/BookingForm";
import ResultCard from "./components/ResultCard";
import ZoneStats from "./components/ZoneStats";
import MyBookings from "./components/MyBookings";

const API = "http://localhost:8080/api";

// ✅ FIX 1: distanceBetween is at TOP LEVEL, not inside any function
function distanceBetween(lat1, lng1, lat2, lng2) {
  const dLat = lat1 - lat2;
  const dLng = lng1 - lng2;
  return Math.sqrt(dLat * dLat + dLng * dLng) * 111000;
}

export default function App() {
  const [slots, setSlots] = useState([]);
  const [booking, setBooking] = useState(null);
  const [myBookings, setMyBookings] = useState([]);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  const [browseMode, setBrowseMode] = useState(false);
  const [browseSlots, setBrowseSlots] = useState([]);
  const [userLocation, setUserLocation] = useState(null);
  const [reallocation, setReallocation] = useState(null);
  const [selectedSlot, setSelectedSlot] = useState(null);
  const reallocationTimer = useRef(null);

  const refreshSlots = async () => {
    const res = await axios.get(`${API}/slots`);
    setSlots(res.data);
  };

  useEffect(() => {
    refreshSlots();
  }, []);

  const handleBook = async (formData) => {
    setLoading(true);
    setError(null);
    setReallocation(null);
    try {
      const res = await axios.post(`${API}/book`, formData);
      if (res.data.message === "success") {
        setBooking(res.data);
        setMyBookings((prev) => [...prev, { ...res.data, ...formData }]);
        await refreshSlots();
        if (!formData.directBook) {
          startReallocationWatch(res.data, formData);
        }
      } else {
        setError(res.data.message);
      }
    } catch {
      setError("Booking failed. Is the backend running?");
    }
    setLoading(false);
    setSelectedSlot(null);
  };

  const handleCancel = async (b) => {
    await axios.post(
      `${API}/cancel?slotId=${b.slotId}&startMin=${b.startMin}&endMin=${b.endMin}`,
    );
    setMyBookings((prev) => prev.filter((x) => x.slotId !== b.slotId));
    if (booking?.slotId === b.slotId) setBooking(null);
    await refreshSlots();
  };

  const startReallocationWatch = (current, formData) => {
    if (reallocationTimer.current) clearInterval(reallocationTimer.current);

    reallocationTimer.current = setInterval(async () => {
      try {
        const res = await axios.get(`${API}/slots`);

        const nearbyFreeSlot = res.data.find((s) => {
          if (s.isOccupied) return false;
          if (s.slotId === current.slotId) return false;
          const d = distanceBetween(s.lat, s.lng, formData.lat, formData.lng);
          return d < current.distanceMeters - 50;
        });

        if (nearbyFreeSlot) {
          // Cancel old booking first
          await axios.post(
            `${API}/cancel?slotId=${current.slotId}&startMin=${formData.startMin}&endMin=${formData.endMin}`,
          );
          // Book the better slot
          const newBooking = await axios.post(`${API}/book`, formData);
          if (newBooking.data.message === "success") {
            setReallocation(newBooking.data);
            setBooking(newBooking.data);
            setMyBookings((prev) =>
              prev.map((b) =>
                b.slotId === current.slotId
                  ? { ...newBooking.data, ...formData }
                  : b,
              ),
            );
            await refreshSlots();
            clearInterval(reallocationTimer.current);
          }
        }
      } catch {
        /* silent */
      }
    }, 15000);

    setTimeout(() => clearInterval(reallocationTimer.current), 120000);
  };

  const handleBrowse = async (lat, lng) => {
    setBrowseMode(true);
    const res = await axios.get(`${API}/browse?lat=${lat}&lng=${lng}`);
    setBrowseSlots(res.data);
  };

  // ✅ FIX 2: simple and direct — no async getNearestZone,
  // just find nearest zone from already-loaded slots state
  const handleDemoCongest = async () => {
    let zone = "Koregaon Park"; // default fallback

    if (userLocation && slots.length > 0) {
      const nearest = slots
        .filter((s) => !s.isOccupied)
        .sort(
          (a, b) =>
            distanceBetween(a.lat, a.lng, userLocation.lat, userLocation.lng) -
            distanceBetween(b.lat, b.lng, userLocation.lat, userLocation.lng),
        )[0];
      if (nearest) zone = nearest.zone;
    }

    await axios.post(`${API}/demo/congest?zone=${encodeURIComponent(zone)}`);
    await refreshSlots();
    alert(`Congestion simulated in "${zone}". Now click Find & Book Slot!`);
  };

  const handleDemoFreeCloser = async () => {
    if (!booking) {
      alert("Book a slot first, then click this.");
      return;
    }
    await axios.post(
      `${API}/demo/free-closest?lat=${booking.lat}&lng=${booking.lng}`,
    );
    // Reallocation timer will detect this within 15 seconds automatically
  };

  const handleDemoReset = async () => {
    await axios.post(`${API}/demo/reset`);
    setBooking(null);
    setMyBookings([]);
    setReallocation(null);
    if (reallocationTimer.current) clearInterval(reallocationTimer.current);
    await refreshSlots();
  };

  return (
    <div>
      <div className="topbar">
        <div className="topbar-logo">
          <div className="logo-icon">P</div>
          <div>
            <h1>SmartPark</h1>
            <span>Pune, India</span>
          </div>
        </div>
        <SearchBar api={API} />
        <div className="demo-bar">
          <span className="demo-label">Demo</span>
          <button
            className="btn-demo btn-demo-congest"
            onClick={handleDemoCongest}
          >
            Simulate Congestion
          </button>
          <button
            className="btn-demo btn-demo-reset"
            onClick={handleDemoFreeCloser}
          >
            Free Closer Slot
          </button>
          <button className="btn-demo btn-demo-reset" onClick={handleDemoReset}>
            Reset All
          </button>
        </div>
      </div>

      <div className="main-layout">
        <div className="map-container">
          <MapPanel
            slots={browseMode ? browseSlots : slots}
            booking={booking}
            userLocation={userLocation}
            browseMode={browseMode}
            onSlotClick={setSelectedSlot}
          />
        </div>

        <div className="side-panel">
          {reallocation && (
            <div className="panel-section">
              <div className="reallocation-banner">
                🔄{" "}
                <div>
                  <strong>Better slot found!</strong> Reassigned to{" "}
                  <strong>{reallocation.slotId}</strong> —{" "}
                  {Math.round(reallocation.distanceMeters)}m away
                </div>
              </div>
            </div>
          )}

          <div className="panel-section">
            <BookingForm
              onBook={handleBook}
              onBrowse={handleBrowse}
              onExitBrowse={() => {
                setBrowseMode(false);
                setBrowseSlots([]);
              }}
              browseMode={browseMode}
              loading={loading}
              setUserLocation={setUserLocation}
              selectedSlot={selectedSlot}
            />
            {error && (
              <div className="error-card" style={{ marginTop: "12px" }}>
                ❌ {error}
              </div>
            )}
          </div>

          {booking && (
            <div className="panel-section">
              <div className="section-title">Your Booking</div>
              <ResultCard booking={booking} />
            </div>
          )}

          {myBookings.length > 0 && (
            <div className="panel-section">
              <div className="section-title">My Bookings</div>
              <MyBookings bookings={myBookings} onCancel={handleCancel} />
            </div>
          )}

          <div className="panel-section">
            <div className="section-title">Zone Availability</div>
            <div className="legend" style={{ marginBottom: "12px" }}>
              <div className="legend-item">
                <div className="legend-dot" style={{ background: "#00c896" }} />
                Available
              </div>
              <div className="legend-item">
                <div className="legend-dot" style={{ background: "#ef4444" }} />
                Occupied
              </div>
              <div className="legend-item">
                <div className="legend-dot" style={{ background: "#f59e0b" }} />
                Filling up
              </div>
            </div>
            <ZoneStats slots={slots} />
          </div>
        </div>
      </div>

      {selectedSlot && (
        <SlotPopup
          slot={selectedSlot}
          loading={loading}
          onBook={handleBook}
          onClose={() => setSelectedSlot(null)}
          userLocation={userLocation}
        />
      )}
    </div>
  );
}

function SlotPopup({ slot, loading, onBook, onClose, userLocation }) {
  const loc = userLocation || { lat: 18.5308, lng: 73.8474 };
  return (
    <div
      style={{
        position: "fixed",
        bottom: "24px",
        left: "50%",
        transform: "translateX(-50%)",
        background: "#141e33",
        border: "1px solid rgba(255,255,255,0.1)",
        borderRadius: "16px",
        padding: "18px 20px",
        zIndex: 3000,
        minWidth: "280px",
        boxShadow: "0 16px 48px rgba(0,0,0,0.6)",
      }}
    >
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          marginBottom: "12px",
        }}
      >
        <div>
          <div
            style={{ fontSize: "18px", fontWeight: "800", color: "#00c896" }}
          >
            {slot.slotId}
          </div>
          <div style={{ fontSize: "12px", color: "#64748b" }}>{slot.zone}</div>
        </div>
        <button
          onClick={onClose}
          style={{
            background: "none",
            border: "none",
            color: "#4a5568",
            fontSize: "18px",
            cursor: "pointer",
          }}
        >
          ✕
        </button>
      </div>
      <div
        style={{
          display: "grid",
          gridTemplateColumns: "1fr 1fr",
          gap: "8px",
          marginBottom: "14px",
        }}
      >
        {[
          ["Type", slot.type],
          ["Price", `₹${slot.pricePerHour}/hr`],
          ["Status", slot.isOccupied ? "Occupied" : "Available"],
        ].map(([l, v]) => (
          <div
            key={l}
            style={{
              background: "rgba(255,255,255,0.03)",
              borderRadius: "8px",
              padding: "8px",
            }}
          >
            <div
              style={{
                fontSize: "10px",
                color: "#4a5568",
                textTransform: "uppercase",
                letterSpacing: "0.5px",
              }}
            >
              {l}
            </div>
            <div
              style={{
                fontSize: "13px",
                fontWeight: "600",
                color: "#f0f4ff",
                marginTop: "2px",
              }}
            >
              {v}
            </div>
          </div>
        ))}
      </div>
      <button
        className="map-popup-btn"
        disabled={slot.isOccupied || loading}
        onClick={() =>
          onBook({
            lat: slot.lat,
            lng: slot.lng,
            startMin: 660,
            endMin: 780,
            vehicleType: slot.type === "EV" ? "EV" : "NORMAL",
            directBook: true,
          })
        }
      >
        {slot.isOccupied
          ? "Slot Occupied"
          : loading
            ? "Booking..."
            : "Book This Slot"}
      </button>
    </div>
  );
}

function SearchBar({ api }) {
  const [query, setQuery] = useState("");
  const [suggestions, setSuggestions] = useState([]);

  const handleChange = async (e) => {
    const val = e.target.value;
    setQuery(val);
    if (val.length < 1) {
      setSuggestions([]);
      return;
    }
    const res = await axios.get(`${api}/zones/autocomplete?prefix=${val}`);
    setSuggestions(res.data);
  };

  return (
    <div className="search-wrapper">
      <span className="search-icon">🔍</span>
      <input
        placeholder="Search zone..."
        value={query}
        onChange={handleChange}
      />
      {suggestions.length > 0 && (
        <div className="autocomplete-dropdown">
          {suggestions.map((s) => (
            <div
              key={s}
              onClick={() => {
                setQuery(s);
                setSuggestions([]);
              }}
            >
              {s}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
