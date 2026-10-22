package pt.ulisboa.lisboapedala.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pt.ulisboa.lisboapedala.common.DatabaseManager;
import pt.ulisboa.lisboapedala.dock_access.DockAccessFacade;
import pt.ulisboa.lisboapedala.dock_access.ReturnBikeRequestDTO;
import pt.ulisboa.lisboapedala.dock_access.ReturnBikeResponseDTO;
import pt.ulisboa.lisboapedala.dock_access.UnlockBikeRequestDTO;
import pt.ulisboa.lisboapedala.dock_access.UnlockBikeResponseDTO;
import pt.ulisboa.lisboapedala.station_availability.StationAvailabilityFacade;
import pt.ulisboa.lisboapedala.station_availability.StationOccupancyDTO;
import pt.ulisboa.lisboapedala.tariff_billing.InvoiceDTO;
import pt.ulisboa.lisboapedala.tariff_billing.TariffBillingFacade;
import pt.ulisboa.lisboapedala.trip_lifecycle.TripDTO;
import pt.ulisboa.lisboapedala.trip_lifecycle.TripLifecycleFacade;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de integração do fluxo transacional da US-1:
 * "Como assinante, desbloqueio e devolvo uma bicicleta e a viagem fica registada e cobrada."
 */
class US1TransactionalFlowIntegrationTest {

    private DatabaseManager databaseManager;
    private StationAvailabilityFacade stationAvailabilityFacade;
    private TariffBillingFacade tariffBillingFacade;
    private TripLifecycleFacade tripLifecycleFacade;
    private DockAccessFacade dockAccessFacade;

    @BeforeEach
    void setUp() {
        databaseManager = DatabaseManager.createInMemory();
        stationAvailabilityFacade = new StationAvailabilityFacade(databaseManager);
        tariffBillingFacade = new TariffBillingFacade(databaseManager);
        tripLifecycleFacade = new TripLifecycleFacade(databaseManager, tariffBillingFacade, stationAvailabilityFacade);
        dockAccessFacade = new DockAccessFacade(databaseManager, tripLifecycleFacade, stationAvailabilityFacade);

        // Configuração inicial das estações
        stationAvailabilityFacade.registerStation(new StationOccupancyDTO("STATION-01", "Cais do Sodré", "Misericórdia", 10, 5, 5));
        stationAvailabilityFacade.registerStation(new StationOccupancyDTO("STATION-02", "Saldanha", "Avenidas Novas", 12, 2, 10));
    }

    @AfterEach
    void tearDown() {
        databaseManager.close();
    }

    @Test
    @DisplayName("US-1: Assinante desbloqueia bicicleta, utiliza 55 min, devolve noutra estação e é faturado 0.50€")
    void subscriberUnlocksUsesAndReturnsBikeSuccessfully() {
        LocalDateTime startTime = LocalDateTime.of(2026, 10, 22, 10, 0, 0);
        String userId = "USER-ASSINANTE-007";
        String bikeId = "BIKE-ELECTRIC-42";

        // 1. Desbloqueio
        UnlockBikeRequestDTO unlockReq = new UnlockBikeRequestDTO(
                "STATION-01",
                "DOCK-03",
                bikeId,
                userId,
                true, // assinante
                "MBWAY",
                startTime
        );

        UnlockBikeResponseDTO unlockRes = dockAccessFacade.unlockBike(unlockReq);
        assertThat(unlockRes.success()).isTrue();
        String tripId = unlockRes.tripId();
        assertThat(tripId).startsWith("TRIP-");

        // Validação da disponibilidade na estação de partida (5 -> 4 bicicletas, 5 -> 6 docas livres)
        StationOccupancyDTO station1Occupancy = stationAvailabilityFacade.getStationOccupancy("STATION-01").orElseThrow();
        assertThat(station1Occupancy.availableBikes()).isEqualTo(4);
        assertThat(station1Occupancy.freeDocks()).isEqualTo(6);

        // Validação de viagem ativa no ciclo de vida
        Optional<TripDTO> activeTrip = tripLifecycleFacade.getActiveTripByUser(userId);
        assertThat(activeTrip).isPresent();
        assertThat(activeTrip.get().status()).isEqualTo("IN_PROGRESS");
        assertThat(activeTrip.get().bikeId()).isEqualTo(bikeId);

        // 2. Devolução após 55 minutos na Estação 2
        LocalDateTime returnTime = startTime.plusMinutes(55);
        String eventId = "EVT-RETURN-999";
        ReturnBikeRequestDTO returnReq = new ReturnBikeRequestDTO(
                eventId,
                "STATION-02",
                "DOCK-08",
                bikeId,
                returnTime,
                true,
                "MBWAY"
        );

        ReturnBikeResponseDTO returnRes = dockAccessFacade.returnBike(returnReq);
        assertThat(returnRes.success()).isTrue();

        // Validação da disponibilidade na estação de chegada (2 -> 3 bicicletas, 10 -> 9 docas livres)
        StationOccupancyDTO station2Occupancy = stationAvailabilityFacade.getStationOccupancy("STATION-02").orElseThrow();
        assertThat(station2Occupancy.availableBikes()).isEqualTo(3);
        assertThat(station2Occupancy.freeDocks()).isEqualTo(9);

        // Validação de encerramento da viagem
        TripDTO completedTrip = tripLifecycleFacade.getTripById(tripId).orElseThrow();
        assertThat(completedTrip.status()).isEqualTo("COMPLETED");
        assertThat(completedTrip.durationMinutes()).isEqualTo(55);
        assertThat(completedTrip.endStationId()).isEqualTo("STATION-02");

        // 3. Validação da Faturação e Cobrança
        Optional<InvoiceDTO> invoiceOpt = tariffBillingFacade.getInvoiceByTripId(tripId);
        assertThat(invoiceOpt).isPresent();
        InvoiceDTO invoice = invoiceOpt.get();

        // Assinante com 55 min: 45 min gratuitos + 1 bloco de 15 min = 50 cêntimos
        assertThat(invoice.amountCents()).isEqualTo(50);
        assertThat(invoice.status()).isEqualTo("PAID");
        assertThat(invoice.paymentMethod()).isEqualTo("MBWAY");
        assertThat(invoice.atFiscalCode()).startsWith("AT-2026-");
        assertThat(invoice.userId()).isEqualTo(userId);
    }
}
