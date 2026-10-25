package pt.ulisboa.lisboapedala.maintenance_orders.persistence;

import pt.ulisboa.lisboapedala.common.DatabaseManager;
import pt.ulisboa.lisboapedala.maintenance_orders.MaintenanceOrderDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositório interno privado de persistência do módulo maintenance_orders.
 * Interage unicamente com a tabela privada maintenance_orders_tickets.
 */
public class MaintenanceOrderRepository {

    private final DatabaseManager databaseManager;

    public MaintenanceOrderRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void save(MaintenanceOrderDTO order) {
        String sql = """
            INSERT INTO maintenance_orders_tickets (
                order_id, bike_id, description, priority, status,
                technician_id, anomaly_id, created_at, resolved_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(order_id) DO UPDATE SET
                bike_id = excluded.bike_id,
                description = excluded.description,
                priority = excluded.priority,
                status = excluded.status,
                technician_id = excluded.technician_id,
                anomaly_id = excluded.anomaly_id,
                resolved_at = excluded.resolved_at;
            """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, order.orderId());
            stmt.setString(2, order.bikeId());
            stmt.setString(3, order.description());
            stmt.setString(4, order.priority());
            stmt.setString(5, order.status());
            stmt.setString(6, order.technicianId());
            stmt.setString(7, order.anomalyId());
            stmt.setString(8, order.createdAt().toString());
            stmt.setString(9, order.resolvedAt() != null ? order.resolvedAt().toString() : null);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao guardar ordem de manutenção " + order.orderId(), e);
        }
    }

    public Optional<MaintenanceOrderDTO> findById(String orderId) {
        String sql = "SELECT * FROM maintenance_orders_tickets WHERE order_id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar ordem de manutenção " + orderId, e);
        }
        return Optional.empty();
    }

    public Optional<MaintenanceOrderDTO> findByAnomalyId(String anomalyId) {
        if (anomalyId == null) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM maintenance_orders_tickets WHERE anomaly_id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, anomalyId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar ordem por anomalia " + anomalyId, e);
        }
        return Optional.empty();
    }

    public List<MaintenanceOrderDTO> findPendingOrders() {
        String sql = "SELECT * FROM maintenance_orders_tickets WHERE status != 'RESOLVED' ORDER BY created_at ASC";
        List<MaintenanceOrderDTO> list = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar ordens pendentes", e);
        }
        return list;
    }

    public List<MaintenanceOrderDTO> findByTechnician(String technicianId) {
        String sql = "SELECT * FROM maintenance_orders_tickets WHERE technician_id = ? ORDER BY CASE priority WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END, created_at ASC";
        List<MaintenanceOrderDTO> list = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, technicianId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar ordens para técnico " + technicianId, e);
        }
        return list;
    }

    public List<MaintenanceOrderDTO> findAll() {
        String sql = "SELECT * FROM maintenance_orders_tickets ORDER BY created_at DESC";
        List<MaintenanceOrderDTO> list = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar todas as ordens de manutenção", e);
        }
        return list;
    }

    private MaintenanceOrderDTO mapRow(ResultSet rs) throws SQLException {
        String resolvedAtStr = rs.getString("resolved_at");
        return new MaintenanceOrderDTO(
                rs.getString("order_id"),
                rs.getString("bike_id"),
                rs.getString("description"),
                rs.getString("priority"),
                rs.getString("status"),
                rs.getString("technician_id"),
                rs.getString("anomaly_id"),
                Instant.parse(rs.getString("created_at")),
                resolvedAtStr != null ? Instant.parse(resolvedAtStr) : null
        );
    }
}
