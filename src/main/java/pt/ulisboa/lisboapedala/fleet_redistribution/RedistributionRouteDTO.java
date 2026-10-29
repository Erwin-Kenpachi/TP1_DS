package pt.ulisboa.lisboapedala.fleet_redistribution;

/**
 * DTO público que representa uma rota de redistribuição planeada ou ativa para carrinhas.
 */
public record RedistributionRouteDTO(
        String routeId,
        String originStationId,
        String destinationStationId,
        int bikesToMove,
        String status,
        String createdAt
) {
}

