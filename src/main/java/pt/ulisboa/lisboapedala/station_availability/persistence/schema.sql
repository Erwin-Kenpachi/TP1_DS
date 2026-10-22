CREATE TABLE IF NOT EXISTS station_availability_stations (
    station_id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    parish TEXT NOT NULL,
    total_docks INTEGER NOT NULL,
    available_bikes INTEGER NOT NULL,
    free_docks INTEGER NOT NULL
);
