package pt.ulisboa.lisboapedala.dock_access;

import java.time.LocalDateTime;

/**
 * Contrato público para eventos de doca (armazenados localmente ou sincronizados).
 */
public record DockEventDTO(
        String eventId,
        String stationId,
        String dockId,
        String bikeId,
        String userId,
        String eventType,
        LocalDateTime occurredAt,
        boolean synced
) {}
