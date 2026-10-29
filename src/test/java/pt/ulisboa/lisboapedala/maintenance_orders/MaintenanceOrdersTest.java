package pt.ulisboa.lisboapedala.maintenance_orders;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pt.ulisboa.lisboapedala.common.DatabaseManager;
import pt.ulisboa.lisboapedala.sensor_ingestion.BikeAnomalyEventDTO;
import pt.ulisboa.lisboapedala.sensor_ingestion.SensorIngestionFacade;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MaintenanceOrdersTest {

    private DatabaseManager databaseManager;
    private SensorIngestionFacade sensorIngestionFacade;
    private MaintenanceOrdersFacade maintenanceOrdersFacade;

    @BeforeEach
    void setUp() {
        databaseManager = DatabaseManager.createInMemory();
        sensorIngestionFacade = new SensorIngestionFacade(databaseManager);
        maintenanceOrdersFacade = new MaintenanceOrdersFacade(databaseManager, sensorIngestionFacade);
    }

    @AfterEach
    void tearDown() {
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    @Test
    @DisplayName("US-2: Deve criar ordem de reparação atribuída à pool geral de qualquer técnico")
    void shouldCreateOrderFromAlertAssignedToAnyTechnician() {
        MaintenanceOrderDTO order = maintenanceOrdersFacade.createOrderFromAlert(
                "BIKE-E-101", "Avaria: travões dianteiros com falha"
        );

        assertThat(order).isNotNull();
        assertThat(order.orderId()).startsWith("MO-");
        assertThat(order.bikeId()).isEqualTo("BIKE-E-101");
        assertThat(order.status()).isEqualTo("PENDING");
        assertThat(order.technicianId()).isNull(); // Atribuída à pool geral de qualquer técnico

        List<MaintenanceOrderDTO> pending = maintenanceOrdersFacade.getPendingOrders();
        assertThat(pending).extracting(MaintenanceOrderDTO::orderId).contains(order.orderId());
    }

    @Test
    @DisplayName("US-2: Deve atribuir ordem a qualquer técnico quando bicicleta avaria e concluir reparação")
    void shouldSyncFromSensorAnomaliesAndCompleteOrder() {
        // 1. Simula reporte de avaria no sensor_ingestion (Avaria A da bicicleta)
        BikeAnomalyEventDTO anomaly = sensorIngestionFacade.reportBikeMalfunction(
                "BIKE-E-202", "BRAKE_FAILURE", "Sensor acusou perda de pressão nos travões"
        );

        // 2. Sincronização ativa (Pull) pelo módulo maintenance_orders: atribuída a qualquer técnico
        int synced = maintenanceOrdersFacade.syncFromSensorAnomalies();
        assertThat(synced).isEqualTo(1);

        List<MaintenanceOrderDTO> pending = maintenanceOrdersFacade.getPendingOrders();
        MaintenanceOrderDTO order = pending.stream()
                .filter(o -> "BIKE-E-202".equals(o.bikeId()))
                .findFirst()
                .orElseThrow();
        assertThat(order.status()).isEqualTo("PENDING");
        assertThat(order.technicianId()).isNull(); // Aberta a qualquer técnico
        assertThat(order.anomalyId()).isEqualTo(anomaly.anomalyId());

        // 3. Técnico de serviço conclui a intervenção da ordem
        MaintenanceOrderDTO completed = maintenanceOrdersFacade.completeOrder(order.orderId());
        assertThat(completed.status()).isEqualTo("RESOLVED");
        assertThat(completed.resolvedAt()).isNotNull();

        // 4. Verifica que a anomalia em sensor_ingestion foi marcada como resolvida
        List<BikeAnomalyEventDTO> unresolved = sensorIngestionFacade.getUnresolvedAnomalies();
        assertThat(unresolved).noneMatch(a -> a.anomalyId().equals(anomaly.anomalyId()));
    }

    @Test
    @DisplayName("Deve garantir idempotência na sincronização de anomalias com ordens de manutenção")
    void shouldEnsureIdempotentSyncFromAnomalies() {
        sensorIngestionFacade.reportBikeMalfunction("BIKE-303", "CRITICAL_BATTERY", "Bateria a 5%");

        int firstSync = maintenanceOrdersFacade.syncFromSensorAnomalies();
        assertThat(firstSync).isEqualTo(1);

        int secondSync = maintenanceOrdersFacade.syncFromSensorAnomalies();
        assertThat(secondSync).isEqualTo(0); // Não deve duplicar
    }
}
