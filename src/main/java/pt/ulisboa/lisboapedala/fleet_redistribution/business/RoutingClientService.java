package pt.ulisboa.lisboapedala.fleet_redistribution.business;

/**
 * Serviço esqueleto de cliente de rotas/mapa para o módulo fleet_redistribution.
 * Preparado para futura integração com serviços de cartografia/distâncias (ADR-003).
 */
public class RoutingClientService {

    public double estimateDistanceKm(String originStationId, String destinationStationId) {
        // Esqueleto: valor base estimado entre estações no concelho de Lisboa
        return 3.5;
    }

    public int estimateTravelTimeMinutes(String originStationId, String destinationStationId) {
        // Esqueleto: tempo estimado médio de deslocação de carrinha de suporte
        return 15;
    }
}

