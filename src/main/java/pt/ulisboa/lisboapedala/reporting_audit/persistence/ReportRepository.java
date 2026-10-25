package pt.ulisboa.lisboapedala.reporting_audit.persistence;

import pt.ulisboa.lisboapedala.common.DatabaseManager;
import pt.ulisboa.lisboapedala.reporting_audit.MonthlyMunicipalReportDTO;
import pt.ulisboa.lisboapedala.reporting_audit.ParishUsageReportDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Repositório de persistência privado do módulo reporting_audit.
 * Guarda relatórios consolidados para auditoria municipal.
 */
public class ReportRepository {

    private final DatabaseManager databaseManager;

    public ReportRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void saveMonthlyReport(MonthlyMunicipalReportDTO report) {
        String sql = """
            INSERT INTO reporting_audit_monthly_reports (
                report_id, report_year, report_month, generated_at, total_trips,
                total_duration_minutes, estimated_distance_km, total_co2_saved_kg, report_json
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(report_year, report_month) DO UPDATE SET
                generated_at = excluded.generated_at,
                total_trips = excluded.total_trips,
                total_duration_minutes = excluded.total_duration_minutes,
                estimated_distance_km = excluded.estimated_distance_km,
                total_co2_saved_kg = excluded.total_co2_saved_kg,
                report_json = excluded.report_json;
            """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, report.reportId());
            stmt.setInt(2, report.year());
            stmt.setInt(3, report.month());
            stmt.setString(4, report.generatedAt().toString());
            stmt.setInt(5, report.totalTrips());
            stmt.setLong(6, report.totalDurationMinutes());
            stmt.setDouble(7, report.estimatedDistanceKm());
            stmt.setDouble(8, report.totalCo2SavedKg());
            stmt.setString(9, serializeParishStats(report.parishStats()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao gravar relatório mensal de auditoria " + report.reportId(), e);
        }
    }

    public Optional<MonthlyMunicipalReportDTO> findByYearAndMonth(int year, int month) {
        String sql = "SELECT * FROM reporting_audit_monthly_reports WHERE report_year = ? AND report_month = ?";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, year);
            stmt.setInt(2, month);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao consultar relatório mensal " + year + "/" + month, e);
        }
        return Optional.empty();
    }

    public List<MonthlyMunicipalReportDTO> findAll() {
        String sql = "SELECT * FROM reporting_audit_monthly_reports ORDER BY report_year DESC, report_month DESC";
        List<MonthlyMunicipalReportDTO> list = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar relatórios mensais", e);
        }
        return list;
    }

    private MonthlyMunicipalReportDTO mapRow(ResultSet rs) throws SQLException {
        return new MonthlyMunicipalReportDTO(
                rs.getString("report_id"),
                rs.getInt("report_year"),
                rs.getInt("report_month"),
                Instant.parse(rs.getString("generated_at")),
                rs.getInt("total_trips"),
                rs.getLong("total_duration_minutes"),
                rs.getDouble("estimated_distance_km"),
                rs.getDouble("total_co2_saved_kg"),
                deserializeParishStats(rs.getString("report_json"))
        );
    }

    private String serializeParishStats(List<ParishUsageReportDTO.ParishStatRecord> stats) {
        if (stats == null || stats.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (ParishUsageReportDTO.ParishStatRecord stat : stats) {
            sb.append(stat.parish()).append(";")
              .append(stat.tripsStarted()).append(";")
              .append(stat.tripsEnded()).append(";")
              .append(stat.totalDurationMinutes()).append(";")
              .append(stat.estimatedCo2SavedKg()).append("\n");
        }
        return sb.toString();
    }

    private List<ParishUsageReportDTO.ParishStatRecord> deserializeParishStats(String data) {
        if (data == null || data.isBlank()) return Collections.emptyList();
        List<ParishUsageReportDTO.ParishStatRecord> list = new ArrayList<>();
        String[] lines = data.split("\n");
        for (String line : lines) {
            if (line.isBlank()) continue;
            String[] parts = line.split(";");
            if (parts.length >= 5) {
                list.add(new ParishUsageReportDTO.ParishStatRecord(
                        parts[0],
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]),
                        Long.parseLong(parts[3]),
                        Double.parseDouble(parts[4])
                ));
            }
        }
        return list;
    }
}
