-- Tabela privada de tickets de manutenção do módulo maintenance_orders (US-2)
-- Em conformidade com ADR-002, ADR-005 e ARCHITECTURE.md:
-- Isolamento de dados por domínio, com chaves externas tratadas como identificadores lógicos.

CREATE TABLE IF NOT EXISTS maintenance_orders_tickets (
    order_id TEXT PRIMARY KEY,
    bike_id TEXT NOT NULL,
    description TEXT NOT NULL,
    priority TEXT NOT NULL,
    status TEXT NOT NULL,
    technician_id TEXT,
    anomaly_id TEXT,
    created_at TEXT NOT NULL,
    resolved_at TEXT
);

CREATE INDEX IF NOT EXISTS idx_maint_orders_bike ON maintenance_orders_tickets(bike_id);
CREATE INDEX IF NOT EXISTS idx_maint_orders_status ON maintenance_orders_tickets(status);
CREATE INDEX IF NOT EXISTS idx_maint_orders_tech ON maintenance_orders_tickets(technician_id);
CREATE INDEX IF NOT EXISTS idx_maint_orders_anomaly ON maintenance_orders_tickets(anomaly_id);
