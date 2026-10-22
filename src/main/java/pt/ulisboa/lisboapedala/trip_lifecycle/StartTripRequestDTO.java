package pt.ulisboa.lisboapedala.trip_lifecycle;

import java.time.LocalDateTime;

/**
 * Contrato público para início de viagem.
 */
public record StartTripRequestDTO(
        String tripId,
        String userId,
        String bikeId,
        String startStationId,
        LocalDateTime startTime
) {}
