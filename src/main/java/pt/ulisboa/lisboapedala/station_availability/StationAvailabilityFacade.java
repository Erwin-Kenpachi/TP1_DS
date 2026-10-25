package pt.ulisboa.lisboapedala.station_availability;

import pt.ulisboa.lisboapedala.common.DatabaseManager;
import pt.ulisboa.lisboapedala.station_availability.business.StationAvailabilityService;
import pt.ulisboa.lisboapedala.station_availability.persistence.StationRepository;

import java.util.Optional;

/**
 * Fachada pública do módulo station_availability.
 * Único ponto de contacto público permitido para outros módulos.
 */
public class StationAvailabilityFacade {

    private final StationAvailabilityService service;

    public StationAvailabilityFacade(DatabaseManager databaseManager) {
        // Inicializa o esquema do módulo
        databaseManager.executeScriptFromClasspath("/pt/ulisboa/lisboapedala/station_availability/persistence/schema.sql");
        StationRepository repository = new StationRepository(databaseManager);
        this.service = new StationAvailabilityService(repository);
    }

    public void registerStation(StationOccupancyDTO station) {
        service.registerStation(station);
    }

    public Optional<StationOccupancyDTO> getStationOccupancy(String stationId) {
        return service.getStationOccupancy(stationId);
    }

    public void updateAvailability(String stationId, int bikeDelta, int dockDelta) {
        service.updateAvailability(stationId, bikeDelta, dockDelta);
    }

    public java.util.List<StationOccupancyDTO> getAllStations() {
        return service.getAllStations();
    }
}

