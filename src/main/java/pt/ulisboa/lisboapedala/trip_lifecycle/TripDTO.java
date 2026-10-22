package pt.ulisboa.lisboapedala.trip_lifecycle;

import java.time.LocalDateTime;

/**
 * Contrato público representativo de uma viagem.
 */
public record TripDTO(
        String tripId,
        String userId,
        String bikeId,
        String startStationId,
        String endStationId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Long durationMinutes,
        String status,
        String returnEventId
) {}
