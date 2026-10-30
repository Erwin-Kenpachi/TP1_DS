package pt.ulisboa.lisboapedala.reporting_audit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pt.ulisboa.lisboapedala.common.DatabaseManager;
import pt.ulisboa.lisboapedala.reporting_audit.presentation.ReportController;
import pt.ulisboa.lisboapedala.tariff_billing.TariffBillingFacade;
import pt.ulisboa.lisboapedala.trip_lifecycle.CompleteTripRequestDTO;
import pt.ulisboa.lisboapedala.trip_lifecycle.StartTripRequestDTO;
import pt.ulisboa.lisboapedala.trip_lifecycle.TripDTO;
import pt.ulisboa.lisboapedala.trip_lifecycle.TripLifecycleFacade;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ReportingAuditTest {

    private DatabaseManager databaseManager;
    private TripLifecycleFacade tripLifecycleFacade;
    private ReportingAuditFacade reportingAuditFacade;
    private ReportController reportController;

    @BeforeEach
    void setUp() {
        databaseManager = DatabaseManager.createInMemory();
        TariffBillingFacade tariffBillingFacade = new TariffBillingFacade(databaseManager);
        tripLifecycleFacade = new TripLifecycleFacade(databaseManager, tariffBillingFacade);
        
        // reporting_audit depende EXCLUSIVAMENTE de tripLifecycleFacade (Ce = 1)
        reportingAuditFacade = new ReportingAuditFacade(databaseManager, tripLifecycleFacade);
        reportController = new ReportController(reportingAuditFacade);

        // Registar estações distribuídas por freguesias de Lisboa no cadastro municipal
        reportingAuditFacade.registerStation("ST-SALDANHA", "Saldanha", "Avenidas Novas", 20, 10, 10);
        reportingAuditFacade.registerStation("ST-CAMPO-GRANDE", "Campo Grande", "Alvalade", 25, 15, 10);
        reportingAuditFacade.registerStation("ST-BELEM", "Torre de Belém", "Belém", 15, 5, 10);
    }

    @Test
    @DisplayName("US-4: Deve gerar com sucesso o relatório de utilização agregada por freguesia")
    void shouldGenerateParishUsageReportSuccessfully() {
        LocalDateTime t1 = LocalDateTime.of(2026, 10, 15, 9, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 10, 15, 9, 30); // 30 min

        // Viagem 1: Saldanha (Avenidas Novas) -> Campo Grande (Alvalade)
        TripDTO trip1 = tripLifecycleFacade.startTrip(new StartTripRequestDTO("TRIP-01", "USER-01", "BIKE-01", "ST-SALDANHA", t1));
        tripLifecycleFacade.completeTrip(new CompleteTripRequestDTO(trip1.tripId(), "ST-CAMPO-GRANDE", t2, "EVT-01", true, "MBWAY"));

        // Viagem 2: Campo Grande (Alvalade) -> Belém (Belém) - 40 min
        LocalDateTime t3 = LocalDateTime.of(2026, 10, 15, 14, 0);
        LocalDateTime t4 = LocalDateTime.of(2026, 10, 15, 14, 40);
        TripDTO trip2 = tripLifecycleFacade.startTrip(new StartTripRequestDTO("TRIP-02", "USER-02", "BIKE-02", "ST-CAMPO-GRANDE", t3));
        tripLifecycleFacade.completeTrip(new CompleteTripRequestDTO(trip2.tripId(), "ST-BELEM", t4, "EVT-02", false, "CARD"));

        ParishUsageReportDTO report = reportingAuditFacade.getParishUsageReport();

        assertThat(report).isNotNull();
        assertThat(report.totalTrips()).isEqualTo(2);
        assertThat(report.totalDurationMinutes()).isEqualTo(70);
        assertThat(report.totalCo2SavedKg()).isGreaterThan(0.0);

        // Validar agregação por freguesia
        assertThat(report.parishStats()).isNotEmpty();
        assertThat(report.parishStats()).anyMatch(stat -> stat.parish().equals("Avenidas Novas") && stat.tripsStarted() == 1);
        assertThat(report.parishStats()).anyMatch(stat -> stat.parish().equals("Alvalade") && stat.tripsStarted() == 1 && stat.tripsEnded() == 1);
        assertThat(report.parishStats()).anyMatch(stat -> stat.parish().equals("Belém") && stat.tripsEnded() == 1);
    }

    @Test
    @DisplayName("Requisito 7: Deve gerar e persistir o relatório mensal municipal consolidado com CO2 evitado")
    void shouldGenerateAndPersistMonthlyMunicipalReport() {
        LocalDateTime t1 = LocalDateTime.of(2026, 10, 10, 10, 0);
        LocalDateTime t2 = LocalDateTime.of(2026, 10, 10, 10, 45); // 45 min

        TripDTO trip = tripLifecycleFacade.startTrip(new StartTripRequestDTO("TRIP-03", "USER-03", "BIKE-03", "ST-SALDANHA", t1));
        tripLifecycleFacade.completeTrip(new CompleteTripRequestDTO(trip.tripId(), "ST-SALDANHA", t2, "EVT-03", true, "MBWAY"));

        MonthlyMunicipalReportDTO monthlyReport = reportingAuditFacade.generateMonthlyReport(2026, 10);

        assertThat(monthlyReport).isNotNull();
        assertThat(monthlyReport.year()).isEqualTo(2026);
        assertThat(monthlyReport.month()).isEqualTo(10);
        assertThat(monthlyReport.totalTrips()).isEqualTo(1);
        assertThat(monthlyReport.totalDurationMinutes()).isEqualTo(45);
        assertThat(monthlyReport.estimatedDistanceKm()).isGreaterThan(10.0);
        assertThat(monthlyReport.totalCo2SavedKg()).isGreaterThan(1.0);

        // Verificar consulta posterior ao relatório arquivado
        Optional<MonthlyMunicipalReportDTO> archived = reportingAuditFacade.getMonthlyReport(2026, 10);
        assertThat(archived).isPresent();
        assertThat(archived.get().reportId()).isEqualTo(monthlyReport.reportId());
        assertThat(archived.get().totalTrips()).isEqualTo(1);
    }

    @Test
    @DisplayName("Deve preparar métricas de estações consolidadas para o portal de Dados Abertos")
    void shouldGenerateStationOpenData() {
        StationOpenDataDTO openData = reportingAuditFacade.getStationOpenData();

        assertThat(openData).isNotNull();
        assertThat(openData.totalStations()).isEqualTo(3);
        assertThat(openData.totalBikesAvailable()).isEqualTo(30); // 10 + 15 + 5
        assertThat(openData.totalDocksFree()).isEqualTo(30);       // 10 + 10 + 10

        assertThat(openData.stations()).hasSize(3);
        assertThat(openData.stations()).anyMatch(st -> st.name().equals("Saldanha") && st.occupancyRate() == 50.0);
    }

    @Test
    @DisplayName("Controlador de Apresentação ReportController deve expor a fachada corretamente")
    void controllerShouldExposeFacadeMethods() {
        ParishUsageReportDTO parishReport = reportController.getParishUsageReport();
        assertThat(parishReport).isNotNull();

        StationOpenDataDTO openData = reportController.getStationOpenData();
        assertThat(openData).isNotNull();
        assertThat(openData.totalStations()).isEqualTo(3);
    }
}
