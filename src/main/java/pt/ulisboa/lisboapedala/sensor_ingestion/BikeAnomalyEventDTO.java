package pt.ulisboa.lisboapedala.sensor_ingestion;

import java.time.Instant;

/**
 * Contrato público que detalha um evento de anomalia detetado pela ingestão de sensores.
 * Utilizado por outros módulos (como maintenance_orders na US-2) para abertura de ordens de manutenção.
 */
public record BikeAnomalyEventDTO(
        String anomalyId,
        String bikeId,
        String anomalyType, // "CRITICAL_BATTERY", "BRAKE_FAILURE", "LOST_SIGNAL", "MECHANICAL_FAULT"
        String description,
        Instant timestamp,
        boolean resolved
) {}
