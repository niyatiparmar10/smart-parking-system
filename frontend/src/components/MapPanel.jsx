import {
  MapContainer,
  TileLayer,
  CircleMarker,
  Popup,
  useMap,
} from "react-leaflet";
import "leaflet/dist/leaflet.css";

const PUNE_CENTER = [18.5204, 73.8567];

function markerColor(slot, booking, browseMode) {
  if (booking && slot.slotId === booking.slotId) return "#0080ff";
  if (slot.isOccupied) return "#ef4444";
  if (browseMode) return "#a78bfa";
  return "#00c896";
}

export default function MapPanel({
  slots,
  booking,
  userLocation,
  browseMode,
  onSlotClick,
}) {
  return (
    <MapContainer
      center={PUNE_CENTER}
      zoom={13}
      style={{ height: "100%", width: "100%" }}
    >
      <TileLayer
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        attribution="© OpenStreetMap"
      />
      {userLocation && (
        <CircleMarker
          center={[userLocation.lat, userLocation.lng]}
          radius={9}
          fillColor="#f59e0b"
          color="white"
          weight={2}
          fillOpacity={1}
        />
      )}
      {slots.map((slot) => (
        <CircleMarker
          key={slot.slotId}
          center={[slot.lat, slot.lng]}
          radius={booking?.slotId === slot.slotId ? 11 : 7}
          fillColor={markerColor(slot, booking, browseMode)}
          color={markerColor(slot, booking, browseMode)}
          weight={booking?.slotId === slot.slotId ? 3 : 1}
          fillOpacity={0.9}
          eventHandlers={{ click: () => onSlotClick(slot) }}
        >
          <Popup>
            <div className="map-popup">
              <div className="map-popup-id">{slot.slotId}</div>
              <div className="map-popup-zone">{slot.zone}</div>
              <div className="map-popup-row">
                <span>Type</span>
                <span>{slot.type}</span>
              </div>
              <div className="map-popup-row">
                <span>Price</span>
                <span>₹{slot.pricePerHour}/hr</span>
              </div>
              <div className="map-popup-row">
                <span>Status</span>
                <span
                  style={{ color: slot.isOccupied ? "#ef4444" : "#00c896" }}
                >
                  {slot.isOccupied ? "Occupied" : "Available"}
                </span>
              </div>
            </div>
          </Popup>
        </CircleMarker>
      ))}
      {booking && <FlyTo lat={booking.lat} lng={booking.lng} />}
    </MapContainer>
  );
}

function FlyTo({ lat, lng }) {
  const map = useMap();
  map.flyTo([lat, lng], 15, { duration: 1.5 });
  return null;
}
