package pt.ulisboa.lisboapedala.trip_lifecycle.presentation;

import pt.ulisboa.lisboapedala.trip_lifecycle.TripDTO;
import pt.ulisboa.lisboapedala.trip_lifecycle.TripLifecycleFacade;

import java.util.Optional;

/**
 * Controlador de Apresentação (REST/App) para o módulo trip_lifecycle.
 * Permite a consulta de viagens ativas e histórico de viagens.
 * Comunica estritamente através da Fachada Pública TripLifecycleFacade.
 */
public class TripController {

    private final TripLifecycleFacade facade;

    public TripController(TripLifecycleFacade facade) {
        this.facade = facade;
    }

    public Optional<TripDTO> getActiveTrip(String userId) {
        return facade.getActiveTripByUser(userId);
    }

    public Optional<TripDTO> getTrip(String tripId) {
        return facade.getTripById(tripId);
    }
}
