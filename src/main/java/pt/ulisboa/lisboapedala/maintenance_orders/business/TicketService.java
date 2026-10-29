package pt.ulisboa.lisboapedala.maintenance_orders.business;

import pt.ulisboa.lisboapedala.maintenance_orders.MaintenanceOrderDTO;
import pt.ulisboa.lisboapedala.maintenance_orders.persistence.MaintenanceOrderRepository;
import pt.ulisboa.lisboapedala.sensor_ingestion.BikeAnomalyEventDTO;
import pt.ulisboa.lisboapedala.sensor_ingestion.SensorIngestionFacade;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço central de gestão do ciclo de vida dos tickets de manutenção (US-2).
 * Consome ativamente a SensorIngestionFacade para recolha e resolução de anomalias técnicas.
 * As ordens são atribuídas à equipa geral de técnicos sem distinção individual ou prioridades redundantes.
 */
public class TicketService {

    private final MaintenanceOrderRepository repository;
    private final TechnicianAssignmentService assignmentService;
    private final SensorIngestionFacade sensorIngestionFacade;

    public TicketService(MaintenanceOrderRepository repository,
                         TechnicianAssignmentService assignmentService,
                         SensorIngestionFacade sensorIngestionFacade) {
        this.repository = repository;
        this.assignmentService = assignmentService;
        this.sensorIngestionFacade = sensorIngestionFacade;
    }

    public synchronized MaintenanceOrderDTO createOrderFromAlert(String bikeId, String issueDescription) {
        String orderId = "MO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        MaintenanceOrderDTO initial = new MaintenanceOrderDTO(
                orderId,
                bikeId,
                issueDescription,
                "STANDARD",
                "PENDING",
                null,
                null,
                Instant.now(),
                null
        );

        MaintenanceOrderDTO order = assignmentService.assignToAnyTechnician(initial);
        repository.save(order);
        return order;
    }

    public List<MaintenanceOrderDTO> getPendingOrders() {
        return repository.findPendingOrders();
    }

    public Optional<MaintenanceOrderDTO> getOrderById(String orderId) {
        return repository.findById(orderId);
    }

    public synchronized MaintenanceOrderDTO completeOrder(String orderId) {
        MaintenanceOrderDTO order = repository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Ordem de manutenção não encontrada: " + orderId));

        if (order.isResolved()) {
            return order;
        }

        Instant now = Instant.now();
        MaintenanceOrderDTO resolved = new MaintenanceOrderDTO(
                order.orderId(),
                order.bikeId(),
                order.description(),
                order.priority(),
                "RESOLVED",
                order.technicianId(),
                order.anomalyId(),
                order.createdAt(),
                now
        );

        repository.save(resolved);

        // Se a ordem de manutenção teve origem numa anomalia de sensores, resolve-a ativamente
        if (order.anomalyId() != null && sensorIngestionFacade != null) {
            sensorIngestionFacade.resolveAnomaly(order.anomalyId());
        }

        return resolved;
    }

    /**
     * Consulta ativamente a SensorIngestionFacade para identificar anomalias técnicas pendentes
     * e convertê-las em ordens de manutenção (Pull model em conformidade com o ADR-005).
     * Cada avaria é atribuída à equipa geral de técnicos através do TechnicianAssignmentService.
     */
    public synchronized int syncFromSensorAnomalies() {
        if (sensorIngestionFacade == null) {
            return 0;
        }

        List<BikeAnomalyEventDTO> anomalies = sensorIngestionFacade.getUnresolvedAnomalies();
        int createdCount = 0;

        for (BikeAnomalyEventDTO anomaly : anomalies) {
            // Verifica idempotência: se já existe ordem para esta anomalia específica
            if (repository.findByAnomalyId(anomaly.anomalyId()).isEmpty()) {
                String orderId = "MO-ANOM-" + anomaly.anomalyId();
                MaintenanceOrderDTO initial = new MaintenanceOrderDTO(
                        orderId,
                        anomaly.bikeId(),
                        "[" + anomaly.anomalyType() + "] " + anomaly.description(),
                        "STANDARD",
                        "PENDING",
                        null,
                        anomaly.anomalyId(),
                        anomaly.timestamp() != null ? anomaly.timestamp() : Instant.now(),
                        null
                );

                MaintenanceOrderDTO order = assignmentService.assignToAnyTechnician(initial);
                repository.save(order);
                createdCount++;
            }
        }

        return createdCount;
    }
}
