package pt.ulisboa.lisboapedala.reporting_audit;

import java.time.Instant;
import java.util.List;

/**
 * Contrato público para publicação da ocupação das estações como Dados Abertos (Open Data).
 */
public record StationOpenDataDTO(
        Instant generatedAt,
        int totalStations,
        int totalBikesAvailable,
        int totalDocksFree,
        List<StationOpenDataRecord> stations
) {
    public record StationOpenDataRecord(
            String stationId,
            String name,
            String parish,
            int totalDocks,
            int availableBikes,
            int freeDocks,
            double occupancyRate
    ) {}
}
