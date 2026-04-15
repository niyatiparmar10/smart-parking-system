export default function ResultCard({ booking }) {
  const walk = Math.round(booking.distanceMeters / 80);
  return (
    <div className="result-card">
      <div className="result-header">
        <div>
          <div className="result-slot-id">{booking.slotId}</div>
          <div className="result-zone">{booking.zone}</div>
        </div>
        <div>
          <div className="result-price">
            ₹{booking.pricePerHour}
            <span>/hr</span>
          </div>
        </div>
      </div>
      <div className="result-stats">
        <div className="result-stat">
          <div className="stat-label">Distance</div>
          <div className="stat-value">
            {Math.round(booking.distanceMeters)}m
          </div>
        </div>
        <div className="result-stat">
          <div className="stat-label">Walk Time</div>
          <div className="stat-value">~{walk} min</div>
        </div>
      </div>
    </div>
  );
}
