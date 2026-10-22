CREATE TABLE IF NOT EXISTS tariff_billing_invoices (
    invoice_id TEXT PRIMARY KEY,
    trip_id TEXT NOT NULL UNIQUE,
    user_id TEXT NOT NULL,
    amount_cents INTEGER NOT NULL,
    tax_cents INTEGER NOT NULL,
    payment_method TEXT NOT NULL,
    status TEXT NOT NULL,
    at_fiscal_code TEXT NOT NULL,
    issued_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_invoices_user_id ON tariff_billing_invoices(user_id);
