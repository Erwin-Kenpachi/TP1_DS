package pt.ulisboa.lisboapedala.reporting_audit;

import java.time.Instant;
import java.util.List;

/**
 * Contrato público para o relatório mensal consolidado enviado à Câmara Municipal (Requisito 7).
 */
public record MonthlyMunicipalReportDTO(
        String reportId,
        int year,
        int month,
        Instant generatedAt,
        int totalTrips,
        long totalDurationMinutes,
        double estimatedDistanceKm,
        double totalCo2SavedKg,
        List<ParishUsageReportDTO.ParishStatRecord> parishStats
) {}
