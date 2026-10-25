package pt.ulisboa.lisboapedala.sensor_ingestion.business;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ulisboa.lisboapedala.sensor_ingestion.BikeAnomalyEventDTO;
import pt.ulisboa.lisboapedala.sensor_ingestion.persistence.TelemetryLogRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Emissor de eventos de anomalia técnica.
 * Regista as anomalias no repositório privado do módulo e disponibiliza-as para consumo desacoplado,
 * SEM invocar diretamente módulos externos (respeitando o acoplamento eferente zero do módulo - ADR-005).
 */
public class MalfunctionEmitter {

    private static final Logger logger = LoggerFactory.getLogger(MalfunctionEmitter.class);

    private final TelemetryLogRepository repository;
    private final List<Consumer<BikeAnomalyEventDTO>> listeners = Collections.synchronizedList(new ArrayList<>());

    public MalfunctionEmitter(TelemetryLogRepository repository) {
        this.repository = repository;
    }

    public void emit(BikeAnomalyEventDTO anomaly) {
        logger.warn("Anomalia detetada na bicicleta {}: [{}] {}", anomaly.bikeId(), anomaly.anomalyType(), anomaly.description());
        repository.saveAnomaly(anomaly);

        synchronized (listeners) {
            for (Consumer<BikeAnomalyEventDTO> listener : listeners) {
                try {
                    listener.accept(anomaly);
                } catch (Exception e) {
                    logger.error("Erro ao notificar ouvinte de anomalia: {}", e.getMessage());
                }
            }
        }
    }

    public void registerListener(Consumer<BikeAnomalyEventDTO> listener) {
        listeners.add(listener);
    }
}
