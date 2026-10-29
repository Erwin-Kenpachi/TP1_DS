package pt.ulisboa.lisboapedala.fleet_redistribution.presentation;

import pt.ulisboa.lisboapedala.fleet_redistribution.FleetRedistributionFacade;

import java.util.List;

/**
 * Controlador da Camada de Apresentação para o módulo fleet_redistribution.
 * Ponto de entrada para as carrinhas/equipas de redistribuição consultarem rotas.
 * Comunica estritamente através da Fachada Pública FleetRedistributionFacade.
 */
public class RouteReader {

    private final FleetRedistributionFacade facade;

    public RouteReader(FleetRedistributionFacade facade) {
        this.facade = facade;
    }

    /**
     * Endpoint de consulta de rotas ativas (ex: GET /api/v1/fleet/routes/active).
     */
    public List<Object> getActiveRoutes() {
        return facade.getActiveRoutes();
    }

    /**
     * Endpoint de consulta de rotas para leitura (alias de conveniência).
     */
    public List<Object> readRoutes() {
        return facade.getActiveRoutes();
    }

    /**
     * Endpoint para consulta de rotas sugeridas pelo algoritmo de reequilíbrio.
     */
    public List<Object> getSuggestedRoutes() {
        return facade.calculateSuggestedRoutes();
    }
}

