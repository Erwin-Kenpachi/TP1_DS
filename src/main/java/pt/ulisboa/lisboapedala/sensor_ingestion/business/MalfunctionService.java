package pt.ulisboa.lisboapedala.sensor_ingestion.business;

import pt.ulisboa.lisboapedala.sensor_ingestion.BikeAnomalyEventDTO;
import pt.ulisboa.lisboapedala.sensor_ingestion.TelemetryDTO;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Serviço responsável por avaliar leituras de sensores e detetar anomalias técnicas
 * (nível de bateria crítico, falhas no sistema de travagem ou avarias mecânicas).
 */
public class MalfunctionService {

    public static final double CRITICAL_BATTERY_THRESHOLD = 15.0;

    public List<BikeAnomalyEventDTO> evaluateAnomalies(TelemetryDTO telemetry) {
        List<BikeAnomalyEventDTO> anomalies = new ArrayList<>();

        // 1. Verificação de bateria crítica
        if (telemetry.batteryLevel() < CRITICAL_BATTERY_THRESHOLD) {
            anomalies.add(new BikeAnomalyEventDTO(
                    "ANO-" + UUID.randomUUID().toString().substring(0, 8),
                    telemetry.bikeId(),
                    "CRITICAL_BATTERY",
                    String.format("Bateria crítica: %.1f%% (abaixo do limite operacional de %.0f%%)",
                            telemetry.batteryLevel(), CRITICAL_BATTERY_THRESHOLD),
                    telemetry.timestamp(),
                    false
            ));
        }

        // 2. Verificação de avaria no sistema de travões
        String brake = telemetry.brakeStatus().toUpperCase();
        if (brake.contains("FAULT") || brake.contains("FAILED") || brake.contains("WORN") || brake.contains("AVARIA")) {
            anomalies.add(new BikeAnomalyEventDTO(
                    "ANO-" + UUID.randomUUID().toString().substring(0, 8),
                    telemetry.bikeId(),
                    "BRAKE_FAILURE",
                    "Falha técnica reportada no sistema de travões: " + telemetry.brakeStatus(),
                    telemetry.timestamp(),
                    false
            ));
        }

        return anomalies;
    }

    public BikeAnomalyEventDTO createManualReport(String bikeId, String anomalyType, String description) {
        return new BikeAnomalyEventDTO(
                "ANO-" + UUID.randomUUID().toString().substring(0, 8),
                bikeId,
                anomalyType != null ? anomalyType.toUpperCase() : "GENERAL_MALFUNCTION",
                description,
                Instant.now(),
                false
        );
    }
}
