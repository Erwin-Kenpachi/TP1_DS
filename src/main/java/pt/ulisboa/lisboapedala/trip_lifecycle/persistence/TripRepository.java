package pt.ulisboa.lisboapedala.trip_lifecycle.persistence;

import pt.ulisboa.lisboapedala.common.DatabaseManager;
import pt.ulisboa.lisboapedala.trip_lifecycle.TripDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TripRepository {

    private final DatabaseManager databaseManager;

    public TripRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void save(TripDTO trip) {
        String sql = """
            INSERT INTO trip_lifecycle_trips (
                trip_id, user_id, bike_id, start_station_id, end_station_id,
                start_time, end_time, duration_minutes, status, return_event_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ? ,?)
            ON CONFLICT(trip_id) DO UPDATE SET
                end_station_id = excluded.end_station_id,
                end_time = excluded.end_time,
                duration_minutes = excluded.duration_minutes,
                status = excluded.status,
                return_event_id = excluded.return_event_id;
            """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, trip.tripId());
            stmt.setString(2, trip.userId());
            stmt.setString(3, trip.bikeId());
            stmt.setString(4, trip.startStationId());
            stmt.setString(5, trip.endStationId());
            stmt.setString(6, trip.startTime().toString());
            stmt.setString(7, trip.endTime() != null ? trip.endTime().toString() : null);
            if (trip.durationMinutes() != null) {
                stmt.setLong(8, trip.durationMinutes());
            } else {
                stmt.setNull(8, java.sql.Types.INTEGER);
            }
            stmt.setString(9, trip.status());
            stmt.setString(10, trip.returnEventId());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao guardar viagem " + trip.tripId(), e);
        }
    }

    public Optional<TripDTO> findById(String tripId) {
        String sql = "SELECT * FROM trip_lifecycle_trips WHERE trip_id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tripId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar viagem " + tripId, e);
        }
        return Optional.empty();
    }

    public Optional<TripDTO> findByReturnEventId(String returnEventId) {
        if (returnEventId == null) {
            return Optional.empty();
        }
        String sql = "SELECT * FROM trip_lifecycle_trips WHERE return_event_id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, returnEventId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar viagem por returnEventId " + returnEventId, e);
        }
        return Optional.empty();
    }

    public Optional<TripDTO> findActiveTripByUser(String userId) {
        String sql = "SELECT * FROM trip_lifecycle_trips WHERE user_id = ? AND status = 'IN_PROGRESS'";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao procurar viagem ativa para utilizador " + userId, e);
        }
        return Optional.empty();
    }

    public Optional<TripDTO> findActiveTripByBike(String bikeId) {
        String sql = "SELECT * FROM trip_lifecycle_trips WHERE bike_id = ? AND status = 'IN_PROGRESS'";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, bikeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao procurar viagem ativa para bicicleta " + bikeId, e);
        }
        return Optional.empty();
    }

    public List<TripDTO> findCompletedTripsBetween(LocalDateTime start, LocalDateTime end) {
        String sql = "SELECT * FROM trip_lifecycle_trips WHERE status = 'COMPLETED' AND start_time >= ? AND start_time <= ?";
        List<TripDTO> list = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, start.toString());
            stmt.setString(2, end.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao procurar viagens por período", e);
        }
        return list;
    }

    private TripDTO mapRow(ResultSet rs) throws SQLException {
        String endTimeStr = rs.getString("end_time");
        Long duration = rs.getObject("duration_minutes") != null ? rs.getLong("duration_minutes") : null;
        return new TripDTO(
                rs.getString("trip_id"),
                rs.getString("user_id"),
                rs.getString("bike_id"),
                rs.getString("start_station_id"),
                rs.getString("end_station_id"),
                LocalDateTime.parse(rs.getString("start_time")),
                endTimeStr != null ? LocalDateTime.parse(endTimeStr) : null,
                duration,
                rs.getString("status"),
                rs.getString("return_event_id")
        );
    }
}
