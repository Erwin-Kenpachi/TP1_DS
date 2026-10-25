package pt.ulisboa.lisboapedala.sensor_ingestion;

import java.time.Instant;
import java.util.List;

/**
 * Contrato público representativo do estado de saúde operacional consolidado de uma bicicleta.
 */
public record BikeHealthStatusDTO(
        String bikeId,
        String healthStatus, // "OPERATIONAL", "WARNING", "CRITICAL", "UNKNOWN"
        double batteryLevel,
        String brakeStatus,
        List<String> activeAlerts,
        Instant lastPingTimestamp
) {}
