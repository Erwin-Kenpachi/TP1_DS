package pt.ulisboa.lisboapedala.station_availability;

/**
 * Contrato público com os dados de ocupação de uma estação.
 */
public record StationOccupancyDTO(
        String stationId,
        String name,
        String parish,
        int totalDocks,
        int availableBikes,
        int freeDocks
) {}
