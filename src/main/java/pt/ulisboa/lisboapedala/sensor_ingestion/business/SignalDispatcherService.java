package pt.ulisboa.lisboapedala.sensor_ingestion.business;

import pt.ulisboa.lisboapedala.sensor_ingestion.BikeAnomalyEventDTO;
import pt.ulisboa.lisboapedala.sensor_ingestion.BikeHealthStatusDTO;
import pt.ulisboa.lisboapedala.sensor_ingestion.TelemetryDTO;
import pt.ulisboa.lisboapedala.sensor_ingestion.persistence.TelemetryLogRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Orquestrador principal da receção e despacho de sinais de telemetria.
 */
public class SignalDispatcherService {

    private final TelemetryLogRepository repository;
    private final TelemetryParserService parserService;
    private final MalfunctionService malfunctionService;
    private final LostBikeDetectorService lostBikeDetectorService;
    private final MalfunctionEmitter malfunctionEmitter;

    public SignalDispatcherService(TelemetryLogRepository repository,
                                   TelemetryParserService parserService,
                                   MalfunctionService malfunctionService,
                                   LostBikeDetectorService lostBikeDetectorService,
                                   MalfunctionEmitter malfunctionEmitter) {
        this.repository = repository;
        this.parserService = parserService;
        this.malfunctionService = malfunctionService;
        this.lostBikeDetectorService = lostBikeDetectorService;
        this.malfunctionEmitter = malfunctionEmitter;
    }

    public TelemetryDTO processTelemetry(TelemetryDTO raw) {
        TelemetryDTO sanitized = parserService.parseAndSanitize(raw);
        repository.saveTelemetry(sanitized);

        // Avaliar anomalias técnicas (bateria, travões)
        List<BikeAnomalyEventDTO> detectedAnomalies = malfunctionService.evaluateAnomalies(sanitized);
        for (BikeAnomalyEventDTO anomaly : detectedAnomalies) {
            malfunctionEmitter.emit(anomaly);
        }

        return sanitized;
    }

    public TelemetryDTO getLatestTelemetry(String bikeId) {
        return repository.findLatestByBikeId(bikeId)
                .orElse(null);
    }

    public BikeHealthStatusDTO getBikeHealthStatus(String bikeId) {
        Optional<TelemetryDTO> latestOpt = repository.findLatestByBikeId(bikeId);
        List<BikeAnomalyEventDTO> activeAnomalies = repository.findUnresolvedAnomaliesByBikeId(bikeId);

        if (latestOpt.isEmpty()) {
            return new BikeHealthStatusDTO(
                    bikeId,
                    "UNKNOWN",
                    0.0,
                    "UNKNOWN",
                    List.of("Sem registo de telemetria"),
                    null
            );
        }

        TelemetryDTO latest = latestOpt.get();
        List<String> alerts = new ArrayList<>();
        for (BikeAnomalyEventDTO anom : activeAnomalies) {
            alerts.add(anom.anomalyType() + ": " + anom.description());
        }

        String status = "OPERATIONAL";
        if (latest.batteryLevel() < MalfunctionService.CRITICAL_BATTERY_THRESHOLD || !activeAnomalies.isEmpty()) {
            status = "CRITICAL";
        } else if (latest.batteryLevel() < 30.0) {
            status = "WARNING";
        }

        return new BikeHealthStatusDTO(
                bikeId,
                status,
                latest.batteryLevel(),
                latest.brakeStatus(),
                alerts,
                latest.timestamp()
        );
    }

    public List<BikeAnomalyEventDTO> getUnresolvedAnomalies() {
        return repository.findUnresolvedAnomalies();
    }

    public BikeAnomalyEventDTO reportManualMalfunction(String bikeId, String failureType, String description) {
        BikeAnomalyEventDTO anomaly = malfunctionService.createManualReport(bikeId, failureType, description);
        malfunctionEmitter.emit(anomaly);
        return anomaly;
    }

    public void resolveAnomaly(String anomalyId) {
        repository.markAnomalyResolved(anomalyId);
    }

    public Optional<BikeAnomalyEventDTO> checkLostSignal(String bikeId, Instant currentTime) {
        Optional<TelemetryDTO> latestOpt = repository.findLatestByBikeId(bikeId);
        if (latestOpt.isEmpty()) {
            return Optional.empty();
        }
        Optional<BikeAnomalyEventDTO> lostAnomaly = lostBikeDetectorService.checkLostSignal(latestOpt.get(), currentTime);
        lostAnomaly.ifPresent(malfunctionEmitter::emit);
        return lostAnomaly;
    }
}
