-- Tabela privada do módulo reporting_audit para histórico de relatórios municipais
CREATE TABLE IF NOT EXISTS reporting_audit_monthly_reports (
    report_id TEXT PRIMARY KEY,
    report_year INTEGER NOT NULL,
    report_month INTEGER NOT NULL,
    generated_at TEXT NOT NULL,
    total_trips INTEGER NOT NULL,
    total_duration_minutes INTEGER NOT NULL,
    estimated_distance_km REAL NOT NULL,
    total_co2_saved_kg REAL NOT NULL,
    report_json TEXT NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_reporting_year_month ON reporting_audit_monthly_reports(report_year, report_month);
