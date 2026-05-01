import { useState, useEffect } from "react";

export default function BookingForm({
  onBook,
  loading,
  selectedSlot,
}) {
  const [durationHours, setDurationHours] = useState(2);
  const [vehicleType, setVehicleType] = useState("NORMAL");

  useEffect(() => {
    if (selectedSlot) {
      setVehicleType(selectedSlot.type === "EV" ? "EV" : "NORMAL");
    }
  }, [selectedSlot]);

  const handleSubmit = () => {
    onBook({
      durationHours: Number(durationHours),
      vehicleType,
    });
  };

  return (
    <div>
      <div className="section-title">Manual Booking (Admin)</div>

      <div className="form-group">
        <label>Expected Duration (Hours)</label>
        <select
          value={durationHours}
          onChange={(e) => setDurationHours(e.target.value)}
          style={{ width: "100%", padding: "10px", borderRadius: "8px", background: "rgba(255,255,255,0.05)", color: "#f0f4ff", border: "1px solid rgba(255,255,255,0.1)" }}
        >
          <option value={1}>1 Hour</option>
          <option value={2}>2 Hours</option>
          <option value={3}>3 Hours</option>
          <option value={4}>4 Hours</option>
          <option value={5}>5 Hours</option>
          <option value={6}>6+ Hours</option>
        </select>
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
        {loading ? "Allocating slot..." : "Force Allocate Slot"}
      </button>
    </div>
  );
}
