package pt.ulisboa.lisboapedala.trip_lifecycle;

import pt.ulisboa.lisboapedala.common.DatabaseManager;
import pt.ulisboa.lisboapedala.station_availability.StationAvailabilityFacade;
import pt.ulisboa.lisboapedala.tariff_billing.TariffBillingFacade;
import pt.ulisboa.lisboapedala.trip_lifecycle.business.TripService;
import pt.ulisboa.lisboapedala.trip_lifecycle.persistence.TripRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Fachada pública do módulo trip_lifecycle.
 * Único ponto de contacto público permitido para outros módulos.
 */
public class TripLifecycleFacade {

    private final TripService service;

    public TripLifecycleFacade(DatabaseManager databaseManager,
                               TariffBillingFacade tariffBillingFacade,
                               StationAvailabilityFacade stationAvailabilityFacade) {
        databaseManager.executeScriptFromClasspath("/pt/ulisboa/lisboapedala/trip_lifecycle/persistence/schema.sql");
        TripRepository repository = new TripRepository(databaseManager);
        this.service = new TripService(repository, tariffBillingFacade, stationAvailabilityFacade);
    }

    public TripDTO startTrip(StartTripRequestDTO request) {
        return service.startTrip(request);
    }

    public TripDTO completeTrip(CompleteTripRequestDTO request) {
        return service.completeTrip(request);
    }

    public Optional<TripDTO> getTripById(String tripId) {
        return service.getTripById(tripId);
    }

    public Optional<TripDTO> getActiveTripByUser(String userId) {
        return service.getActiveTripByUser(userId);
    }

    public Optional<TripDTO> getActiveTripByBike(String bikeId) {
        return service.getActiveTripByBike(bikeId);
    }

    public List<TripDTO> getCompletedTripsBetween(LocalDateTime start, LocalDateTime end) {
        return service.getCompletedTripsBetween(start, end);
    }

    public List<TripDTO> getTripsByUser(String userId) {
        return service.getTripsByUser(userId);
    }
}
