package pt.ulisboa.lisboapedala.maintenance_orders;

import pt.ulisboa.lisboapedala.common.DatabaseManager;
import pt.ulisboa.lisboapedala.maintenance_orders.business.TechnicianAssignmentService;
import pt.ulisboa.lisboapedala.maintenance_orders.business.TicketService;
import pt.ulisboa.lisboapedala.maintenance_orders.persistence.MaintenanceOrderRepository;
import pt.ulisboa.lisboapedala.sensor_ingestion.SensorIngestionFacade;

import java.util.List;
import java.util.Optional;

/**
 * Fachada pública do módulo maintenance_orders.
 * Único ponto de contacto público permitido para outros módulos e apresentação.
 * Suporta o registo, priorização, atribuição e resolução de ordens de manutenção (US-2).
 *
 * Em conformidade com o ADR-005 e ARCHITECTURE.md:
 * Depende unicamente de sensor_ingestion.
 */
public class MaintenanceOrdersFacade {

    private final TicketService ticketService;

    public MaintenanceOrdersFacade(DatabaseManager databaseManager,
                                   SensorIngestionFacade sensorIngestionFacade) {
        databaseManager.executeScriptFromClasspath("/pt/ulisboa/lisboapedala/maintenance_orders/persistence/schema.sql");
        MaintenanceOrderRepository repository = new MaintenanceOrderRepository(databaseManager);
        TechnicianAssignmentService assignmentService = new TechnicianAssignmentService();
        this.ticketService = new TicketService(repository, assignmentService, sensorIngestionFacade);
    }

    /**
     * Cria uma nova ordem de reparação a partir de um alerta ou reporte de avaria.
     */
    public MaintenanceOrderDTO createOrderFromAlert(String bikeId, String issueDescription) {
        return ticketService.createOrderFromAlert(bikeId, issueDescription);
    }

    /**
     * Devolve a lista de ordens de manutenção ativas no sistema para todos os técnicos.
     */
    public List<MaintenanceOrderDTO> getActiveOrders() {
        syncFromSensorAnomalies();
        return ticketService.getPendingOrders();
    }

    /**
     * Devolve a lista de ordens de manutenção pendentes para os técnicos (compatibilidade).
     */
    public List<MaintenanceOrderDTO> getPendingOrders() {
        return ticketService.getPendingOrders();
    }

    /**
     * Consulta os detalhes de uma ordem por ID.
     */
    public Optional<MaintenanceOrderDTO> getOrderById(String orderId) {
        return ticketService.getOrderById(orderId);
    }

    /**
     * Marca a ordem de manutenção como resolvida e atualiza a telemetria associada.
     */
    public MaintenanceOrderDTO completeOrder(String orderId) {
        return ticketService.completeOrder(orderId);
    }

    /**
     * Sincroniza ativamente as anomalias técnicas pendentes de sensor_ingestion, criando os respetivos tickets.
     */
    public int syncFromSensorAnomalies() {
        return ticketService.syncFromSensorAnomalies();
    }
}
