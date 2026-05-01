import { useState } from "react";
import axios from "axios";

const API = "http://localhost:8080/api";

export default function KioskPage() {
  const [step, setStep] = useState("idle"); // idle → loading → assigned
  const [booking, setBooking] = useState(null);
  const [vehicleType, setVehicleType] = useState("NORMAL");
  const [durationHours, setDurationHours] = useState(2);

  const handleFindParking = async () => {
    setStep("loading");
    try {
      const res = await axios.post(`${API}/book`, {
        durationHours: Number(durationHours),
        vehicleType,
      });

      if (res.data.message === "success") {
        setBooking(res.data);
        setStep("assigned");
      } else {
        setStep("error");
      }
    } catch {
      setStep("error");
    }
  };

  const handleReset = () => {
    setStep("idle");
    setBooking(null);
  };

  return (
    <div style={styles.page}>
      {/* Header */}
      <div style={styles.header}>
        <div style={styles.headerIcon}>🅿</div>
        <div>
          <div style={styles.headerTitle}>ParkSmart Mall</div>
          <div style={styles.headerSub}>Automated Parking System</div>
        </div>
      </div>

      {/* Main content */}
      <div style={styles.card}>
        {step === "idle" && (
          <>
            <div style={styles.welcomeIcon}>🚗</div>
            <div style={styles.welcomeTitle}>Welcome!</div>
            <div style={styles.welcomeSub}>
              Select your vehicle type and parking duration.
            </div>

            <div style={styles.pillRow}>
              {["NORMAL", "EV", "DISABLED"].map((t) => (
                <button
                  key={t}
                  style={{
                    ...styles.pill,
                    ...(vehicleType === t ? styles.pillActive : {}),
                  }}
                  onClick={() => setVehicleType(t)}
                >
                  {t === "NORMAL"
                    ? "🚗 Normal"
                    : t === "EV"
                      ? "⚡ Electric"
                      : "♿ Accessible"}
                </button>
              ))}
            </div>

            <div style={{width: '100%', marginTop: '10px'}}>
              <div style={{fontSize: '13px', color: '#94a3b8', marginBottom: '8px', textAlign: 'center'}}>
                How long do you plan to stay?
              </div>
              <select 
                value={durationHours} 
                onChange={e => setDurationHours(e.target.value)}
                style={{
                  width: '100%', padding: '14px', borderRadius: '12px', background: 'rgba(255,255,255,0.05)', 
                  color: '#fff', border: '1px solid rgba(255,255,255,0.1)', cursor: 'pointer', fontFamily: "'Inter', sans-serif"
                }}
              >
                <option value={1} style={{color: '#000'}}>1 Hour</option>
                <option value={2} style={{color: '#000'}}>2 Hours</option>
                <option value={3} style={{color: '#000'}}>3 Hours</option>
                <option value={4} style={{color: '#000'}}>4 Hours</option>
                <option value={5} style={{color: '#000'}}>5 Hours</option>
                <option value={6} style={{color: '#000'}}>6+ Hours</option>
              </select>
            </div>

            <button style={styles.mainBtn} onClick={handleFindParking}>
              Find Me a Parking Spot
            </button>
          </>
        )}

        {step === "loading" && (
          <>
            <div style={styles.loadingIcon}>⏳</div>
            <div style={styles.welcomeTitle}>Allocating spot...</div>
            <div style={styles.welcomeSub}>
              Our system is assigning the best open zone.
            </div>
          </>
        )}

        {step === "assigned" && booking && (
          <>
             <div style={styles.successIcon}>✅</div>
            <div style={styles.slotId}>{booking.slotId}</div>
            <div style={styles.zoneName}>{booking.zone}</div>

            <div style={styles.detailsGrid}>
              <div style={styles.detailBox}>
                <div style={styles.detailLabel}>Expires In</div>
                <div style={styles.detailValue}>
                  {durationHours} hr
                </div>
              </div>
              <div style={styles.detailBox}>
                <div style={styles.detailLabel}>Rate</div>
                <div style={styles.detailValue}>₹{booking.pricePerHour}/hr</div>
              </div>
            </div>

            <div style={styles.instruction}>
              Please proceed to <strong>{booking.zone}</strong> and park at slot{" "}
              <strong>{booking.slotId}</strong>
            </div>

            <button style={styles.secondaryBtn} onClick={handleReset}>
              ← Back to Home
            </button>
          </>
        )}

        {step === "error" && (
          <>
            <div style={styles.welcomeIcon}>😕</div>
            <div style={styles.welcomeTitle}>No spots available</div>
            <div style={styles.welcomeSub}>
              All parking zones are currently full. Please try again later.
            </div>
            <button style={styles.mainBtn} onClick={handleReset}>
              Try Again
            </button>
          </>
        )}
      </div>

      {/* Footer */}
      <div style={styles.footer}>Powered by SmartPark Queue System</div>
    </div>
  );
}

const styles = {
  page: {
    minHeight: "100vh",
    background: "linear-gradient(135deg, #0a0f1e 0%, #0d1f3c 100%)",
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    justifyContent: "space-between",
    padding: "40px 20px",
    fontFamily: "'Inter', sans-serif",
    color: "#f0f4ff",
  },
  header: {
    display: "flex",
    alignItems: "center",
    gap: "16px",
    marginBottom: "20px",
  },
  headerIcon: {
    width: "56px",
    height: "56px",
    background: "linear-gradient(135deg, #00c896, #0080ff)",
    borderRadius: "14px",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    fontSize: "28px",
    fontWeight: "800",
    color: "white",
  },
  headerTitle: {
    fontSize: "24px",
    fontWeight: "800",
    letterSpacing: "-0.5px",
  },
  headerSub: {
    fontSize: "13px",
    color: "#00c896",
    fontWeight: "500",
  },
  card: {
    background: "rgba(255,255,255,0.04)",
    border: "1px solid rgba(255,255,255,0.08)",
    borderRadius: "24px",
    padding: "48px 40px",
    width: "100%",
    maxWidth: "480px",
    display: "flex",
    flexDirection: "column",
    alignItems: "center",
    gap: "20px",
    backdropFilter: "blur(12px)",
  },
  welcomeIcon: { fontSize: "64px" },
  welcomeTitle: {
    fontSize: "28px",
    fontWeight: "800",
    textAlign: "center",
    letterSpacing: "-0.5px",
  },
  welcomeSub: {
    fontSize: "15px",
    color: "#94a3b8",
    textAlign: "center",
    lineHeight: "1.6",
  },
  pillRow: {
    display: "flex",
    gap: "10px",
    flexWrap: "wrap",
    justifyContent: "center",
  },
  pill: {
    padding: "10px 20px",
    background: "rgba(255,255,255,0.05)",
    border: "1px solid rgba(255,255,255,0.1)",
    borderRadius: "100px",
    color: "#94a3b8",
    fontSize: "14px",
    fontWeight: "600",
    cursor: "pointer",
    fontFamily: "'Inter', sans-serif",
    transition: "all 0.2s",
  },
  pillActive: {
    background: "rgba(0,200,150,0.15)",
    border: "1px solid #00c896",
    color: "#00c896",
  },
  mainBtn: {
    width: "100%",
    padding: "18px",
    background: "linear-gradient(135deg, #00c896, #0080ff)",
    border: "none",
    borderRadius: "14px",
    color: "white",
    fontSize: "18px",
    fontWeight: "700",
    cursor: "pointer",
    fontFamily: "'Inter', sans-serif",
    letterSpacing: "-0.3px",
  },
  loadingIcon: { fontSize: "64px" },
  successIcon: { fontSize: "56px" },
  slotId: {
    fontSize: "64px",
    fontWeight: "900",
    color: "#00c896",
    letterSpacing: "-2px",
  },
  zoneName: {
    fontSize: "20px",
    fontWeight: "600",
    color: "#94a3b8",
  },
  detailsGrid: {
    display: "grid",
    gridTemplateColumns: "1fr 1fr",
    gap: "12px",
    width: "100%",
  },
  detailBox: {
    background: "rgba(255,255,255,0.04)",
    borderRadius: "12px",
    padding: "16px",
    textAlign: "center",
  },
  detailLabel: {
    fontSize: "11px",
    color: "#4a5568",
    fontWeight: "600",
    textTransform: "uppercase",
    letterSpacing: "0.5px",
    marginBottom: "6px",
  },
  detailValue: {
    fontSize: "20px",
    fontWeight: "800",
    color: "#f0f4ff",
  },
  instruction: {
    fontSize: "15px",
    color: "#94a3b8",
    textAlign: "center",
    lineHeight: "1.6",
    background: "rgba(0,200,150,0.06)",
    border: "1px solid rgba(0,200,150,0.15)",
    borderRadius: "12px",
    padding: "16px",
    width: "100%",
  },
  secondaryBtn: {
    padding: "12px 24px",
    background: "transparent",
    border: "1px solid rgba(255,255,255,0.1)",
    borderRadius: "10px",
    color: "#64748b",
    fontSize: "14px",
    cursor: "pointer",
    fontFamily: "'Inter', sans-serif",
  },
  reallocBanner: {
    background: "rgba(0,128,255,0.1)",
    border: "1px solid rgba(0,128,255,0.25)",
    borderRadius: "10px",
    padding: "12px 16px",
    fontSize: "13px",
    color: "#60a5fa",
    width: "100%",
    textAlign: "center",
  },
  footer: {
    fontSize: "12px",
    color: "#334155",
    marginTop: "20px",
  },
};
