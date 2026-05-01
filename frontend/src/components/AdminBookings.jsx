export default function AdminBookings({ bookings, onCancel }) {
  return (
    <div>
      {bookings.map((slot) => (
        <div key={slot.slotId} className="booking-item">
          <div className="booking-item-left">
            <div className="booking-item-slot">{slot.slotId}</div>
            <div className="booking-item-detail">
              {slot.zone} · {slot.type} · ₹{slot.pricePerHour}/hr
            </div>
          </div>
          <button className="btn-cancel" onClick={() => onCancel(slot)}>
            Cancel
          </button>
        </div>
      ))}
    </div>
  );
}
