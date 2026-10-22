package pt.ulisboa.lisboapedala.station_availability.presentation;

import pt.ulisboa.lisboapedala.station_availability.StationAvailabilityFacade;
import pt.ulisboa.lisboapedala.station_availability.StationOccupancyDTO;

import java.util.Optional;

/**
 * Controlador de Apresentação (REST/CLI) para o módulo station_availability.
 * Comunica estritamente através da Fachada Pública StationAvailabilityFacade.
 */
public class StationController {

    private final StationAvailabilityFacade facade;

    public StationController(StationAvailabilityFacade facade) {
        this.facade = facade;
    }

    public Optional<StationOccupancyDTO> getStationOccupancy(String stationId) {
        return facade.getStationOccupancy(stationId);
    }

    public void registerStation(String stationId, String name, String parish, int totalDocks, int availableBikes, int freeDocks) {
        facade.registerStation(new StationOccupancyDTO(stationId, name, parish, totalDocks, availableBikes, freeDocks));
    }
}
