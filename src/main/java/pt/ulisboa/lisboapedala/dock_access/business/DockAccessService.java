package pt.ulisboa.lisboapedala.dock_access.business;

import pt.ulisboa.lisboapedala.dock_access.DockEventDTO;
import pt.ulisboa.lisboapedala.dock_access.ReturnBikeRequestDTO;
import pt.ulisboa.lisboapedala.dock_access.ReturnBikeResponseDTO;
import pt.ulisboa.lisboapedala.dock_access.UnlockBikeRequestDTO;
import pt.ulisboa.lisboapedala.dock_access.UnlockBikeResponseDTO;
import pt.ulisboa.lisboapedala.dock_access.persistence.DockEventRepository;
import pt.ulisboa.lisboapedala.station_availability.StationAvailabilityFacade;
import pt.ulisboa.lisboapedala.trip_lifecycle.CompleteTripRequestDTO;
import pt.ulisboa.lisboapedala.trip_lifecycle.StartTripRequestDTO;
import pt.ulisboa.lisboapedala.trip_lifecycle.TripDTO;
import pt.ulisboa.lisboapedala.trip_lifecycle.TripLifecycleFacade;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class DockAccessService {

    private final DockEventRepository repository;
    private final TripLifecycleFacade tripLifecycleFacade;
    private final StationAvailabilityFacade stationAvailabilityFacade;

    public DockAccessService(DockEventRepository repository,
                             TripLifecycleFacade tripLifecycleFacade,
                             StationAvailabilityFacade stationAvailabilityFacade) {
        this.repository = repository;
        this.tripLifecycleFacade = tripLifecycleFacade;
        this.stationAvailabilityFacade = stationAvailabilityFacade;
    }

    public synchronized UnlockBikeResponseDTO unlockBike(UnlockBikeRequestDTO request) {
        String tripId = "TRIP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String eventId = "EVT-UNLOCK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 1. Regista início no ciclo de vida da viagem (falha se o utilizador já tiver viagem ativa)
        tripLifecycleFacade.startTrip(new StartTripRequestDTO(
                tripId,
                request.userId(),
                request.bikeId(),
                request.stationId(),
                request.requestedAt()
        ));

        // 2. Regista o evento local da doca física
        repository.save(new DockEventDTO(
                eventId,
                request.stationId(),
                request.dockId(),
                request.bikeId(),
                request.userId(),
                "UNLOCK",
                request.requestedAt(),
                true
        ));

        // 3. Atualiza contadores físicos na estação de levantamento (menos 1 bicicleta, mais 1 doca livre)
        stationAvailabilityFacade.updateAvailability(request.stationId(), -1, 1);

        // 4. Simula o envio de comando elétrico para o trinco físico da doca
        return new UnlockBikeResponseDTO(true, tripId, "Doca " + request.dockId() + " destrancada com sucesso.");
    }

    public synchronized ReturnBikeResponseDTO returnBike(ReturnBikeRequestDTO request) {
        // 1. Regista o evento localmente (padrão Store-and-Forward / resiliência da doca)
        repository.save(new DockEventDTO(
                request.eventId(),
                request.stationId(),
                request.dockId(),
                request.bikeId(),
                null,
                "RETURN",
                request.occurredAt(),
                false
        ));

        // 2. Identifica a viagem ativa correspondente à bicicleta
        Optional<TripDTO> activeTripOpt = tripLifecycleFacade.getActiveTripByBike(request.bikeId());

        if (activeTripOpt.isEmpty()) {
            // Pode ser um reenvio/duplicação de um evento já processado (idempotência)
            // Se o evento já foi processado pelo trip_lifecycle, o completeTrip trata de forma idempotente
            // Tentamos concluir mesmo assim caso exista identificador de viagem associado
            return new ReturnBikeResponseDTO(true, "UNKNOWN_OR_ALREADY_PROCESSED", "Evento de devolução registado ou já finalizado.");
        }

        TripDTO activeTrip = activeTripOpt.get();

        // 3. Conclui a viagem via trip_lifecycle de forma síncrona e idempotente
        TripDTO completedTrip = tripLifecycleFacade.completeTrip(new CompleteTripRequestDTO(
                activeTrip.tripId(),
                request.stationId(),
                request.occurredAt(),
                request.eventId(),
                request.isSubscriber(),
                request.paymentMethod()
        ));

        repository.markSynced(request.eventId());

        // 4. Atualiza contadores físicos na estação de devolução (mais 1 bicicleta, menos 1 doca livre)
        stationAvailabilityFacade.updateAvailability(request.stationId(), 1, -1);

        return new ReturnBikeResponseDTO(true, completedTrip.tripId(), "Bicicleta trancada e viagem concluída com sucesso.");
    }

    public synchronized List<ReturnBikeResponseDTO> syncOfflineEvents(List<ReturnBikeRequestDTO> events) {
        List<ReturnBikeResponseDTO> responses = new ArrayList<>();
        for (ReturnBikeRequestDTO event : events) {
            responses.add(returnBike(event));
        }
        return responses;
    }
}
