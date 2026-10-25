package pt.ulisboa.lisboapedala.history_reader;

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
import pt.ulisboa.lisboapedala.tariff_billing.TariffBillingFacade;
import pt.ulisboa.lisboapedala.trip_lifecycle.TripLifecycleFacade;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HistoryReaderTest {

    private DatabaseManager databaseManager;
    private TripLifecycleFacade tripLifecycleFacade;
    private TariffBillingFacade tariffBillingFacade;
    private DockAccessFacade dockAccessFacade;
    private HistoryReaderFacade historyReaderFacade;

    @BeforeEach
    void setUp() {
        databaseManager = DatabaseManager.createInMemory();
        StationAvailabilityFacade stationFacade = new StationAvailabilityFacade(databaseManager);
        stationFacade.registerStation(new StationOccupancyDTO("ST-01", "Cais do Sodre", "Misericordia", 10, 5, 5));
        stationFacade.registerStation(new StationOccupancyDTO("ST-02", "Saldanha", "Avenidas Novas", 10, 5, 5));

        tariffBillingFacade = new TariffBillingFacade(databaseManager);
        tripLifecycleFacade = new TripLifecycleFacade(databaseManager, tariffBillingFacade, stationFacade);
        dockAccessFacade = new DockAccessFacade(databaseManager, tripLifecycleFacade, stationFacade);
        historyReaderFacade = new HistoryReaderFacade(tripLifecycleFacade, tariffBillingFacade);
    }

    @AfterEach
    void tearDown() {
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    @Test
    @DisplayName("Deve retornar lista vazia se utilizador não tiver viagens")
    void shouldReturnEmptyHistoryWhenUserHasNoTrips() {
        List<UserTripHistoryDTO> history = historyReaderFacade.getUserCompleteHistory("USER-EMPTY");
        assertThat(history).isEmpty();
    }

    @Test
    @DisplayName("Deve unificar viagem concluída com a respetiva fatura fiscal emitida")
    void shouldAggregateCompletedTripWithInvoice() {
        String userId = "USER-HIST-01";
        String bikeId = "BIKE-HIST-01";
        LocalDateTime start = LocalDateTime.of(2026, 10, 24, 10, 0);

        // Inicia viagem
        UnlockBikeRequestDTO unlockReq = new UnlockBikeRequestDTO(
                "ST-01", "DOCK-01", bikeId, userId, true, "MBWAY", start
        );
        UnlockBikeResponseDTO unlockRes = dockAccessFacade.unlockBike(unlockReq);
        assertThat(unlockRes.success()).isTrue();

        // Devolve viagem apos 60 min (45 min gratis + 1 bloco de 15 min = 0.50 EUR)
        LocalDateTime end = start.plusMinutes(60);
        ReturnBikeRequestDTO returnReq = new ReturnBikeRequestDTO(
                "EVT-RET-01", "ST-02", "DOCK-02", bikeId, end, true, "MBWAY"
        );
        ReturnBikeResponseDTO returnRes = dockAccessFacade.returnBike(returnReq);
        assertThat(returnRes.success()).isTrue();

        // Consulta via history_reader
        List<UserTripHistoryDTO> history = historyReaderFacade.getUserCompleteHistory(userId);
        assertThat(history).hasSize(1);

        UserTripHistoryDTO item = history.get(0);
        assertThat(item.trip().tripId()).isEqualTo(unlockRes.tripId());
        assertThat(item.trip().status()).isEqualTo("COMPLETED");
        assertThat(item.trip().durationMinutes()).isEqualTo(60L);
        assertThat(item.hasInvoice()).isTrue();
        assertThat(item.invoice()).isNotNull();
        assertThat(item.invoice().tripId()).isEqualTo(unlockRes.tripId());
        assertThat(item.invoice().amountCents()).isEqualTo(50);
        assertThat(item.invoice().status()).isEqualTo("PAID");

        // Consulta específica por tripId
        Optional<TripReceiptDetailsDTO> receiptDetails = historyReaderFacade.getTripReceiptDetails(unlockRes.tripId());
        assertThat(receiptDetails).isPresent();
        assertThat(receiptDetails.get().invoice().invoiceId()).isEqualTo(item.invoice().invoiceId());
    }

    @Test
    @DisplayName("Deve retornar viagem ativa sem fatura associada")
    void shouldIncludeActiveTripWithoutInvoice() {
        String userId = "USER-HIST-ACTIVE";
        String bikeId = "BIKE-ACTIVE-01";
        LocalDateTime start = LocalDateTime.now();

        UnlockBikeRequestDTO unlockReq = new UnlockBikeRequestDTO(
                "ST-01", "DOCK-01", bikeId, userId, false, "CARTAO", start
        );
        UnlockBikeResponseDTO unlockRes = dockAccessFacade.unlockBike(unlockReq);
        assertThat(unlockRes.success()).isTrue();

        List<UserTripHistoryDTO> history = historyReaderFacade.getUserCompleteHistory(userId);
        assertThat(history).hasSize(1);

        UserTripHistoryDTO item = history.get(0);
        assertThat(item.trip().status()).isEqualTo("IN_PROGRESS");
        assertThat(item.hasInvoice()).isFalse();
        assertThat(item.invoice()).isNull();
    }
}
