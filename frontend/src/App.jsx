import { useState, useEffect } from "react";
import axios from "axios";
import MapPanel from "./components/MapPanel";
import BookingForm from "./components/BookingForm";
import ResultCard from "./components/ResultCard";
import ZoneStats from "./components/ZoneStats";
import AdminBookings from "./components/AdminBookings";
import KioskPage from "./components/KioskPage";

const API = "http://localhost:8080/api";

export default function App() {
  const [isKiosk, setIsKiosk] = useState(false);
  const [slots, setSlots] = useState([]);
  const [booking, setBooking] = useState(null);
  const [allBookings, setAllBookings] = useState([]);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  const [selectedSlot, setSelectedSlot] = useState(null);
  const [filterZone, setFilterZone] = useState("");

  const refreshSlots = async () => {
    try {
      const res = await axios.get(`${API}/slots`);
      setSlots(res.data);
      setAllBookings(res.data.filter((s) => s.isOccupied));
    } catch {
      // API error
    }
  };

  useEffect(() => {
    refreshSlots();
  }, []);

  // When exiting Kiosk mode, refresh the admin panel
  useEffect(() => {
    if (!isKiosk) {
      refreshSlots();
    }
  }, [isKiosk]);

  const handleBook = async (formData) => {
    setLoading(true);
    setError(null);
    try {
      const res = await axios.post(`${API}/book`, formData);
      if (res.data.message === "success") {
        setBooking(res.data);
        await refreshSlots();
        alert("Force allocation successful.");
      } else {
        setError(res.data.message);
      }
    } catch {
      setError("Booking failed. Is the backend running?");
    }
    setLoading(false);
    setSelectedSlot(null);
  };

  const handleAdminCancel = async (slot) => {
    try {
      // Pass the existing booking times back so backend cleanly deletes it
      const sMin = slot.bookings[slot.bookings.length - 1][0];
      const eMin = slot.bookings[slot.bookings.length - 1][1];

      const res = await axios.post(
        `${API}/cancel?slotId=${slot.slotId}&startMin=${sMin}&endMin=${eMin}`,
      );
      if (booking?.slotId === slot.slotId) setBooking(null);
      await refreshSlots();
      // Inform the Admin if a dynamic reallocation happened behind the scenes!
      if (res.data && res.data.includes("Reallocated")) {
        alert(res.data);
      }
    } catch (e) {
      console.error(e);
    }
  };

  const handleDemoCongest = async () => {
    try {
      const res = await axios.post(`${API}/demo/congest?zone=Zone%20A`);
      await refreshSlots();
      alert(res.data + " - Go to Kiosk and book a slot to see rerouting to Zone B!");
    } catch (e) {}
  };

  const handleDemoReset = async () => {
    await axios.post(`${API}/demo/reset`);
    setBooking(null);
    setAllBookings([]);
    await refreshSlots();
  };

  if (isKiosk) {
    return (
      <div>
        <button
          onClick={() => setIsKiosk(false)}
          style={{
            position: "fixed",
            top: "12px",
            right: "12px",
            zIndex: 9999,
            padding: "6px 14px",
            background: "rgba(255,255,255,0.1)",
            border: "1px solid rgba(255,255,255,0.2)",
            borderRadius: "8px",
            color: "#94a3b8",
            fontSize: "12px",
            cursor: "pointer",
            fontFamily: "'Inter', sans-serif",
          }}
        >
          Admin View
        </button>
        <KioskPage />
      </div>
    );
  }

  return (
    <div>
      <div className="topbar">
        <div className="topbar-logo">
          <div className="logo-icon">P</div>
          <div>
            <h1>SmartPark Admin</h1>
            <span>Pune, India</span>
          </div>
        </div>
        <SearchBar api={API} onSelect={setFilterZone} />
        <button
          onClick={() => setIsKiosk(true)}
          style={{
            padding: "6px 14px",
            background: "rgba(0,200,150,0.1)",
            border: "1px solid rgba(0,200,150,0.2)",
            borderRadius: "8px",
            color: "#00c896",
            fontSize: "12px",
            cursor: "pointer",
            fontFamily: "'Inter', sans-serif",
            marginLeft: "8px",
          }}
        >
          Kiosk View
        </button>
        <div className="demo-bar">
          <span className="demo-label">Demo Tools</span>
          <button
            className="btn-demo btn-demo-congest"
            onClick={handleDemoCongest}
          >
            Spam Traffic (Zone A)
          </button>
          <button className="btn-demo btn-demo-reset" onClick={handleDemoReset}>
            Reset Everything
          </button>
        </div>
      </div>

      <div className="main-layout">
        <div className="map-container">
          <MapPanel
            slots={filterZone ? slots.filter((s) => s.zone === filterZone) : slots}
            booking={booking}
            browseMode={false}
            onSlotClick={setSelectedSlot}
          />
        </div>

        <div className="side-panel">
          <div className="panel-section">
            <BookingForm
              onBook={handleBook}
              loading={loading}
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
              <div className="section-title">Manual Force Allocation Result</div>
              <ResultCard booking={booking} />
            </div>
          )}

          {allBookings.length > 0 && (
            <div className="panel-section">
              <div className="section-title">Active Bookings</div>
              <AdminBookings
                bookings={allBookings}
                onCancel={handleAdminCancel}
              />
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
        />
      )}
    </div>
  );
}

function SlotPopup({ slot, loading, onBook, onClose }) {
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
            durationHours: 2,
            vehicleType: slot.type === "EV" ? "EV" : "NORMAL",
          })
        }
      >
        {slot.isOccupied
          ? "Slot Occupied"
          : loading
            ? "Booking..."
            : "Force Book This Slot"}
      </button>
    </div>
  );
}

function SearchBar({ api, onSelect }) {
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
      {query && (
        <span
           style={{cursor:"pointer", color:"#ef4444", marginLeft: "8px", fontSize: "14px"}}
           onClick={() => { setQuery(""); setSuggestions([]); if (onSelect) onSelect(""); }}
        >
          ✕
        </span>
      )}
      {suggestions.length > 0 && (
        <div className="autocomplete-dropdown">
          {suggestions.map((s) => (
            <div
              key={s}
              onClick={() => {
                setQuery(s);
                setSuggestions([]);
                if (onSelect) onSelect(s);
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
