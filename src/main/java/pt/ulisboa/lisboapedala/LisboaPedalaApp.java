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
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Aplicação interativa em linha de comandos (CLI) para o sistema Lisboa Pedala.
 * Permite simulação manual com input do utilizador ou demonstração automática.
 */
public class LisboaPedalaApp {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final DatabaseManager dbManager;
    private final StationController stationController;
    private final DockController dockController;
    private final TripController tripController;
    private final BillingController billingController;
    private final Scanner scanner;

    public LisboaPedalaApp() {
        this.dbManager = DatabaseManager.createInMemory();
        StationAvailabilityFacade stationFacade = new StationAvailabilityFacade(dbManager);
        TariffBillingFacade billingFacade = new TariffBillingFacade(dbManager);
        TripLifecycleFacade tripFacade = new TripLifecycleFacade(dbManager, billingFacade, stationFacade);
        DockAccessFacade dockFacade = new DockAccessFacade(dbManager, tripFacade, stationFacade);

        this.stationController = new StationController(stationFacade);
        this.dockController = new DockController(dockFacade);
        this.tripController = new TripController(tripFacade);
        this.billingController = new BillingController(billingFacade);
        this.scanner = new Scanner(System.in);

        seedStations();
    }

    private void seedStations() {
        stationController.registerStation("ST-01", "Cais do Sodré", "Misericórdia", 10, 6, 4);
        stationController.registerStation("ST-02", "Saldanha", "Avenidas Novas", 12, 3, 9);
        stationController.registerStation("ST-03", "Rossio", "Santa Maria Maior", 8, 4, 4);
        stationController.registerStation("ST-04", "Belém", "Belém", 15, 2, 13);
    }

    public static void main(String[] args) {
        LisboaPedalaApp app = new LisboaPedalaApp();
        app.run();
    }

    public void run() {
        System.out.println("================================================================================");
        System.out.println("                      SISTEMA LISBOA PEDALA - MONÓLITO MODULAR                   ");
        System.out.println("                       Simulador Interativo da US-1                             ");
        System.out.println("================================================================================");

        boolean running = true;
        while (running) {
            printMainMenu();
            String option = scanner.nextLine().trim();
            switch (option) {
                case "1" -> listStations();
                case "2" -> handleUnlockBike();
                case "3" -> handleViewActiveTrip();
                case "4" -> handleReturnBike();
                case "5" -> handleViewInvoices();
                case "6" -> handleSimulateOfflineReplay();
                case "7" -> runAutoDemo();
                case "0" -> {
                    System.out.println("\nA terminar o simulador do Lisboa Pedala. Até à próxima!");
                    running = false;
                }
                default -> System.out.println("Opção inválida. Por favor escolhe uma das opções do menu.");
            }
        }
        dbManager.close();
    }

    private void printMainMenu() {
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println(" MENU PRINCIPAL:");
        System.out.println(" [1] Ver disponibilidade das estações");
        System.out.println(" [2] Desbloquear bicicleta (Início de Viagem)");
        System.out.println(" [3] Consultar viagem ativa");
        System.out.println(" [4] Devolver bicicleta (Fecho de Viagem e Cobrança)");
        System.out.println(" [5] Consultar faturas emitidas na Autoridade Tributária");
        System.out.println(" [6] Simular resiliência offline e idempotência (Reenvio de evento)");
        System.out.println(" [7] Executar demonstração automática completa");
        System.out.println(" [0] Sair");
        System.out.print("Escolhe uma opção: ");
    }

    private void listStations() {
        System.out.println("\n--- ESTADO DAS ESTAÇÕES ---");
        for (String stId : List.of("ST-01", "ST-02", "ST-03", "ST-04")) {
            stationController.getStationOccupancy(stId).ifPresent(s -> {
                System.out.printf(" * [%s] %-16s (%-18s) | Bicicletas Livres: %2d | Docas Livres: %2d (Total: %2d)%n",
                        s.stationId(), s.name(), s.parish(), s.availableBikes(), s.freeDocks(), s.totalDocks());
            });
        }
    }

    private void handleUnlockBike() {
        System.out.println("\n--- DESBLOQUEAR BICICLETA ---");
        listStations();

        System.out.print("\nIdentificador do utilizador (Enter para 'USER-01'): ");
        String userId = scanner.nextLine().trim();
        if (userId.isEmpty()) userId = "USER-01";

        // Verifica se utilizador já tem viagem ativa
        if (tripController.getActiveTrip(userId).isPresent()) {
            System.out.println("AVISO: O utilizador " + userId + " já tem uma viagem em curso! Devolve primeiro essa bicicleta.");
            return;
        }

        System.out.print("É assinante de passe anual? (s/n, default: s): ");
        String subInput = scanner.nextLine().trim();
        boolean isSubscriber = subInput.isEmpty() || subInput.equalsIgnoreCase("s");

        System.out.print("Código da estação de partida (ex: ST-01, ST-02, default: ST-01): ");
        String stationId = scanner.nextLine().trim().toUpperCase();
        if (stationId.isEmpty()) stationId = "ST-01";

        // Validação da estação
        Optional<StationOccupancyDTO> stationOpt = stationController.getStationOccupancy(stationId);
        if (stationOpt.isEmpty()) {
            System.out.println("ERRO: Estação " + stationId + " não encontrada!");
            return;
        }
        if (stationOpt.get().availableBikes() <= 0) {
            System.out.println("ERRO: A estação " + stationId + " não tem bicicletas disponíveis!");
            return;
        }

        System.out.print("Número da doca (default: DOCK-01): ");
        String dockId = scanner.nextLine().trim();
        if (dockId.isEmpty()) dockId = "DOCK-01";

        System.out.print("Identificador da bicicleta (default: BIKE-100): ");
        String bikeId = scanner.nextLine().trim();
        if (bikeId.isEmpty()) bikeId = "BIKE-100";

        System.out.print("Método de pagamento (MBWAY / CARTAO, default: MBWAY): ");
        String paymentMethod = scanner.nextLine().trim().toUpperCase();
        if (paymentMethod.isEmpty()) paymentMethod = "MBWAY";

        LocalDateTime now = LocalDateTime.now();

        System.out.println("\n>> A enviar pedido para DockController...");
        UnlockBikeResponseDTO res = dockController.unlockBike(
                stationId, dockId, bikeId, userId, isSubscriber, paymentMethod, now
        );

        if (res.success()) {
            System.out.println("SUCESSO: " + res.message());
            System.out.println("ID da Viagem Criada: " + res.tripId());
            System.out.println("Hora de início: " + now.format(TIME_FMT));
            System.out.println("Tipo de Utilizador: " + (isSubscriber ? "Assinante (45 min gratuitos)" : "Ocasional"));
        } else {
            System.out.println("FALHA: " + res.message());
        }
    }

    private void handleViewActiveTrip() {
        System.out.print("\nIdentificador do utilizador (default: USER-01): ");
        String userId = scanner.nextLine().trim();
        if (userId.isEmpty()) userId = "USER-01";

        Optional<TripDTO> tripOpt = tripController.getActiveTrip(userId);
        if (tripOpt.isEmpty()) {
            System.out.println("Nenhuma viagem ativa encontrada para o utilizador " + userId + ".");
        } else {
            TripDTO t = tripOpt.get();
            System.out.println("\n+---------------- DETALHE DA VIAGEM ATIVA ----------------+");
            System.out.printf("| ID Viagem:        %-37s |%n", t.tripId());
            System.out.printf("| Utilizador:       %-37s |%n", t.userId());
            System.out.printf("| Bicicleta:        %-37s |%n", t.bikeId());
            System.out.printf("| Estação Origem:   %-37s |%n", t.startStationId());
            System.out.printf("| Hora de Início:   %-37s |%n", t.startTime().format(TIME_FMT));
            System.out.printf("| Estado:           %-37s |%n", t.status());
            System.out.println("+----------------------------------------------------------+");
        }
    }

    private void handleReturnBike() {
        System.out.println("\n--- DEVOLVER BICICLETA ---");
        System.out.print("Identificador da bicicleta a devolver (default: BIKE-100): ");
        String bikeId = scanner.nextLine().trim();
        if (bikeId.isEmpty()) bikeId = "BIKE-100";

        listStations();
        System.out.print("\nCódigo da estação de devolução (ex: ST-02, default: ST-02): ");
        String stationId = scanner.nextLine().trim().toUpperCase();
        if (stationId.isEmpty()) stationId = "ST-02";

        Optional<StationOccupancyDTO> stationOpt = stationController.getStationOccupancy(stationId);
        if (stationOpt.isEmpty()) {
            System.out.println("ERRO: Estação não encontrada!");
            return;
        }
        if (stationOpt.get().freeDocks() <= 0) {
            System.out.println("ERRO: A estação " + stationId + " está cheia (0 docas livres)!");
            return;
        }

        System.out.print("Número da doca onde encaixas a bicicleta (default: DOCK-05): ");
        String dockId = scanner.nextLine().trim();
        if (dockId.isEmpty()) dockId = "DOCK-05";

        System.out.print("Quantos minutos durou a viagem? (ex: 25, 45, 55, 70, default: 55): ");
        String durInput = scanner.nextLine().trim();
        long minutes = 55;
        if (!durInput.isEmpty()) {
            try {
                minutes = Long.parseLong(durInput);
            } catch (NumberFormatException e) {
                System.out.println("Entrada inválida, a assumir 55 minutos.");
            }
        }

        System.out.print("O utilizador é assinante? (s/n, default: s): ");
        String subInput = scanner.nextLine().trim();
        boolean isSubscriber = subInput.isEmpty() || subInput.equalsIgnoreCase("s");

        System.out.print("Método de pagamento (MBWAY / CARTAO, default: MBWAY): ");
        String paymentMethod = scanner.nextLine().trim().toUpperCase();
        if (paymentMethod.isEmpty()) paymentMethod = "MBWAY";

        String eventId = "EVT-RET-" + System.currentTimeMillis();
        LocalDateTime returnTime = LocalDateTime.now().plusMinutes(minutes);

        System.out.println("\n>> A simular sensor mecânico de encaixe da doca e envio para DockController...");
        ReturnBikeResponseDTO res = dockController.returnBike(
                eventId, stationId, dockId, bikeId, returnTime, isSubscriber, paymentMethod
        );

        System.out.println("RESULTADO: " + res.message());
        if (res.tripId() != null && !res.tripId().startsWith("UNKNOWN")) {
            System.out.println("Viagem Concluída: " + res.tripId());
            System.out.println("\n>> A consultar fatura gerada automaticamente em tariff_billing...");
            Optional<InvoiceDTO> invoiceOpt = billingController.getInvoiceByTrip(res.tripId());
            invoiceOpt.ifPresent(this::printInvoice);
        }
    }

    private void handleViewInvoices() {
        System.out.print("\nIdentificador do utilizador (default: USER-01): ");
        String userId = scanner.nextLine().trim();
        if (userId.isEmpty()) userId = "USER-01";

        List<InvoiceDTO> invoices = billingController.getUserInvoices(userId);
        if (invoices.isEmpty()) {
            System.out.println("Nenhuma fatura encontrada para o utilizador " + userId + ".");
        } else {
            System.out.println("\nFaturas emitidas para " + userId + " (" + invoices.size() + " faturas encontradas):");
            for (InvoiceDTO inv : invoices) {
                printInvoice(inv);
            }
        }
    }

    private void handleSimulateOfflineReplay() {
        System.out.println("\n--- SIMULAÇÃO DE RESILIÊNCIA OFFLINE & IDEMPOTÊNCIA (ADR-004) ---");
        System.out.println("Cenário: Uma doca física sem rede móvel armazenou um evento de devolução.");
        System.out.println("Quando a rede volta, envia o evento. Em caso de timeout/retry da doca, o evento é reenviado.");

        String bikeId = "BIKE-OFFLINE-99";
        String userId = "USER-OFFLINE-TEST";

        // Desbloqueio
        dockController.unlockBike("ST-01", "DOCK-01", bikeId, userId, true, "MBWAY", LocalDateTime.now());
        System.out.println("1. Bicicleta " + bikeId + " desbloqueada no Cais do Sodré.");

        String eventId = "OFFLINE-EVT-SPECIAL-888";
        LocalDateTime returnTime = LocalDateTime.now().plusMinutes(60);

        System.out.println("2. Primeiro envio do evento de devolução (" + eventId + "):");
        ReturnBikeResponseDTO res1 = dockController.returnBike(eventId, "ST-02", "DOCK-02", bikeId, returnTime, true, "MBWAY");
        System.out.println("   -> Resposta: " + res1.message());

        System.out.println("3. Reenvio imediato do MESMO evento (mesmo eventId):");
        ReturnBikeResponseDTO res2 = dockController.returnBike(eventId, "ST-02", "DOCK-02", bikeId, returnTime, true, "MBWAY");
        System.out.println("   -> Resposta: " + res2.message());

        int invoiceCount = billingController.getUserInvoices(userId).size();
        System.out.printf("4. Verificação de Idempotência: Total de faturas emitidas para %s = %d (Espera-se: 1)%n",
                userId, invoiceCount);

        if (invoiceCount == 1) {
            System.out.println(">> SUCESSO: Cobrança duplicada evitada com 100% de integridade transacional!");
        } else {
            System.out.println(">> FALHA: Foi gerada fatura duplicada!");
        }
    }

    private void runAutoDemo() {
        System.out.println("\n--- A EXECUTAR DEMONSTRAÇÃO AUTOMÁTICA COMPLETA ---");
        listStations();

        String userId = "USER-DEMO-AUTO";
        String bikeId = "BIKE-AUTO-1";
        LocalDateTime start = LocalDateTime.now();

        System.out.println("\n1. Desbloquear bicicleta no Cais do Sodré:");
        UnlockBikeResponseDTO unl = dockController.unlockBike("ST-01", "DOCK-01", bikeId, userId, true, "MBWAY", start);
        System.out.println("   " + unl.message() + " | Viagem: " + unl.tripId());

        System.out.println("\n2. Estado da viagem ativa na App:");
        tripController.getActiveTrip(userId).ifPresent(t ->
                System.out.println("   Viagem ativa: " + t.tripId() + " às " + t.startTime().format(TIME_FMT)));

        System.out.println("\n3. Devolver no Saldanha com 55 min:");
        ReturnBikeResponseDTO ret = dockController.returnBike("EVT-AUTO-1", "ST-02", "DOCK-03", bikeId, start.plusMinutes(55), true, "MBWAY");
        System.out.println("   " + ret.message());

        System.out.println("\n4. Fatura gerada na AT:");
        billingController.getInvoiceByTrip(unl.tripId()).ifPresent(this::printInvoice);
    }

    private void printInvoice(InvoiceDTO inv) {
        System.out.println("+-------------------- RECIBO FISCAL (AT) --------------------+");
        System.out.printf("| Fatura ID:         %-39s |%n", inv.invoiceId());
        System.out.printf("| Utilizador:        %-39s |%n", inv.userId());
        System.out.printf("| Viagem ID:         %-39s |%n", inv.tripId());
        System.out.printf("| Valor Total:       %-39s |%n", String.format("%.2f €", inv.amountCents() / 100.0));
        System.out.printf("| IVA (23%%):         %-39s |%n", String.format("%.2f €", inv.taxCents() / 100.0));
        System.out.printf("| Pagamento:         %-39s |%n", inv.paymentMethod());
        System.out.printf("| Estado:            %-39s |%n", inv.status());
        System.out.printf("| Validação AT:      %-39s |%n", inv.atFiscalCode());
        System.out.println("| Retenção Legal:    10 anos garantidos (ADR-006)             |");
        System.out.println("+------------------------------------------------------------+");
    }
}
