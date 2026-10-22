package pt.ulisboa.lisboapedala.dock_access;

import pt.ulisboa.lisboapedala.common.DatabaseManager;
import pt.ulisboa.lisboapedala.dock_access.business.DockAccessService;
import pt.ulisboa.lisboapedala.dock_access.persistence.DockEventRepository;
import pt.ulisboa.lisboapedala.station_availability.StationAvailabilityFacade;
import pt.ulisboa.lisboapedala.trip_lifecycle.TripLifecycleFacade;

import java.util.List;

/**
 * Fachada pública do módulo dock_access.
 * Único ponto de contacto público permitido para outros módulos ou clientes de apresentação.
 */
public class DockAccessFacade {

    private final DockAccessService service;

    public DockAccessFacade(DatabaseManager databaseManager,
                            TripLifecycleFacade tripLifecycleFacade,
                            StationAvailabilityFacade stationAvailabilityFacade) {
        databaseManager.executeScriptFromClasspath("/pt/ulisboa/lisboapedala/dock_access/persistence/schema.sql");
        DockEventRepository repository = new DockEventRepository(databaseManager);
        this.service = new DockAccessService(repository, tripLifecycleFacade, stationAvailabilityFacade);
    }

    public UnlockBikeResponseDTO unlockBike(UnlockBikeRequestDTO request) {
        return service.unlockBike(request);
    }

    public ReturnBikeResponseDTO returnBike(ReturnBikeRequestDTO request) {
        return service.returnBike(request);
    }

    public List<ReturnBikeResponseDTO> syncOfflineEvents(List<ReturnBikeRequestDTO> events) {
        return service.syncOfflineEvents(events);
    }
}
