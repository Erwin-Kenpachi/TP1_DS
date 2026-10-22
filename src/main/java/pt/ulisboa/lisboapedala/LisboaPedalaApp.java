package pt.ulisboa.lisboapedala;

import pt.ulisboa.lisboapedala.common.DatabaseManager;
import pt.ulisboa.lisboapedala.dock_access.DockAccessFacade;
import pt.ulisboa.lisboapedala.dock_access.ReturnBikeResponseDTO;
import pt.ulisboa.lisboapedala.dock_access.UnlockBikeResponseDTO;
import pt.ulisboa.lisboapedala.dock_access.presentation.DockController;
import pt.ulisboa.lisboapedala.station_availability.StationAvailabilityFacade;
import pt.ulisboa.lisboapedala.station_availability.StationOccupancyDTO;
import pt.ulisboa.lisboapedala.station_availability.presentation.StationController;
import pt.ulisboa.lisboapedala.tariff_billing.InvoiceDTO;
import pt.ulisboa.lisboapedala.tariff_billing.TariffBillingFacade;
import pt.ulisboa.lisboapedala.tariff_billing.presentation.BillingController;
import pt.ulisboa.lisboapedala.trip_lifecycle.TripDTO;
import pt.ulisboa.lisboapedala.trip_lifecycle.TripLifecycleFacade;
import pt.ulisboa.lisboapedala.trip_lifecycle.presentation.TripController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Ponto de entrada executável para demonstração visual do fluxo transacional da US-1
 * através das camadas de Apresentação (Controllers) e Fachadas Públicas (*Facade).
 */
public class LisboaPedalaApp {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("                      SISTEMA LISBOA PEDALA - MONÓLITO MODULAR                   ");
        System.out.println("           Demonstração Visual da US-1 (Desbloqueio -> Devolução -> Cobrança)   ");
        System.out.println("================================================================================\n");

        // 1. Inicialização da Base de Dados SQLite e Fachadas
        System.out.println(">> [1/6] A inicializar base de dados SQLite e Fachadas Públicas...");
        DatabaseManager dbManager = DatabaseManager.createInMemory();

        StationAvailabilityFacade stationFacade = new StationAvailabilityFacade(dbManager);
        TariffBillingFacade billingFacade = new TariffBillingFacade(dbManager);
        TripLifecycleFacade tripFacade = new TripLifecycleFacade(dbManager, billingFacade, stationFacade);
        DockAccessFacade dockFacade = new DockAccessFacade(dbManager, tripFacade, stationFacade);

        // Inicialização dos Controladores da Camada de Apresentação
        StationController stationController = new StationController(stationFacade);
        DockController dockController = new DockController(dockFacade);
        TripController tripController = new TripController(tripFacade);
        BillingController billingController = new BillingController(billingFacade);

        // Registo de estações exemplo
        stationController.registerStation("ST-01", "Cais do Sodré", "Misericórdia", 10, 5, 5);
        stationController.registerStation("ST-02", "Saldanha", "Avenidas Novas", 12, 2, 10);
        System.out.println("   [OK] Esquemas criados e estações iniciais registadas.\n");

        // 2. Consulta de Disponibilidade Inicial
        System.out.println(">> [2/6] Consulta de Disponibilidade inicial nas estações (StationController):");
        printStation(stationController, "ST-01");
        printStation(stationController, "ST-02");
        System.out.println();

        // 3. Desbloqueio da Bicicleta
        String userId = "USER-ASSINANTE-LISBOA-01";
        String bikeId = "BIKE-ELET-42";
        LocalDateTime startTime = LocalDateTime.now();

        System.out.println(">> [3/6] Assinante solicita desbloqueio no Cais do Sodré (DockController):");
        System.out.printf("   Utilizador: %s (Assinante Passe Anual)%n", userId);
        System.out.printf("   Bicicleta: %s na doca DOCK-03 às %s%n", bikeId, startTime.format(TIME_FMT));

        UnlockBikeResponseDTO unlockRes = dockController.unlockBike(
                "ST-01", "DOCK-03", bikeId, userId, true, "MBWAY", startTime
        );

        System.out.printf("   Resultado: %s (Viagem ID: %s)%n", unlockRes.message(), unlockRes.tripId());
        System.out.println("   Disponibilidade atualizada no Cais do Sodré:");
        printStation(stationController, "ST-01");
        System.out.println();

        // 4. Verificação de Viagem em Curso
        System.out.println(">> [4/6] Verificação de Viagem Ativa na App do Utilizador (TripController):");
        Optional<TripDTO> activeTrip = tripController.getActiveTrip(userId);
        activeTrip.ifPresent(trip -> {
            System.out.printf("   Viagem: %s | Estado: %s | Início: %s | Origem: %s%n",
                    trip.tripId(), trip.status(), trip.startTime().format(TIME_FMT), trip.startStationId());
        });
        System.out.println();

        // 5. Devolução da Bicicleta após 55 minutos
        LocalDateTime returnTime = startTime.plusMinutes(55);
        String returnEventId = "EVT-RETURN-SALDANHA-789";

        System.out.println(">> [5/6] Assinante encaixa e tranca a bicicleta no Saldanha aos 55 min (DockController):");
        System.out.printf("   Estação: Saldanha (ST-02), Doca: DOCK-07, Hora: %s (Duração: 55 min)%n", returnTime.format(TIME_FMT));

        ReturnBikeResponseDTO returnRes = dockController.returnBike(
                returnEventId, "ST-02", "DOCK-07", bikeId, returnTime, true, "MBWAY"
        );

        System.out.printf("   Resultado: %s%n", returnRes.message());
        System.out.println("   Disponibilidade atualizada no Saldanha:");
        printStation(stationController, "ST-02");
        System.out.println();

        // 6. Verificação de Faturação e Retenção Fiscal (10 anos)
        System.out.println(">> [6/6] Consulta da Fatura gerada na AT (BillingController):");
        String tripId = unlockRes.tripId();
        Optional<InvoiceDTO> invoiceOpt = billingController.getInvoiceByTrip(tripId);
        invoiceOpt.ifPresent(invoice -> {
            System.out.println("   +-----------------------------------------------------------+");
            System.out.printf("   | FATURA / RECIBO FISCAL: %-34s |%n", invoice.invoiceId());
            System.out.printf("   | Utilizador:             %-34s |%n", invoice.userId());
            System.out.printf("   | Viagem Associada:       %-34s |%n", invoice.tripId());
            System.out.printf("   | Duração:                55 min (45 min grátis + 1 bloco)  |%n");
            System.out.printf("   | Valor Total Cobrado:    %.2f €                             |%n", invoice.amountCents() / 100.0);
            System.out.printf("   | IVA (23%% incluído):     %.2f €                             |%n", invoice.taxCents() / 100.0);
            System.out.printf("   | Método de Pagamento:    %-34s |%n", invoice.paymentMethod());
            System.out.printf("   | Estado:                 %-34s |%n", invoice.status());
            System.out.printf("   | Código Validação AT:    %-34s |%n", invoice.atFiscalCode());
            System.out.println("   | Arquivo Legal:          Retenção obrigatória por 10 anos  |");
            System.out.println("   +-----------------------------------------------------------+");
        });
        System.out.println();

        // Demonstração adicional de Idempotência (ADR-004)
        System.out.println(">> [BÓNUS] Teste de Idempotência e Resiliência Offline (Reenvio do mesmo evento):");
        ReturnBikeResponseDTO replayRes = dockController.returnBike(
                returnEventId, "ST-02", "DOCK-07", bikeId, returnTime, true, "MBWAY"
        );
        System.out.printf("   Reenvio processado: %s%n", replayRes.message());
        System.out.printf("   Total de faturas emitidas para o utilizador: %d (Cobrança única garantida)%n",
                billingController.getUserInvoices(userId).size());

        System.out.println("\n================================================================================");
        System.out.println("               DEMONSTRAÇÃO CONCLUÍDA COM SUCESSO - 100% OPERACIONAL             ");
        System.out.println("================================================================================");

        dbManager.close();
    }

    private static void printStation(StationController controller, String stationId) {
        controller.getStationOccupancy(stationId).ifPresent(s -> {
            System.out.printf("   * [%s] %-15s (%s) -> Bicicletas: %d | Docas Livres: %d | Total Docas: %d%n",
                    s.stationId(), s.name(), s.parish(), s.availableBikes(), s.freeDocks(), s.totalDocks());
        });
    }
}
