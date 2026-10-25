package pt.ulisboa.lisboapedala.reporting_audit.business;

import pt.ulisboa.lisboapedala.reporting_audit.ParishUsageReportDTO;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Entidade de domínio representativa de um relatório municipal mensal consolidado.
 */
public class MunicipalReport {

    private final String reportId;
    private final int year;
    private final int month;
    private final Instant generatedAt;
    private final int totalTrips;
    private final long totalDurationMinutes;
    private final double estimatedDistanceKm;
    private final double totalCo2SavedKg;
    private final List<ParishUsageReportDTO.ParishStatRecord> parishStats;

    public MunicipalReport(int year, int month, int totalTrips, long totalDurationMinutes,
                           double estimatedDistanceKm, double totalCo2SavedKg,
                           List<ParishUsageReportDTO.ParishStatRecord> parishStats) {
        this.reportId = "REP-" + year + "-" + String.format("%02d", month) + "-" + UUID.randomUUID().toString().substring(0, 8);
        this.year = year;
        this.month = month;
        this.generatedAt = Instant.now();
        this.totalTrips = totalTrips;
        this.totalDurationMinutes = totalDurationMinutes;
        this.estimatedDistanceKm = estimatedDistanceKm;
        this.totalCo2SavedKg = totalCo2SavedKg;
        this.parishStats = parishStats;
    }

    public String getReportId() { return reportId; }
    public int getYear() { return year; }
    public int getMonth() { return month; }
    public Instant getGeneratedAt() { return generatedAt; }
    public int getTotalTrips() { return totalTrips; }
    public long getTotalDurationMinutes() { return totalDurationMinutes; }
    public double getEstimatedDistanceKm() { return estimatedDistanceKm; }
    public double getTotalCo2SavedKg() { return totalCo2SavedKg; }
    public List<ParishUsageReportDTO.ParishStatRecord> getParishStats() { return parishStats; }
}
