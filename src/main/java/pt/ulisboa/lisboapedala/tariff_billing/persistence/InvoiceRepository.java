package pt.ulisboa.lisboapedala.tariff_billing.persistence;

import pt.ulisboa.lisboapedala.common.DatabaseManager;
import pt.ulisboa.lisboapedala.tariff_billing.InvoiceDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InvoiceRepository {

    private final DatabaseManager databaseManager;

    public InvoiceRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void save(InvoiceDTO invoice) {
        String sql = """
            INSERT INTO tariff_billing_invoices (
                invoice_id, trip_id, user_id, amount_cents, tax_cents, payment_method, status, at_fiscal_code, issued_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
            """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, invoice.invoiceId());
            stmt.setString(2, invoice.tripId());
            stmt.setString(3, invoice.userId());
            stmt.setInt(4, invoice.amountCents());
            stmt.setInt(5, invoice.taxCents());
            stmt.setString(6, invoice.paymentMethod());
            stmt.setString(7, invoice.status());
            stmt.setString(8, invoice.atFiscalCode());
            stmt.setString(9, invoice.issuedAt());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao gravar fatura " + invoice.invoiceId(), e);
        }
    }

    public Optional<InvoiceDTO> findByTripId(String tripId) {
        String sql = "SELECT * FROM tariff_billing_invoices WHERE trip_id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tripId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar fatura por tripId " + tripId, e);
        }
        return Optional.empty();
    }

    public Optional<InvoiceDTO> findById(String invoiceId) {
        String sql = "SELECT * FROM tariff_billing_invoices WHERE invoice_id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, invoiceId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar fatura " + invoiceId, e);
        }
        return Optional.empty();
    }

    public List<InvoiceDTO> findByUserId(String userId) {
        String sql = "SELECT * FROM tariff_billing_invoices WHERE user_id = ? ORDER BY issued_at DESC";
        List<InvoiceDTO> list = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar faturas do utilizador " + userId, e);
        }
        return list;
    }

    private InvoiceDTO mapRow(ResultSet rs) throws SQLException {
        return new InvoiceDTO(
                rs.getString("invoice_id"),
                rs.getString("trip_id"),
                rs.getString("user_id"),
                rs.getInt("amount_cents"),
                rs.getInt("tax_cents"),
                rs.getString("payment_method"),
                rs.getString("status"),
                rs.getString("at_fiscal_code"),
                rs.getString("issued_at")
        );
    }
}
