-- Tabela privada do módulo fleet_redistribution para registo e planeamento de rotas de redistribuição
CREATE TABLE IF NOT EXISTS fleet_redistribution_routes (
    route_id TEXT PRIMARY KEY,
    origin_station_id TEXT NOT NULL,
    destination_station_id TEXT NOT NULL,
    bikes_to_move INTEGER NOT NULL,
    status TEXT NOT NULL,
    created_at TEXT NOT NULL
);

