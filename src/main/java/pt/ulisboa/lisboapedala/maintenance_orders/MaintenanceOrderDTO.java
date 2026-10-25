package pt.ulisboa.lisboapedala.maintenance_orders;

import java.time.Instant;

/**
 * Contrato público representativo de uma ordem/ticket de manutenção (US-2).
 * Detalha a avaria detetada ou reportada, o técnico responsável e o estado de resolução.
 */
public record MaintenanceOrderDTO(
        String orderId,
        String bikeId,
        String description,
        String priority,    // "HIGH", "MEDIUM", "LOW"
        String status,      // "PENDING", "ASSIGNED", "IN_PROGRESS", "RESOLVED"
        String technicianId,
        String anomalyId,   // Identificador da anomalia de telemetria se aplicável (null se manual)
        Instant createdAt,
        Instant resolvedAt
) {
    public boolean isResolved() {
        return "RESOLVED".equalsIgnoreCase(status);
    }

    public boolean isAssigned() {
        return technicianId != null && !technicianId.isBlank();
    }
}
