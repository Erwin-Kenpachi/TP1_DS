-- Tabela privada de telemetria IoT do módulo sensor_ingestion
-- Conforme ADR-006 e RGPD: desvinculada de dados pessoais de utilizador (apenas identifica a bicicleta)
CREATE TABLE IF NOT EXISTS sensor_ingestion_telemetry (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    bike_id TEXT NOT NULL,
    battery_level REAL NOT NULL,
    latitude REAL,
    longitude REAL,
    speed_kmh REAL,
    brake_status TEXT NOT NULL,
    timestamp TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sensor_telemetry_bike ON sensor_ingestion_telemetry(bike_id, timestamp);

-- Tabela privada para anomalias detetadas (bateria crítica, avaria de travões, etc.)
CREATE TABLE IF NOT EXISTS sensor_ingestion_anomalies (
    anomaly_id TEXT PRIMARY KEY,
    bike_id TEXT NOT NULL,
    anomaly_type TEXT NOT NULL,
    description TEXT NOT NULL,
    timestamp TEXT NOT NULL,
    resolved INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_sensor_anomalies_bike ON sensor_ingestion_anomalies(bike_id, resolved);
