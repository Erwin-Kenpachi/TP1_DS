package pt.ulisboa.lisboapedala.reporting_audit.business;

import pt.ulisboa.lisboapedala.reporting_audit.ParishUsageReportDTO;
import pt.ulisboa.lisboapedala.trip_lifecycle.TripDTO;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agregador de utilização por freguesia (US-4).
 * Cruza as estações de origem e destino com as respetivas freguesias do concelho de Lisboa,
 * agregando contadores e métricas ambientais de forma anónima (ADR-006).
 */
public class ParishUsageAggregator {

    private final CarbonSavingsCalculator carbonCalculator;

    public ParishUsageAggregator(CarbonSavingsCalculator carbonCalculator) {
        this.carbonCalculator = carbonCalculator;
    }

    public ParishUsageReportDTO aggregate(List<TripDTO> trips, Map<String, String> stationParishMap) {
        if (trips == null || trips.isEmpty()) {
            return new ParishUsageReportDTO(Instant.now(), 0, 0, 0.0, Collections.emptyList());
        }

        Map<String, ParishMutableStat> statsMap = new HashMap<>();
        long totalDuration = 0;

        for (TripDTO trip : trips) {
            long duration = trip.durationMinutes() != null ? trip.durationMinutes() : 0;
            totalDuration += duration;

            String startParish = stationParishMap.getOrDefault(trip.startStationId(), "Outra / Não Especificada");
            String endParish = stationParishMap.getOrDefault(trip.endStationId(), startParish);

            // Registo de início
            statsMap.computeIfAbsent(startParish, ParishMutableStat::new).tripsStarted++;

            // Registo de fim e duração
            ParishMutableStat endStat = statsMap.computeIfAbsent(endParish, ParishMutableStat::new);
            endStat.tripsEnded++;
            endStat.totalDurationMinutes += duration;
        }

        List<ParishUsageReportDTO.ParishStatRecord> parishRecords = new ArrayList<>();
        double totalCo2 = 0.0;

        for (ParishMutableStat stat : statsMap.values()) {
            double co2 = carbonCalculator.calculateCo2SavedFromDuration(stat.totalDurationMinutes);
            totalCo2 += co2;
            parishRecords.add(new ParishUsageReportDTO.ParishStatRecord(
                    stat.parish,
                    stat.tripsStarted,
                    stat.tripsEnded,
                    stat.totalDurationMinutes,
                    co2
            ));
        }

        parishRecords.sort((a, b) -> Integer.compare(b.tripsStarted() + b.tripsEnded(), a.tripsStarted() + a.tripsEnded()));

        return new ParishUsageReportDTO(
                Instant.now(),
                trips.size(),
                totalDuration,
                totalCo2,
                parishRecords
        );
    }

    private static class ParishMutableStat {
        final String parish;
        int tripsStarted = 0;
        int tripsEnded = 0;
        long totalDurationMinutes = 0;

        ParishMutableStat(String parish) {
            this.parish = parish;
        }
    }
}
