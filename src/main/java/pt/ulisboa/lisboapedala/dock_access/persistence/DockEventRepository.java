package pt.ulisboa.lisboapedala.dock_access.persistence;

import pt.ulisboa.lisboapedala.common.DatabaseManager;
import pt.ulisboa.lisboapedala.dock_access.DockEventDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DockEventRepository {

    private final DatabaseManager databaseManager;

    public DockEventRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void save(DockEventDTO event) {
        String sql = """
            INSERT INTO dock_access_events (event_id, station_id, dock_id, bike_id, user_id, event_type, occurred_at, synced)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(event_id) DO UPDATE SET
                synced = excluded.synced;
            """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, event.eventId());
            stmt.setString(2, event.stationId());
            stmt.setString(3, event.dockId());
            stmt.setString(4, event.bikeId());
            stmt.setString(5, event.userId());
            stmt.setString(6, event.eventType());
            stmt.setString(7, event.occurredAt().toString());
            stmt.setInt(8, event.synced() ? 1 : 0);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao guardar evento de doca " + event.eventId(), e);
        }
    }

    public Optional<DockEventDTO> findById(String eventId) {
        String sql = "SELECT * FROM dock_access_events WHERE event_id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, eventId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar evento de doca " + eventId, e);
        }
        return Optional.empty();
    }

    public List<DockEventDTO> findUnsyncedEvents() {
        String sql = "SELECT * FROM dock_access_events WHERE synced = 0 ORDER BY occurred_at ASC";
        List<DockEventDTO> list = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar eventos não sincronizados", e);
        }
        return list;
    }

    public void markSynced(String eventId) {
        String sql = "UPDATE dock_access_events SET synced = 1 WHERE event_id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, eventId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao marcar evento sincronizado " + eventId, e);
        }
    }

    private DockEventDTO mapRow(ResultSet rs) throws SQLException {
        return new DockEventDTO(
                rs.getString("event_id"),
                rs.getString("station_id"),
                rs.getString("dock_id"),
                rs.getString("bike_id"),
                rs.getString("user_id"),
                rs.getString("event_type"),
                LocalDateTime.parse(rs.getString("occurred_at")),
                rs.getInt("synced") == 1
        );
    }
}
