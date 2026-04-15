export default function MyBookings({ bookings, onCancel }) {
  const fmt = (m) => {
    const h = Math.floor(m / 60);
    const min = m % 60;
    const ampm = h >= 12 ? "PM" : "AM";
    const h12 = h % 12 || 12;
    return `${h12}:${String(min).padStart(2, "0")} ${ampm}`;
  };

  return (
    <div>
      {bookings.map((b, i) => (
        <div key={i} className="booking-item">
          <div className="booking-item-left">
            <div className="booking-item-slot">{b.slotId}</div>
            <div className="booking-item-detail">
              {b.zone} · {fmt(b.startMin)}–{fmt(b.endMin)}
            </div>
          </div>
          <button className="btn-cancel" onClick={() => onCancel(b)}>
            Cancel
          </button>
        </div>
      ))}
    </div>
  );
}
