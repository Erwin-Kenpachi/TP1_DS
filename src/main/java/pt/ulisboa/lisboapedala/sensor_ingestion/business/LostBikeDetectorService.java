package pt.ulisboa.lisboapedala.sensor_ingestion.business;

import pt.ulisboa.lisboapedala.sensor_ingestion.BikeAnomalyEventDTO;
import pt.ulisboa.lisboapedala.sensor_ingestion.TelemetryDTO;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço responsável por identificar potenciais bicicletas perdidas ou furtadas
 * com base na ausência prolongada de sinal (> 2 horas sem comunicação fora de doca).
 */
public class LostBikeDetectorService {

    public static final Duration LOST_SIGNAL_THRESHOLD = Duration.ofHours(2);

    public Optional<BikeAnomalyEventDTO> checkLostSignal(TelemetryDTO latestTelemetry, Instant currentTime) {
        if (latestTelemetry == null || latestTelemetry.timestamp() == null) {
            return Optional.empty();
        }

        Duration elapsed = Duration.between(latestTelemetry.timestamp(), currentTime);
        if (elapsed.compareTo(LOST_SIGNAL_THRESHOLD) > 0) {
            return Optional.of(new BikeAnomalyEventDTO(
                    "ANO-" + UUID.randomUUID().toString().substring(0, 8),
                    latestTelemetry.bikeId(),
                    "LOST_SIGNAL",
                    String.format("Ausência de sinal prolongada há mais de %d minutos fora de doca.", elapsed.toMinutes()),
                    currentTime,
                    false
            ));
        }

        return Optional.empty();
    }
}
