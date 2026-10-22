CREATE TABLE IF NOT EXISTS dock_access_events (
    event_id TEXT PRIMARY KEY,
    station_id TEXT NOT NULL,
    dock_id TEXT NOT NULL,
    bike_id TEXT NOT NULL,
    user_id TEXT,
    event_type TEXT NOT NULL,
    occurred_at TEXT NOT NULL,
    synced INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_dock_events_synced ON dock_access_events(synced);
