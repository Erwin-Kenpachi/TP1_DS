package pt.ulisboa.lisboapedala.fleet_redistribution;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pt.ulisboa.lisboapedala.common.DatabaseManager;
import pt.ulisboa.lisboapedala.fleet_redistribution.presentation.RouteReader;
import pt.ulisboa.lisboapedala.station_availability.StationAvailabilityFacade;
import pt.ulisboa.lisboapedala.station_availability.StationOccupancyDTO;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FleetRedistributionTest {

    private DatabaseManager databaseManager;
    private StationAvailabilityFacade stationAvailabilityFacade;
    private FleetRedistributionFacade fleetRedistributionFacade;
    private RouteReader routeReader;

    @BeforeEach
    void setUp() {
        databaseManager = DatabaseManager.createInMemory();
        stationAvailabilityFacade = new StationAvailabilityFacade(databaseManager);
        fleetRedistributionFacade = new FleetRedistributionFacade(databaseManager, stationAvailabilityFacade);
        routeReader = new RouteReader(fleetRedistributionFacade);
    }

    @Test
    @DisplayName("Esqueleto: Deve inicializar o repositório e consultar rotas ativas através da fachada e RouteReader")
    void shouldInitializeAndExposeActiveRoutesViaFacadeAndController() {
        assertThat(fleetRedistributionFacade.getActiveRoutes()).isEmpty();
        assertThat(routeReader.getActiveRoutes()).isEmpty();

        fleetRedistributionFacade.planRoute("ROUTE-01", "ST-01", "ST-02", 5);

        List<Object> activeRoutes = fleetRedistributionFacade.getActiveRoutes();
        assertThat(activeRoutes).hasSize(1);

        Object firstRoute = activeRoutes.getFirst();
        assertThat(firstRoute).isInstanceOf(RedistributionRouteDTO.class);
        RedistributionRouteDTO dto = (RedistributionRouteDTO) firstRoute;
        assertThat(dto.routeId()).isEqualTo("ROUTE-01");
        assertThat(dto.originStationId()).isEqualTo("ST-01");
        assertThat(dto.destinationStationId()).isEqualTo("ST-02");
        assertThat(dto.bikesToMove()).isEqualTo(5);

        // Validar que a camada de apresentação reflete a fachada
        assertThat(routeReader.getActiveRoutes()).hasSize(1);
        assertThat(routeReader.readRoutes()).hasSize(1);
    }

    @Test
    @DisplayName("Esqueleto: Deve consultar síncronamente StationAvailabilityFacade e calcular rota sugerida")
    void shouldCalculateSuggestedRoutesWhenStationImbalanceExists() {
        // Registar uma estação congestionada (muitas bicicletas, poucas docas)
        stationAvailabilityFacade.registerStation(new StationOccupancyDTO("ST-CONGESTED", "Baixa", "Santa Maria Maior", 20, 18, 2));
        // Registar uma estação em défice (poucas bicicletas, muitas docas)
        stationAvailabilityFacade.registerStation(new StationOccupancyDTO("ST-DEFICIT", "Saldanha", "Avenidas Novas", 20, 1, 19));

        List<Object> suggested = fleetRedistributionFacade.calculateSuggestedRoutes();
        assertThat(suggested).isNotEmpty();

        List<Object> activeFromReader = routeReader.getSuggestedRoutes();
        assertThat(activeFromReader).isNotEmpty();
    }
}

