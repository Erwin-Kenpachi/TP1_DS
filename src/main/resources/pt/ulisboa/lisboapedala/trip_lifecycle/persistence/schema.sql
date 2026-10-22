CREATE TABLE IF NOT EXISTS trip_lifecycle_trips (
    trip_id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    bike_id TEXT NOT NULL,
    start_station_id TEXT NOT NULL,
    end_station_id TEXT,
    start_time TEXT NOT NULL,
    end_time TEXT,
    duration_minutes INTEGER,
    status TEXT NOT NULL,
    return_event_id TEXT UNIQUE
);

CREATE INDEX IF NOT EXISTS idx_trips_user_id ON trip_lifecycle_trips(user_id);
CREATE INDEX IF NOT EXISTS idx_trips_status ON trip_lifecycle_trips(status);
