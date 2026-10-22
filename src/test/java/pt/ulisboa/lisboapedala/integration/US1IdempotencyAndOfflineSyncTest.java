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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validação de Robustez, Sincronização de Docas Offline e Idempotência Estrita (ADR-004 e US-1):
 * Reenvio do mesmo evento de devolução offline não pode duplicar viagem nem cobrança.
 */
class US1IdempotencyAndOfflineSyncTest {

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

        stationAvailabilityFacade.registerStation(new StationOccupancyDTO("ST-ROSSIO", "Rossio", "Santa Maria Maior", 10, 5, 5));
        stationAvailabilityFacade.registerStation(new StationOccupancyDTO("ST-BELEM", "Belém", "Belém", 10, 2, 8));
    }

    @AfterEach
    void tearDown() {
        databaseManager.close();
    }

    @Test
    @DisplayName("ADR-004: Reenvio de evento offline com mesmo eventId não duplica viagem nem fatura")
    void offlineReturnEventReplayIsStrictlyIdempotent() {
        LocalDateTime startTime = LocalDateTime.of(2026, 10, 22, 14, 0, 0);
        String userId = "USER-ROBUST-123";
        String bikeId = "BIKE-OFFLINE-77";

        // Desbloqueio online inicial
        UnlockBikeResponseDTO unlockRes = dockAccessFacade.unlockBike(new UnlockBikeRequestDTO(
                "ST-ROSSIO", "DOCK-1", bikeId, userId, true, "CREDIT_CARD", startTime
        ));
        assertThat(unlockRes.success()).isTrue();
        String tripId = unlockRes.tripId();

        // Simulação de evento retido offline na doca de Belém
        LocalDateTime returnTime = startTime.plusMinutes(70); // 70 min (> 45 min -> 2 blocos de 15 min = 1.00€)
        String eventId = "OFFLINE-EVT-ST-BELEM-001";

        ReturnBikeRequestDTO offlineEvent = new ReturnBikeRequestDTO(
                eventId,
                "ST-BELEM",
                "DOCK-4",
                bikeId,
                returnTime,
                true,
                "CREDIT_CARD"
        );

        // 1ª Entrega do evento (sincronização offline inicial)
        ReturnBikeResponseDTO firstResponse = dockAccessFacade.returnBike(offlineEvent);
        assertThat(firstResponse.success()).isTrue();

        // 2ª Entrega do MESMO evento (reenvio acidental por timeout de rede da doca)
        ReturnBikeResponseDTO secondResponse = dockAccessFacade.returnBike(offlineEvent);
        assertThat(secondResponse.success()).isTrue();

        // 3ª Entrega em lote via sincronizador de eventos offline
        List<ReturnBikeResponseDTO> batchResponses = dockAccessFacade.syncOfflineEvents(List.of(offlineEvent));
        assertThat(batchResponses).hasSize(1);
        assertThat(batchResponses.get(0).success()).isTrue();

        // Validações de integridade e idempotência:
        // A. Viagem mantida com integridade
        TripDTO trip = tripLifecycleFacade.getTripById(tripId).orElseThrow();
        assertThat(trip.status()).isEqualTo("COMPLETED");
        assertThat(trip.durationMinutes()).isEqualTo(70);
        assertThat(trip.endStationId()).isEqualTo("ST-BELEM");

        // B. Exatamente UMA fatura emitida para o utilizador
        List<InvoiceDTO> userInvoices = tariffBillingFacade.getUserInvoices(userId);
        assertThat(userInvoices).hasSize(1);
        InvoiceDTO invoice = userInvoices.get(0);
        assertThat(invoice.tripId()).isEqualTo(tripId);
        assertThat(invoice.amountCents()).isEqualTo(100); // 70 min - 45 min = 25 min -> 2 blocos -> 100 cêntimos

        // C. Disponibilidade da estação não foi inflacionada por múltiplas chamadas
        StationOccupancyDTO belemOccupancy = stationAvailabilityFacade.getStationOccupancy("ST-BELEM").orElseThrow();
        // Começou com 2 bicicletas; com exatamente uma devolução válida, tem de ter 3 (não 4 ou 5)
        assertThat(belemOccupancy.availableBikes()).isEqualTo(3);
        assertThat(belemOccupancy.freeDocks()).isEqualTo(7);
    }
}
