package pt.ulisboa.lisboapedala.dock_access.presentation;

import pt.ulisboa.lisboapedala.dock_access.DockAccessFacade;
import pt.ulisboa.lisboapedala.dock_access.ReturnBikeRequestDTO;
import pt.ulisboa.lisboapedala.dock_access.ReturnBikeResponseDTO;
import pt.ulisboa.lisboapedala.dock_access.UnlockBikeRequestDTO;
import pt.ulisboa.lisboapedala.dock_access.UnlockBikeResponseDTO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controlador de Apresentação (REST/CLI/App) para o módulo dock_access.
 * Ponto de entrada de pedidos de ciclistas e eventos enviados por docas físicas.
 * Comunica estritamente através da Fachada Pública DockAccessFacade.
 */
public class DockController {

    private final DockAccessFacade facade;

    public DockController(DockAccessFacade facade) {
        this.facade = facade;
    }

    public UnlockBikeResponseDTO unlockBike(String stationId,
                                           String dockId,
                                           String bikeId,
                                           String userId,
                                           boolean isSubscriber,
                                           String paymentMethod,
                                           LocalDateTime requestedAt) {
        UnlockBikeRequestDTO request = new UnlockBikeRequestDTO(
                stationId, dockId, bikeId, userId, isSubscriber, paymentMethod, requestedAt
        );
        return facade.unlockBike(request);
    }

    public ReturnBikeResponseDTO returnBike(String eventId,
                                           String stationId,
                                           String dockId,
                                           String bikeId,
                                           LocalDateTime occurredAt,
                                           boolean isSubscriber,
                                           String paymentMethod) {
        ReturnBikeRequestDTO request = new ReturnBikeRequestDTO(
                eventId, stationId, dockId, bikeId, occurredAt, isSubscriber, paymentMethod
        );
        return facade.returnBike(request);
    }

    public List<ReturnBikeResponseDTO> syncOfflineEvents(List<ReturnBikeRequestDTO> events) {
        return facade.syncOfflineEvents(events);
    }
}
