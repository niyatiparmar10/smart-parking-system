import { useState, useEffect } from "react";

export default function BookingForm({
  onBook,
  onBrowse,
  onExitBrowse,
  browseMode,
  loading,
  setUserLocation,
  selectedSlot,
}) {
  const [startTime, setStartTime] = useState("11:00");
  const [endTime, setEndTime] = useState("13:00");
  const [vehicleType, setVehicleType] = useState("NORMAL");
  const [locStatus, setLocStatus] = useState("idle");
  const [coords, setCoords] = useState(null);

  useEffect(() => {
    if (selectedSlot) {
      setVehicleType(selectedSlot.type === "EV" ? "EV" : "NORMAL");
    }
  }, [selectedSlot]);

  const toMinutes = (t) => {
    const [h, m] = t.split(":").map(Number);
    return h * 60 + m;
  };

  const getLocation = () => {
    setLocStatus("loading");
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const loc = { lat: pos.coords.latitude, lng: pos.coords.longitude };
        setCoords(loc);
        setUserLocation(loc);
        setLocStatus("detected");
      },
      () => {
        const fallback = { lat: 18.5308, lng: 73.8474 };
        setCoords(fallback);
        setUserLocation(fallback);
        setLocStatus("fallback");
      },
    );
  };

  const handleSubmit = () => {
    const location = coords || { lat: 18.5308, lng: 73.8474 };
    onBook({
      lat: location.lat,
      lng: location.lng,
      startMin: toMinutes(startTime),
      endMin: toMinutes(endTime),
      vehicleType,
    });
  };

  const locLabel = {
    idle: "📍 Use my location",
    loading: "Getting location...",
    detected: "✓ Location detected",
    fallback: "📍 Using Shivajinagar",
  }[locStatus];

  return (
    <div>
      <div className="section-title">Book Parking</div>

      <div className="form-group">
        <label>Your Location</label>
        <button
          className={`loc-btn ${locStatus === "detected" ? "detected" : ""}`}
          onClick={getLocation}
        >
          {locLabel}
        </button>
      </div>

      <div className="time-row">
        <div className="form-group">
          <label>Start</label>
          <input
            type="time"
            value={startTime}
            onChange={(e) => setStartTime(e.target.value)}
          />
        </div>
        <div className="form-group">
          <label>End</label>
          <input
            type="time"
            value={endTime}
            onChange={(e) => setEndTime(e.target.value)}
          />
        </div>
      </div>

      <div className="form-group">
        <label>Vehicle Type</label>
        <div className="vehicle-pills">
          {["NORMAL", "EV", "DISABLED"].map((t) => (
            <button
              key={t}
              className={`vehicle-pill ${vehicleType === t ? "active" : ""}`}
              onClick={() => setVehicleType(t)}
            >
              {t === "NORMAL"
                ? "🚗 Normal"
                : t === "EV"
                  ? "⚡ EV"
                  : "♿ Disabled"}
            </button>
          ))}
        </div>
      </div>

      <button className="btn-book" onClick={handleSubmit} disabled={loading}>
        {loading ? "Finding best slot..." : "Find & Book Slot"}
      </button>

      <button
        className="btn-browse"
        onClick={
          browseMode
            ? onExitBrowse
            : () => {
                const loc = coords || { lat: 18.5308, lng: 73.8474 };
                onBrowse(loc.lat, loc.lng);
              }
        }
      >
        {browseMode ? "✕ Exit Browse Mode" : "🔍 Browse Nearby Slots"}
      </button>
    </div>
  );
}
