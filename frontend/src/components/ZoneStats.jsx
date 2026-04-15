const ZONES = [
  "Koregaon Park",
  "Shivajinagar",
  "FC Road",
  "Kothrud",
  "Hadapsar",
  "Viman Nagar",
  "Aundh",
  "Baner",
  "Camp",
  "Deccan",
];

export default function ZoneStats({ slots }) {
  // Count free and total per zone
  const stats = ZONES.map((zone) => {
    const zoneSlots = slots.filter((s) => s.zone === zone);
    const total = zoneSlots.length;
    const free = zoneSlots.filter((s) => !s.isOccupied).length;
    const pct = total > 0 ? ((total - free) / total) * 100 : 0;
    return { zone, free, total, pct };
  });

  const barColor = (pct) => {
    if (pct >= 80) return "#ef4444"; // red = congested
    if (pct >= 50) return "#f59e0b"; // orange = filling up
    return "#22c55e"; // green = available
  };

  return (
    <div>
      <h2 style={{ marginBottom: "10px" }}>Zone Availability</h2>
      <div className="zone-stats">
        {stats.map((s) => (
          <div className="zone-row" key={s.zone}>
            <span className="zone-name">{s.zone}</span>
            <div className="zone-bar-wrap">
              <div className="zone-bar">
                <div
                  className="zone-bar-fill"
                  style={{
                    width: `${s.pct}%`,
                    background: barColor(s.pct),
                  }}
                />
              </div>
              <span
                style={{ fontSize: "11px", color: "#94a3b8", minWidth: "32px" }}
              >
                {s.free}/{s.total}
              </span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
