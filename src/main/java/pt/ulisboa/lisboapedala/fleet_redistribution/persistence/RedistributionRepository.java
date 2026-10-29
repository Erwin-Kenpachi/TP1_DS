package pt.ulisboa.lisboapedala.fleet_redistribution.persistence;

import pt.ulisboa.lisboapedala.common.DatabaseManager;
import pt.ulisboa.lisboapedala.fleet_redistribution.RedistributionRouteDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositório interno privado de persistência do módulo fleet_redistribution.
 * Interage unicamente com a tabela privada fleet_redistribution_routes.
 */
public class RedistributionRepository {

    private final DatabaseManager databaseManager;

    public RedistributionRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void save(RedistributionRouteDTO route) {
        String sql = """
            INSERT INTO fleet_redistribution_routes (
                route_id, origin_station_id, destination_station_id, bikes_to_move, status, created_at
            ) VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(route_id) DO UPDATE SET
                origin_station_id = excluded.origin_station_id,
                destination_station_id = excluded.destination_station_id,
                bikes_to_move = excluded.bikes_to_move,
                status = excluded.status;
            """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, route.routeId());
            stmt.setString(2, route.originStationId());
            stmt.setString(3, route.destinationStationId());
            stmt.setInt(4, route.bikesToMove());
            stmt.setString(5, route.status());
            stmt.setString(6, route.createdAt());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao guardar rota de redistribuição " + route.routeId(), e);
        }
    }

    public List<RedistributionRouteDTO> findActiveRoutes() {
        String sql = "SELECT * FROM fleet_redistribution_routes WHERE status = 'ACTIVE' ORDER BY created_at DESC";
        List<RedistributionRouteDTO> routes = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                routes.add(mapResultSet(rs));
            }
            return routes;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar rotas ativas de redistribuição", e);
        }
    }

    public List<RedistributionRouteDTO> findAll() {
        String sql = "SELECT * FROM fleet_redistribution_routes ORDER BY created_at DESC";
        List<RedistributionRouteDTO> routes = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                routes.add(mapResultSet(rs));
            }
            return routes;
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar todas as rotas de redistribuição", e);
        }
    }

    public Optional<RedistributionRouteDTO> findById(String routeId) {
        String sql = "SELECT * FROM fleet_redistribution_routes WHERE route_id = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, routeId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar rota " + routeId, e);
        }
    }

    private RedistributionRouteDTO mapResultSet(ResultSet rs) throws SQLException {
        return new RedistributionRouteDTO(
                rs.getString("route_id"),
                rs.getString("origin_station_id"),
                rs.getString("destination_station_id"),
                rs.getInt("bikes_to_move"),
                rs.getString("status"),
                rs.getString("created_at")
        );
    }
}

