package pt.ulisboa.lisboapedala.fleet_redistribution.business;

import pt.ulisboa.lisboapedala.fleet_redistribution.RedistributionRouteDTO;
import pt.ulisboa.lisboapedala.fleet_redistribution.persistence.RedistributionRepository;
import pt.ulisboa.lisboapedala.station_availability.StationAvailabilityFacade;
import pt.ulisboa.lisboapedala.station_availability.StationOccupancyDTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lógica central (esqueleto) para cálculo de desequilíbrios entre estações e planeamento de rotas (US-5).
 * Comunica exclusivamente de forma síncrona com a fachada pública StationAvailabilityFacade
 * respeitando as regras estritas de dependência de ARCHITECTURE.md e ADR-005.
 */
public class CalculateRoutes {

    private final RedistributionRepository repository;
    private final StationAvailabilityFacade stationAvailabilityFacade;
    private final RoutingClientService routingClientService;

    public CalculateRoutes(RedistributionRepository repository,
                           StationAvailabilityFacade stationAvailabilityFacade,
                           RoutingClientService routingClientService) {
        this.repository = repository;
        this.stationAvailabilityFacade = stationAvailabilityFacade;
        this.routingClientService = routingClientService;
    }

    /**
     * Consulta as rotas de redistribuição ativas no repositório.
     */
    public List<Object> getActiveRoutes() {
        List<RedistributionRouteDTO> routes = repository.findActiveRoutes();
        return new ArrayList<>(routes);
    }

    /**
     * Esqueleto para identificação de desequilíbrios entre estações e geração de propostas de rota.
     * Consulta síncronamente a fachada de disponibilidade de estações.
     */
    public List<Object> calculateSuggestedRoutes() {
        List<StationOccupancyDTO> stations = stationAvailabilityFacade.getAllStations();
        List<Object> suggestedRoutes = new ArrayList<>();

        if (stations == null || stations.size() < 2) {
            return suggestedRoutes;
        }

        StationOccupancyDTO congested = null;
        StationOccupancyDTO deficit = null;

        for (StationOccupancyDTO station : stations) {
            // Estação congestionada: mais de 80% ocupada ou poucas docas livres
            if (congested == null && station.freeDocks() <= 2 && station.availableBikes() > 5) {
                congested = station;
            }
            // Estação em défice: menos de 2 bicicletas disponíveis
            if (deficit == null && station.availableBikes() <= 2 && station.freeDocks() > 3) {
                deficit = station;
            }
        }

        if (congested != null && deficit != null) {
            int bikesToMove = Math.min(congested.availableBikes() / 2, deficit.freeDocks() / 2);
            if (bikesToMove > 0) {
                RedistributionRouteDTO route = new RedistributionRouteDTO(
                        "ROUTE-AUTO-" + System.currentTimeMillis(),
                        congested.stationId(),
                        deficit.stationId(),
                        bikesToMove,
                        "ACTIVE",
                        LocalDateTime.now().toString()
                );
                repository.save(route);
                suggestedRoutes.add(route);
            }
        }

        return suggestedRoutes;
    }

    /**
     * Regista/planeia manualmente uma rota de redistribuição no repositório.
     */
    public void planRoute(String routeId, String originStationId, String destinationStationId, int bikesToMove) {
        RedistributionRouteDTO route = new RedistributionRouteDTO(
                routeId,
                originStationId,
                destinationStationId,
                bikesToMove,
                "ACTIVE",
                LocalDateTime.now().toString()
        );
        repository.save(route);
    }
}

