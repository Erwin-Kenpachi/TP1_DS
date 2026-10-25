package pt.ulisboa.lisboapedala.sensor_ingestion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pt.ulisboa.lisboapedala.common.DatabaseManager;
import pt.ulisboa.lisboapedala.sensor_ingestion.presentation.TelemetryController;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SensorIngestionTest {

    private DatabaseManager databaseManager;
    private SensorIngestionFacade facade;
    private TelemetryController controller;

    @BeforeEach
    void setUp() {
        databaseManager = DatabaseManager.createInMemory();
        facade = new SensorIngestionFacade(databaseManager);
        controller = new TelemetryController(facade);
    }

    @Test
    @DisplayName("Deve receber e persistir pacote regular de telemetria IoT")
    void shouldIngestAndPersistTelemetrySuccessfully() {
        Instant now = Instant.now();
        TelemetryDTO ping = new TelemetryDTO(
                "BIKE-001",
                85.5,
                38.7500,
                -9.1500,
                18.2,
                "OK",
                now
        );

        TelemetryDTO saved = facade.ingestTelemetry(ping);
        assertThat(saved.bikeId()).isEqualTo("BIKE-001");
        assertThat(saved.batteryLevel()).isEqualTo(85.5);

        TelemetryDTO latest = facade.getLatestTelemetry("BIKE-001");
        assertThat(latest).isNotNull();
        assertThat(latest.bikeId()).isEqualTo("BIKE-001");
        assertThat(latest.brakeStatus()).isEqualTo("OK");
        assertThat(latest.speedKmh()).isEqualTo(18.2);

        BikeHealthStatusDTO health = facade.getBikeHealthStatus("BIKE-001");
        assertThat(health.healthStatus()).isEqualTo("OPERATIONAL");
        assertThat(health.activeAlerts()).isEmpty();
    }

    @Test
    @DisplayName("Deve detetar anomalia técnica de bateria crítica (< 15%)")
    void shouldDetectCriticalBatteryAnomaly() {
        TelemetryDTO ping = new TelemetryDTO(
                "BIKE-E02",
                12.0,
                38.7200,
                -9.1400,
                15.0,
                "OK",
                Instant.now()
        );

        facade.ingestTelemetry(ping);

        List<BikeAnomalyEventDTO> anomalies = facade.getUnresolvedAnomalies();
        assertThat(anomalies).isNotEmpty();
        assertThat(anomalies).anyMatch(a -> a.bikeId().equals("BIKE-E02") && a.anomalyType().equals("CRITICAL_BATTERY"));

        BikeHealthStatusDTO health = facade.getBikeHealthStatus("BIKE-E02");
        assertThat(health.healthStatus()).isEqualTo("CRITICAL");
        assertThat(health.activeAlerts()).anyMatch(alert -> alert.contains("CRITICAL_BATTERY"));
    }

    @Test
    @DisplayName("Deve detetar avaria técnica no sistema de travões")
    void shouldDetectBrakeFailureAnomaly() {
        TelemetryDTO ping = new TelemetryDTO(
                "BIKE-E03",
                70.0,
                38.7300,
                -9.1300,
                0.0,
                "BRAKE_FAULT",
                Instant.now()
        );

        facade.ingestTelemetry(ping);

        List<BikeAnomalyEventDTO> anomalies = facade.getUnresolvedAnomalies();
        assertThat(anomalies).anyMatch(a -> a.bikeId().equals("BIKE-E03") && a.anomalyType().equals("BRAKE_FAILURE"));
    }

    @Test
    @DisplayName("Deve permitir registo manual de avaria e posterior resolução")
    void shouldAllowManualMalfunctionReportAndResolution() {
        BikeAnomalyEventDTO report = facade.reportBikeMalfunction("BIKE-E04", "CHAIN_BROKEN", "Corrente partida reportada");
        assertThat(report.anomalyId()).isNotNull();

        List<BikeAnomalyEventDTO> unresolvedBefore = facade.getUnresolvedAnomalies();
        assertThat(unresolvedBefore).anyMatch(a -> a.anomalyId().equals(report.anomalyId()));

        facade.resolveAnomaly(report.anomalyId());

        List<BikeAnomalyEventDTO> unresolvedAfter = facade.getUnresolvedAnomalies();
        assertThat(unresolvedAfter).noneMatch(a -> a.anomalyId().equals(report.anomalyId()));
    }

    @Test
    @DisplayName("Deve detetar sinal perdido em bicicletas elétricas fora de doca há mais de 2 horas")
    void shouldDetectLostBikeSignal() {
        Instant twoHoursAndHalfAgo = Instant.now().minus(Duration.ofMinutes(150));
        TelemetryDTO oldPing = new TelemetryDTO(
                "BIKE-LOST-01",
                50.0,
                38.7100,
                -9.1200,
                0.0,
                "OK",
                twoHoursAndHalfAgo
        );

        facade.ingestTelemetry(oldPing);

        Optional<BikeAnomalyEventDTO> lostAnomaly = facade.checkLostSignal("BIKE-LOST-01", Instant.now());
        assertThat(lostAnomaly).isPresent();
        assertThat(lostAnomaly.get().anomalyType()).isEqualTo("LOST_SIGNAL");
        assertThat(lostAnomaly.get().description()).contains("Ausência de sinal prolongada");
    }

    @Test
    @DisplayName("Deve notificar ouvintes assíncronos registados quando uma anomalia ocorre")
    void shouldNotifyRegisteredAnomalyListeners() {
        AtomicBoolean notified = new AtomicBoolean(false);
        facade.registerAnomalyListener(anomaly -> {
            if ("BIKE-EVENT-01".equals(anomaly.bikeId())) {
                notified.set(true);
            }
        });

        facade.reportBikeMalfunction("BIKE-EVENT-01", "FLAT_TIRE", "Pneu furado");
        assertThat(notified.get()).isTrue();
    }

    @Test
    @DisplayName("Camada de Apresentação TelemetryController deve integrar corretamente com a fachada")
    void controllerShouldIngestAndQueryThroughFacade() {
        controller.receiveTelemetryPing(
                "BIKE-CTRL-01",
                90.0,
                38.7600,
                -9.1600,
                22.0,
                "OK",
                Instant.now()
        );

        BikeHealthStatusDTO health = controller.getBikeHealth("BIKE-CTRL-01");
        assertThat(health.healthStatus()).isEqualTo("OPERATIONAL");
        assertThat(health.batteryLevel()).isEqualTo(90.0);
    }

    @Test
    @DisplayName("Deve rejeitar identificadores vazios na telemetria")
    void shouldRejectInvalidBikeId() {
        TelemetryDTO invalid = new TelemetryDTO(
                "   ",
                50.0,
                0.0,
                0.0,
                0.0,
                "OK",
                Instant.now()
        );

        assertThatThrownBy(() -> facade.ingestTelemetry(invalid))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
