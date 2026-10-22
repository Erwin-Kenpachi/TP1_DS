package pt.ulisboa.lisboapedala.dock_access;

import java.time.LocalDateTime;

/**
 * Contrato público para solicitação de desbloqueio de uma bicicleta numa doca.
 */
public record UnlockBikeRequestDTO(
        String stationId,
        String dockId,
        String bikeId,
        String userId,
        boolean isSubscriber,
        String paymentMethod,
        LocalDateTime requestedAt
) {}
