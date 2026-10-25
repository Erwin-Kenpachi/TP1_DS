package pt.ulisboa.lisboapedala.reporting_audit;

import java.time.Instant;
import java.util.List;

/**
 * Contrato público representativo do relatório de utilização por freguesia (US-4).
 * Em cumprimento com o RGPD (ADR-006), agrega exclusivamente métricas consolidadas e anónimas.
 */
public record ParishUsageReportDTO(
        Instant generatedAt,
        int totalTrips,
        long totalDurationMinutes,
        double totalCo2SavedKg,
        List<ParishStatRecord> parishStats
) {
    public record ParishStatRecord(
            String parish,
            int tripsStarted,
            int tripsEnded,
            long totalDurationMinutes,
            double estimatedCo2SavedKg
    ) {}
}
