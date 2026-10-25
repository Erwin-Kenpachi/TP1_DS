package pt.ulisboa.lisboapedala.trip_lifecycle.business;

import pt.ulisboa.lisboapedala.station_availability.StationAvailabilityFacade;
import pt.ulisboa.lisboapedala.tariff_billing.BillingRequestDTO;
import pt.ulisboa.lisboapedala.tariff_billing.TariffBillingFacade;
import pt.ulisboa.lisboapedala.trip_lifecycle.CompleteTripRequestDTO;
import pt.ulisboa.lisboapedala.trip_lifecycle.StartTripRequestDTO;
import pt.ulisboa.lisboapedala.trip_lifecycle.TripDTO;
import pt.ulisboa.lisboapedala.trip_lifecycle.persistence.TripRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class TripService {

    private final TripRepository tripRepository;
    private final TariffBillingFacade tariffBillingFacade;
    private final StationAvailabilityFacade stationAvailabilityFacade;

    public TripService(TripRepository tripRepository,
                       TariffBillingFacade tariffBillingFacade,
                       StationAvailabilityFacade stationAvailabilityFacade) {
        this.tripRepository = tripRepository;
        this.tariffBillingFacade = tariffBillingFacade;
        this.stationAvailabilityFacade = stationAvailabilityFacade;
    }

    public synchronized TripDTO startTrip(StartTripRequestDTO request) {
        // Validação: utilizador não pode ter outra viagem em curso
        Optional<TripDTO> activeTrip = tripRepository.findActiveTripByUser(request.userId());
        if (activeTrip.isPresent()) {
            throw new IllegalStateException("O utilizador " + request.userId() + " já tem uma viagem ativa: " + activeTrip.get().tripId());
        }

        TripDTO newTrip = new TripDTO(
                request.tripId(),
                request.userId(),
                request.bikeId(),
                request.startStationId(),
                null,
                request.startTime(),
                null,
                null,
                "IN_PROGRESS",
                null
        );

        tripRepository.save(newTrip);

        // Atualiza contadores na estação de levantamento (menos 1 bicicleta, mais 1 doca livre)
        stationAvailabilityFacade.updateAvailability(request.startStationId(), -1, 1);

        return newTrip;
    }

    public synchronized TripDTO completeTrip(CompleteTripRequestDTO request) {
        // 1. Verificação de Idempotência por returnEventId (reconexão / reenvio de docas offline)
        if (request.returnEventId() != null) {
            Optional<TripDTO> existingByEvent = tripRepository.findByReturnEventId(request.returnEventId());
            if (existingByEvent.isPresent()) {
                return existingByEvent.get();
            }
        }

        // 2. Procura a viagem
        TripDTO trip = tripRepository.findById(request.tripId())
                .orElseThrow(() -> new IllegalArgumentException("Viagem não encontrada: " + request.tripId()));

        // Idempotência por estado da própria viagem
        if ("COMPLETED".equals(trip.status())) {
            return trip;
        }

        // 3. Cálculo de duração
        long duration = Duration.between(trip.startTime(), request.endTime()).toMinutes();
        if (duration < 0) {
            duration = 0;
        }

        TripDTO completedTrip = new TripDTO(
                trip.tripId(),
                trip.userId(),
                trip.bikeId(),
                trip.startStationId(),
                request.endStationId(),
                trip.startTime(),
                request.endTime(),
                duration,
                "COMPLETED",
                request.returnEventId()
        );

        tripRepository.save(completedTrip);

        // 4. Atualiza estação de devolução (mais 1 bicicleta, menos 1 doca livre)
        stationAvailabilityFacade.updateAvailability(request.endStationId(), 1, -1);

        // 5. Aciona faturação de forma síncrona e idempotente
        tariffBillingFacade.processTripBilling(new BillingRequestDTO(
                completedTrip.tripId(),
                completedTrip.userId(),
                request.isSubscriber(),
                duration,
                request.paymentMethod()
        ));

        return completedTrip;
    }

    public Optional<TripDTO> getTripById(String tripId) {
        return tripRepository.findById(tripId);
    }

    public Optional<TripDTO> getActiveTripByUser(String userId) {
        return tripRepository.findActiveTripByUser(userId);
    }

    public Optional<TripDTO> getActiveTripByBike(String bikeId) {
        return tripRepository.findActiveTripByBike(bikeId);
    }

    public List<TripDTO> getCompletedTripsBetween(LocalDateTime start, LocalDateTime end) {
        return tripRepository.findCompletedTripsBetween(start, end);
    }

    public List<TripDTO> getTripsByUser(String userId) {
        return tripRepository.findTripsByUser(userId);
    }
}
