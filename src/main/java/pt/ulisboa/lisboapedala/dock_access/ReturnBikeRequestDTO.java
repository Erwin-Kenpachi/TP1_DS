package pt.ulisboa.lisboapedala.dock_access;

import java.time.LocalDateTime;

/**
 * Contrato público para registo de devolução física de uma bicicleta numa doca.
 */
public record ReturnBikeRequestDTO(
        String eventId,
        String stationId,
        String dockId,
        String bikeId,
        LocalDateTime occurredAt,
        boolean isSubscriber,
        String paymentMethod
) {}
