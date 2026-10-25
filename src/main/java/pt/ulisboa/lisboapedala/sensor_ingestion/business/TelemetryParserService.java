package pt.ulisboa.lisboapedala.sensor_ingestion.business;

import pt.ulisboa.lisboapedala.sensor_ingestion.TelemetryDTO;

import java.time.Instant;

/**
 * Serviço responsável por validar, sanitizar e decodificar dados brutos de telemetria IoT.
 */
public class TelemetryParserService {

    public TelemetryDTO parseAndSanitize(TelemetryDTO raw) {
        if (raw.bikeId() == null || raw.bikeId().isBlank()) {
            throw new IllegalArgumentException("Identificador de bicicleta inválido.");
        }

        double battery = Math.max(0.0, Math.min(100.0, raw.batteryLevel()));
        double speed = Math.max(0.0, raw.speedKmh());
        String brakes = (raw.brakeStatus() == null || raw.brakeStatus().isBlank()) ? "OK" : raw.brakeStatus().toUpperCase();
        Instant timestamp = raw.timestamp() != null ? raw.timestamp() : Instant.now();

        return new TelemetryDTO(
                raw.bikeId().trim(),
                battery,
                raw.latitude(),
                raw.longitude(),
                speed,
                brakes,
                timestamp
        );
    }
}
