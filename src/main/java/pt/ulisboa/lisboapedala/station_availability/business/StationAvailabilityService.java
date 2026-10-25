package pt.ulisboa.lisboapedala.station_availability.business;

import pt.ulisboa.lisboapedala.station_availability.StationOccupancyDTO;
import pt.ulisboa.lisboapedala.station_availability.persistence.StationRepository;

import java.util.Optional;

public class StationAvailabilityService {

    private final StationRepository repository;

    public StationAvailabilityService(StationRepository repository) {
        this.repository = repository;
    }

    public void registerStation(StationOccupancyDTO station) {
        repository.saveOrUpdate(station);
    }

    public Optional<StationOccupancyDTO> getStationOccupancy(String stationId) {
        return repository.findById(stationId);
    }

    public void updateAvailability(String stationId, int bikeDelta, int dockDelta) {
        repository.updateCounters(stationId, bikeDelta, dockDelta);
    }

    public java.util.List<StationOccupancyDTO> getAllStations() {
        return repository.findAll();
    }
}

