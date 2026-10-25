package pt.ulisboa.lisboapedala.maintenance_orders.business;

import pt.ulisboa.lisboapedala.maintenance_orders.MaintenanceOrderDTO;

import java.util.Objects;

/**
 * Serviço de regras de negócio para disponibilização e atribuição de ordens
 * à equipa de manutenção (pool partilhada por qualquer técnico quando ocorre avaria).
 */
public class TechnicianAssignmentService {

    /**
     * Atribui uma ordem de manutenção à equipa geral de técnicos quando ocorre uma avaria,
     * tornando-a imediatamente visível e disponível para qualquer técnico de serviço.
     */
    public MaintenanceOrderDTO assignToAnyTechnician(MaintenanceOrderDTO order) {
        Objects.requireNonNull(order, "Ordem não pode ser nula");
        if (order.isResolved()) {
            throw new IllegalStateException("Não é possível atribuir uma ordem já resolvida: " + order.orderId());
        }

        return new MaintenanceOrderDTO(
                order.orderId(),
                order.bikeId(),
                order.description(),
                "STANDARD",
                "PENDING",
                null, // Disponível para qualquer técnico
                order.anomalyId(),
                order.createdAt(),
                order.resolvedAt()
        );
    }
}
