package pt.ulisboa.lisboapedala.fleet_redistribution;

import pt.ulisboa.lisboapedala.common.DatabaseManager;
import pt.ulisboa.lisboapedala.fleet_redistribution.business.CalculateRoutes;
import pt.ulisboa.lisboapedala.fleet_redistribution.business.RoutingClientService;
import pt.ulisboa.lisboapedala.fleet_redistribution.persistence.RedistributionRepository;
import pt.ulisboa.lisboapedala.station_availability.StationAvailabilityFacade;

import java.util.List;

/**
 * Fachada pública do módulo fleet_redistribution.
 * Único ponto de contacto público permitido para outros módulos e apresentação.
 *
 * Em conformidade com o ADR-005 e ARCHITECTURE.md:
 * Depende exclusivamente de station_availability (via StationAvailabilityFacade).
 */
public class FleetRedistributionFacade {

    private final CalculateRoutes calculateRoutes;

    public FleetRedistributionFacade(DatabaseManager databaseManager,
                                     StationAvailabilityFacade stationAvailabilityFacade) {
        // Inicializa o esquema de base de dados privado do módulo
        databaseManager.executeScriptFromClasspath("/pt/ulisboa/lisboapedala/fleet_redistribution/persistence/schema.sql");
        RedistributionRepository repository = new RedistributionRepository(databaseManager);
        RoutingClientService routingClientService = new RoutingClientService();
        this.calculateRoutes = new CalculateRoutes(repository, stationAvailabilityFacade, routingClientService);
    }

    /**
     * Método básico de consulta de rotas ativas de redistribuição.
     *
     * @return lista de rotas ativas
     */
    public List<Object> getActiveRoutes() {
        return calculateRoutes.getActiveRoutes();
    }

    /**
     * Consulta síncrona para cálculo de rotas sugeridas com base em desequilíbrios de frota.
     *
     * @return lista de rotas sugeridas
     */
    public List<Object> calculateSuggestedRoutes() {
        return calculateRoutes.calculateSuggestedRoutes();
    }

    /**
     * Regista/planeia uma rota de redistribuição.
     */
    public void planRoute(String routeId, String originStationId, String destinationStationId, int bikesToMove) {
        calculateRoutes.planRoute(routeId, originStationId, destinationStationId, bikesToMove);
    }
}

