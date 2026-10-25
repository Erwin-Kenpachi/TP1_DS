package pt.ulisboa.lisboapedala.maintenance_orders.presentation;

import pt.ulisboa.lisboapedala.maintenance_orders.MaintenanceOrderDTO;
import pt.ulisboa.lisboapedala.maintenance_orders.MaintenanceOrdersFacade;

import java.util.List;
import java.util.Optional;

/**
 * Controlador de apresentação para o painel de campo dos técnicos e reporte de avarias (US-2).
 * Comunica estritamente através da Fachada Pública MaintenanceOrdersFacade.
 */
public class MaintenanceController {

    private final MaintenanceOrdersFacade facade;

    public MaintenanceController(MaintenanceOrdersFacade facade) {
        this.facade = facade;
    }

    public MaintenanceOrderDTO reportMalfunction(String bikeId, String description) {
        return facade.createOrderFromAlert(bikeId, description);
    }

    public List<MaintenanceOrderDTO> getActiveOrders() {
        return facade.getActiveOrders();
    }

    public List<MaintenanceOrderDTO> getPendingOrders() {
        return facade.getPendingOrders();
    }

    public Optional<MaintenanceOrderDTO> getOrder(String orderId) {
        return facade.getOrderById(orderId);
    }

    public MaintenanceOrderDTO completeOrder(String orderId) {
        return facade.completeOrder(orderId);
    }

    public int syncSensorAnomalies() {
        return facade.syncFromSensorAnomalies();
    }
}
