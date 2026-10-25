package pt.ulisboa.lisboapedala.sensor_ingestion.presentation;

import pt.ulisboa.lisboapedala.sensor_ingestion.BikeAnomalyEventDTO;
import pt.ulisboa.lisboapedala.sensor_ingestion.BikeHealthStatusDTO;
import pt.ulisboa.lisboapedala.sensor_ingestion.SensorIngestionFacade;
import pt.ulisboa.lisboapedala.sensor_ingestion.TelemetryDTO;

import java.time.Instant;
import java.util.List;

/**
 * Controlador da Camada de Apresentação / Ingestão de Rede (REST/IoT/Webhook)
 * Simula a interface HTTP/REST para receção contínua dos pings IoT de telemetria das 1 050 bicicletas elétricas.
 * Comunica exclusivamente através da Fachada Pública SensorIngestionFacade.
 */
public class TelemetryController {

    private final SensorIngestionFacade facade;

    public TelemetryController(SensorIngestionFacade facade) {
        this.facade = facade;
    }

    /**
     * Endpoint simulado para receção de pings de telemetria (POST /api/v1/telemetry)
     */
    public TelemetryDTO receiveTelemetryPing(String bikeId,
                                            double batteryLevel,
                                            double latitude,
                                            double longitude,
                                            double speedKmh,
                                            String brakeStatus,
                                            Instant timestamp) {
        TelemetryDTO dto = new TelemetryDTO(
                bikeId,
                batteryLevel,
                latitude,
                longitude,
                speedKmh,
                brakeStatus,
                timestamp != null ? timestamp : Instant.now()
        );
        return facade.ingestTelemetry(dto);
    }

    /**
     * Endpoint para consulta de estado de saúde de uma bicicleta (GET /api/v1/bikes/{id}/health)
     */
    public BikeHealthStatusDTO getBikeHealth(String bikeId) {
        return facade.getBikeHealthStatus(bikeId);
    }

    /**
     * Endpoint para consulta de anomalias por resolver (GET /api/v1/telemetry/anomalies)
     */
    public List<BikeAnomalyEventDTO> getUnresolvedAnomalies() {
        return facade.getUnresolvedAnomalies();
    }

    /**
     * Endpoint para reporte de avaria na bicicleta (POST /api/v1/bikes/{id}/report-malfunction)
     */
    public BikeAnomalyEventDTO reportMalfunction(String bikeId, String failureType, String description) {
        return facade.reportBikeMalfunction(bikeId, failureType, description);
    }
}
