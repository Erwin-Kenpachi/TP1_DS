package pt.ulisboa.lisboapedala.station_availability.persistence;

import pt.ulisboa.lisboapedala.common.DatabaseManager;
import pt.ulisboa.lisboapedala.station_availability.StationOccupancyDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class StationRepository {

    private final DatabaseManager databaseManager;

    public StationRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void saveOrUpdate(StationOccupancyDTO station) {
        String sql = """
            INSERT INTO station_availability_stations (station_id, name, parish, total_docks, available_bikes, free_docks)
            VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(station_id) DO UPDATE SET
                name = excluded.name,
                parish = excluded.parish,
                total_docks = excluded.total_docks,
                available_bikes = excluded.available_bikes,
                free_docks = excluded.free_docks;
            """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, station.stationId());
            stmt.setString(2, station.name());
            stmt.setString(3, station.parish());
            stmt.setInt(4, station.totalDocks());
            stmt.setInt(5, station.availableBikes());
            stmt.setInt(6, station.freeDocks());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao guardar estação " + station.stationId(), e);
        }
    }

    public Optional<StationOccupancyDTO> findById(String stationId) {
        String sql = "SELECT * FROM station_availability_stations WHERE station_id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, stationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new StationOccupancyDTO(
                            rs.getString("station_id"),
                            rs.getString("name"),
                            rs.getString("parish"),
                            rs.getInt("total_docks"),
                            rs.getInt("available_bikes"),
                            rs.getInt("free_docks")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao procurar estação " + stationId, e);
        }
        return Optional.empty();
    }

    public void updateCounters(String stationId, int bikesDelta, int freeDocksDelta) {
        String sql = """
            UPDATE station_availability_stations
            SET available_bikes = MAX(0, available_bikes + ?),
                free_docks = MAX(0, free_docks + ?)
            WHERE station_id = ?
            """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bikesDelta);
            stmt.setInt(2, freeDocksDelta);
            stmt.setString(3, stationId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar contadores da estação " + stationId, e);
        }
    }

    public java.util.List<StationOccupancyDTO> findAll() {
        String sql = "SELECT * FROM station_availability_stations";
        java.util.List<StationOccupancyDTO> stations = new java.util.ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                stations.add(new StationOccupancyDTO(
                        rs.getString("station_id"),
                        rs.getString("name"),
                        rs.getString("parish"),
                        rs.getInt("total_docks"),
                        rs.getInt("available_bikes"),
                        rs.getInt("free_docks")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar todas as estações", e);
        }
        return stations;
    }
}

