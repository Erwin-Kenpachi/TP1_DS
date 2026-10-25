package pt.ulisboa.lisboapedala.sensor_ingestion.persistence;

import pt.ulisboa.lisboapedala.common.DatabaseManager;
import pt.ulisboa.lisboapedala.sensor_ingestion.BikeAnomalyEventDTO;
import pt.ulisboa.lisboapedala.sensor_ingestion.TelemetryDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositório de persistência privado do módulo sensor_ingestion.
 * Responsável por registar leituras de telemetria e o histórico de anomalias detetadas.
 */
public class TelemetryLogRepository {

    private final DatabaseManager databaseManager;

    public TelemetryLogRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void saveTelemetry(TelemetryDTO telemetry) {
        String sql = """
            INSERT INTO sensor_ingestion_telemetry (bike_id, battery_level, latitude, longitude, speed_kmh, brake_status, timestamp)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, telemetry.bikeId());
            stmt.setDouble(2, telemetry.batteryLevel());
            stmt.setDouble(3, telemetry.latitude());
            stmt.setDouble(4, telemetry.longitude());
            stmt.setDouble(5, telemetry.speedKmh());
            stmt.setString(6, telemetry.brakeStatus());
            stmt.setString(7, telemetry.timestamp().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao persistir telemetria da bicicleta " + telemetry.bikeId(), e);
        }
    }

    public Optional<TelemetryDTO> findLatestByBikeId(String bikeId) {
        String sql = """
            SELECT * FROM sensor_ingestion_telemetry
            WHERE bike_id = ?
            ORDER BY id DESC LIMIT 1
            """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, bikeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapTelemetryRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar telemetria da bicicleta " + bikeId, e);
        }
        return Optional.empty();
    }

    public void saveAnomaly(BikeAnomalyEventDTO anomaly) {
        String sql = """
            INSERT INTO sensor_ingestion_anomalies (anomaly_id, bike_id, anomaly_type, description, timestamp, resolved)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(anomaly_id) DO UPDATE SET
                resolved = excluded.resolved;
            """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, anomaly.anomalyId());
            stmt.setString(2, anomaly.bikeId());
            stmt.setString(3, anomaly.anomalyType());
            stmt.setString(4, anomaly.description());
            stmt.setString(5, anomaly.timestamp().toString());
            stmt.setInt(6, anomaly.resolved() ? 1 : 0);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao guardar anomalia " + anomaly.anomalyId(), e);
        }
    }

    public List<BikeAnomalyEventDTO> findUnresolvedAnomalies() {
        String sql = """
            SELECT * FROM sensor_ingestion_anomalies
            WHERE resolved = 0
            ORDER BY timestamp DESC
            """;
        List<BikeAnomalyEventDTO> result = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                result.add(mapAnomalyRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar anomalias pendentes", e);
        }
        return result;
    }

    public List<BikeAnomalyEventDTO> findUnresolvedAnomaliesByBikeId(String bikeId) {
        String sql = """
            SELECT * FROM sensor_ingestion_anomalies
            WHERE bike_id = ? AND resolved = 0
            ORDER BY timestamp DESC
            """;
        List<BikeAnomalyEventDTO> result = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, bikeId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapAnomalyRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar anomalias ativas da bicicleta " + bikeId, e);
        }
        return result;
    }

    public void markAnomalyResolved(String anomalyId) {
        String sql = "UPDATE sensor_ingestion_anomalies SET resolved = 1 WHERE anomaly_id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, anomalyId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao resolver anomalia " + anomalyId, e);
        }
    }

    private TelemetryDTO mapTelemetryRow(ResultSet rs) throws SQLException {
        return new TelemetryDTO(
                rs.getString("bike_id"),
                rs.getDouble("battery_level"),
                rs.getDouble("latitude"),
                rs.getDouble("longitude"),
                rs.getDouble("speed_kmh"),
                rs.getString("brake_status"),
                Instant.parse(rs.getString("timestamp"))
        );
    }

    private BikeAnomalyEventDTO mapAnomalyRow(ResultSet rs) throws SQLException {
        return new BikeAnomalyEventDTO(
                rs.getString("anomaly_id"),
                rs.getString("bike_id"),
                rs.getString("anomaly_type"),
                rs.getString("description"),
                Instant.parse(rs.getString("timestamp")),
                rs.getInt("resolved") == 1
        );
    }
}
