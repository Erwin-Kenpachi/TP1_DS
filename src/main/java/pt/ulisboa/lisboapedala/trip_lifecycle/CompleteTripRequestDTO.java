package pt.ulisboa.lisboapedala.trip_lifecycle;

import java.time.LocalDateTime;

/**
 * Contrato público para conclusão de viagem.
 */
public record CompleteTripRequestDTO(
        String tripId,
        String endStationId,
        LocalDateTime endTime,
        String returnEventId,
        boolean isSubscriber,
        String paymentMethod
) {}
