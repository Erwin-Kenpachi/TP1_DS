package pt.ulisboa.lisboapedala.sensor_ingestion;

import java.time.Instant;

/**
 * Contrato público que representa um pacote de telemetria emitido por uma bicicleta elétrica.
 * Em conformidade com o RGPD (ADR-006), não contém identificadores de utilizador, apenas dados técnicos do veículo.
 */
public record TelemetryDTO(
        String bikeId,
        double batteryLevel,
        double latitude,
        double longitude,
        double speedKmh,
        String brakeStatus,
        Instant timestamp
) {}
