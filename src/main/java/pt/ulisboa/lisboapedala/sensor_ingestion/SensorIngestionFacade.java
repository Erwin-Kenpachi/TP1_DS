package pt.ulisboa.lisboapedala.sensor_ingestion;

import pt.ulisboa.lisboapedala.common.DatabaseManager;
import pt.ulisboa.lisboapedala.sensor_ingestion.business.LostBikeDetectorService;
import pt.ulisboa.lisboapedala.sensor_ingestion.business.MalfunctionEmitter;
import pt.ulisboa.lisboapedala.sensor_ingestion.business.MalfunctionService;
import pt.ulisboa.lisboapedala.sensor_ingestion.business.SignalDispatcherService;
import pt.ulisboa.lisboapedala.sensor_ingestion.business.TelemetryParserService;
import pt.ulisboa.lisboapedala.sensor_ingestion.persistence.TelemetryLogRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Fachada pública do módulo sensor_ingestion.
 * Ponto de entrada estável e único para outros módulos (ex: maintenance_orders na US-2)
 * e para a camada de apresentação/controladores IoT.
 *
 * Em conformidade com o ADR-002 e ADR-005:
 * Instabilidade nula (I = 0.0), dependendo unicamente da infraestrutura transversal DatabaseManager.
 */
public class SensorIngestionFacade {

    private final SignalDispatcherService dispatcherService;
    private final MalfunctionEmitter malfunctionEmitter;

    public SensorIngestionFacade(DatabaseManager databaseManager) {
        databaseManager.executeScriptFromClasspath("/pt/ulisboa/lisboapedala/sensor_ingestion/persistence/schema.sql");
        TelemetryLogRepository repository = new TelemetryLogRepository(databaseManager);
        TelemetryParserService parserService = new TelemetryParserService();
        MalfunctionService malfunctionService = new MalfunctionService();
        LostBikeDetectorService lostBikeDetectorService = new LostBikeDetectorService();
        this.malfunctionEmitter = new MalfunctionEmitter(repository);
        this.dispatcherService = new SignalDispatcherService(
                repository,
                parserService,
                malfunctionService,
                lostBikeDetectorService,
                this.malfunctionEmitter
        );
    }

    /**
     * Ingestão de um novo sinal de telemetria periódico de uma bicicleta elétrica (~35 req/s).
     */
    public TelemetryDTO ingestTelemetry(TelemetryDTO telemetry) {
        return dispatcherService.processTelemetry(telemetry);
    }

    /**
     * Consulta da última telemetria registada de uma bicicleta.
     */
    public TelemetryDTO getLatestTelemetry(String bikeId) {
        return dispatcherService.getLatestTelemetry(bikeId);
    }

    /**
     * Avaliação do estado de saúde operacional consolidado de uma bicicleta.
     */
    public BikeHealthStatusDTO getBikeHealthStatus(String bikeId) {
        return dispatcherService.getBikeHealthStatus(bikeId);
    }

    /**
     * Lista de anomalias ativas pendentes de resolução (consumida pelo módulo maintenance_orders na US-2).
     */
    public List<BikeAnomalyEventDTO> getUnresolvedAnomalies() {
        return dispatcherService.getUnresolvedAnomalies();
    }

    /**
     * Registo direto/manual de avaria reportada na bicicleta.
     */
    public BikeAnomalyEventDTO reportBikeMalfunction(String bikeId, String failureType, String description) {
        return dispatcherService.reportManualMalfunction(bikeId, failureType, description);
    }

    /**
     * Marcação de uma anomalia como resolvida (após conclusão da ordem de manutenção).
     */
    public void resolveAnomaly(String anomalyId) {
        dispatcherService.resolveAnomaly(anomalyId);
    }

    /**
     * Verificação de sinal perdido em bicicletas elétricas fora de doca há mais de 2 horas.
     */
    public Optional<BikeAnomalyEventDTO> checkLostSignal(String bikeId, Instant currentTime) {
        return dispatcherService.checkLostSignal(bikeId, currentTime);
    }

    /**
     * Permite a registo de ouvintes assíncronos/desacoplados para eventos de anomalia.
     */
    public void registerAnomalyListener(Consumer<BikeAnomalyEventDTO> listener) {
        malfunctionEmitter.registerListener(listener);
    }
}
