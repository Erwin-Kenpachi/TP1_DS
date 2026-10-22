package pt.ulisboa.lisboapedala;

import pt.ulisboa.lisboapedala.common.DatabaseManager;
import pt.ulisboa.lisboapedala.common.UserRepository;
import pt.ulisboa.lisboapedala.common.UserRepository.UserProfile;
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
import java.util.UUID;

/**
 * Aplicacao interativa em linha de comandos (CLI) para o sistema Lisboa Pedala.
 * Usa persistencia em ficheiro SQLite para manter dados entre sessoes.
 * Inclui ecra de registo/login antes de aceder ao menu principal.
 */
public class LisboaPedalaApp {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final String DB_FILE = "lisboapedala.db";

    private final DatabaseManager dbManager;
    private final UserRepository userRepository;
    private final StationController stationController;
    private final DockController dockController;
    private final TripController tripController;
    private final BillingController billingController;
    private final Scanner scanner;

    /** Utilizador autenticado na sessao actual. */
    private UserProfile currentUser;

    public LisboaPedalaApp() {
        // Persistencia em ficheiro SQLite — os dados sobrevivem ao fecho da aplicacao
        this.dbManager = DatabaseManager.createForFile(DB_FILE);
        this.userRepository = new UserRepository(dbManager);

        StationAvailabilityFacade stationFacade = new StationAvailabilityFacade(dbManager);
        TariffBillingFacade billingFacade = new TariffBillingFacade(dbManager);
        TripLifecycleFacade tripFacade = new TripLifecycleFacade(dbManager, billingFacade, stationFacade);
        DockAccessFacade dockFacade = new DockAccessFacade(dbManager, tripFacade, stationFacade);

        this.stationController = new StationController(stationFacade);
        this.dockController = new DockController(dockFacade);
        this.tripController = new TripController(tripFacade);
        this.billingController = new BillingController(billingFacade);
        this.scanner = new Scanner(System.in);

        seedStationsIfNeeded(stationFacade);
    }

    /**
     * Regista as estacoes apenas se a base de dados ainda nao as tiver
     * (primeira execucao da aplicacao).
     */
    private void seedStationsIfNeeded(StationAvailabilityFacade stationFacade) {
        if (stationFacade.getStationOccupancy("ST-01").isPresent()) {
            return; // ja inicializada
        }
        stationController.registerStation("ST-01", "Cais do Sodre",     "Misericordia",      10, 6,  4);
        stationController.registerStation("ST-02", "Saldanha",           "Avenidas Novas",    12, 3,  9);
        stationController.registerStation("ST-03", "Rossio",             "Santa Maria Maior",  8, 4,  4);
        stationController.registerStation("ST-04", "Belem",              "Belem",             15, 2, 13);
        stationController.registerStation("ST-05", "Marques de Pombal",  "Santo Antonio",     10, 5,  5);
        stationController.registerStation("ST-06", "Intendente",         "Arroios",           10, 3,  7);
    }

    public static void main(String[] args) {
        LisboaPedalaApp app = new LisboaPedalaApp();
        app.run();
    }

    public void run() {
        printBanner();

        // Ecra de autenticacao
        if (!authenticationFlow()) {
            System.out.println("A sair...");
            dbManager.close();
            return;
        }

        // Menu principal (sessao iniciada)
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
                    System.out.println("\nSessao terminada. Ate a proxima, " + currentUser.username() + "!");
                    running = false;
                }
                default -> System.out.println("Opcao invalida. Por favor escolhe uma das opcoes do menu.");
            }
        }
        dbManager.close();
    }

    // -------------------------------------------------------------------------
    // Autenticacao
    // -------------------------------------------------------------------------

    private boolean authenticationFlow() {
        while (true) {
            System.out.println("\n========================================");
            System.out.println("  [1] Iniciar sessao (Login)");
            System.out.println("  [2] Criar conta (Registo)");
            System.out.println("  [0] Sair");
            System.out.print("Escolhe uma opcao: ");
            String opt = scanner.nextLine().trim();

            switch (opt) {
                case "1" -> { if (handleLogin()) return true; }
                case "2" -> handleRegister();
                case "0" -> { return false; }
                default  -> System.out.println("Opcao invalida.");
            }
        }
    }

    private boolean handleLogin() {
        System.out.println("\n--- INICIAR SESSAO ---");
        System.out.print("Nome de utilizador: ");
        String username = scanner.nextLine().trim();
        System.out.print("Palavra-passe: ");
        String password = scanner.nextLine().trim();

        Optional<UserProfile> profileOpt = userRepository.login(username, password);
        if (profileOpt.isEmpty()) {
            System.out.println("Nome de utilizador ou palavra-passe incorretos.");
            return false;
        }
        currentUser = profileOpt.get();
        System.out.println("\nBem-vindo(a), " + currentUser.username() + "!"
                + (currentUser.isSubscriber() ? "  [Assinante Passe Anual]" : "  [Utilizador Ocasional]"));
        return true;
    }

    private void handleRegister() {
        System.out.println("\n--- CRIAR CONTA ---");
        System.out.print("Nome de utilizador (unico): ");
        String username = scanner.nextLine().trim();
        if (username.isBlank()) {
            System.out.println("Nome de utilizador nao pode ser vazio.");
            return;
        }
        if (userRepository.usernameExists(username)) {
            System.out.println("Esse nome de utilizador ja existe. Escolhe outro.");
            return;
        }

        System.out.print("Palavra-passe: ");
        String password = scanner.nextLine().trim();
        if (password.isBlank()) {
            System.out.println("Palavra-passe nao pode ser vazia.");
            return;
        }

        System.out.print("Tens passe anual? (s/n, default: n): ");
        String subInput = scanner.nextLine().trim();
        boolean isSubscriber = subInput.equalsIgnoreCase("s");

        System.out.print("Metodo de pagamento preferido (MBWAY / CARTAO, default: MBWAY): ");
        String payMethod = scanner.nextLine().trim().toUpperCase();
        if (payMethod.isEmpty() || (!payMethod.equals("MBWAY") && !payMethod.equals("CARTAO"))) {
            payMethod = "MBWAY";
        }

        String userId = "USER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        boolean ok = userRepository.register(userId, username, password, isSubscriber, payMethod);
        if (ok) {
            System.out.println("Conta criada com sucesso! Ja podes iniciar sessao.");
        } else {
            System.out.println("Erro ao criar conta. Tenta novamente.");
        }
    }

    // -------------------------------------------------------------------------
    // Banner e menu
    // -------------------------------------------------------------------------

    private void printBanner() {
        System.out.println("================================================================================");
        System.out.println("                 SISTEMA LISBOA PEDALA - MONOLITO MODULAR                      ");
        System.out.println("                      Simulador Interativo da US-1                             ");
        System.out.println("================================================================================");
    }

    private void printMainMenu() {
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println(" Sessao: " + currentUser.username()
                + "  |  " + (currentUser.isSubscriber() ? "Assinante (45 min gratis)" : "Utilizador Ocasional")
                + "  |  Pagamento: " + currentUser.paymentMethod());
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println(" [1] Ver disponibilidade das estacoes");
        System.out.println(" [2] Desbloquear bicicleta (Inicio de Viagem)");
        System.out.println(" [3] Consultar a minha viagem ativa");
        System.out.println(" [4] Devolver bicicleta (Fecho de Viagem e Cobranca)");
        System.out.println(" [5] Consultar as minhas faturas");
        System.out.println(" [6] Simular resiliencia offline e idempotencia (ADR-004)");
        System.out.println(" [7] Executar demonstracao automatica completa");
        System.out.println(" [0] Terminar sessao");
        System.out.print("Escolhe uma opcao: ");
    }

    // -------------------------------------------------------------------------
    // Opcoes do menu
    // -------------------------------------------------------------------------

    private void listStations() {
        System.out.println("\n--- ESTACOES DISPONIVEIS ---");
        for (String stId : List.of("ST-01", "ST-02", "ST-03", "ST-04", "ST-05", "ST-06")) {
            stationController.getStationOccupancy(stId).ifPresent(s -> {
                System.out.printf(" [%s] %-18s (%-18s) | Bikes livres: %2d | Docas livres: %2d (Total: %2d)%n",
                        s.stationId(), s.name(), s.parish(),
                        s.availableBikes(), s.freeDocks(), s.totalDocks());
            });
        }
    }

    private void handleUnlockBike() {
        System.out.println("\n--- DESBLOQUEAR BICICLETA ---");

        // Impede desbloquear se ja tem viagem em curso
        if (tripController.getActiveTrip(currentUser.userId()).isPresent()) {
            System.out.println("AVISO: Ja tens uma viagem em curso! Devolve primeiro a bicicleta atual.");
            System.out.println("       Usa a opcao [3] para consultar a viagem ativa.");
            return;
        }

        listStations();

        System.out.print("\nCodigo da estacao de partida (ex: ST-01, default: ST-01): ");
        String stationId = scanner.nextLine().trim().toUpperCase();
        if (stationId.isEmpty()) stationId = "ST-01";

        Optional<StationOccupancyDTO> stationOpt = stationController.getStationOccupancy(stationId);
        if (stationOpt.isEmpty()) {
            System.out.println("ERRO: Estacao " + stationId + " nao encontrada.");
            return;
        }
        if (stationOpt.get().availableBikes() <= 0) {
            System.out.println("ERRO: A estacao " + stationId + " nao tem bicicletas disponiveis.");
            return;
        }

        System.out.print("Numero da doca (default: DOCK-01): ");
        String dockId = scanner.nextLine().trim();
        if (dockId.isEmpty()) dockId = "DOCK-01";

        System.out.print("Identificador da bicicleta (default: BIKE-100): ");
        String bikeId = scanner.nextLine().trim();
        if (bikeId.isEmpty()) bikeId = "BIKE-100";

        LocalDateTime now = LocalDateTime.now();
        System.out.println("\n>> A enviar pedido de desbloqueio a doca " + dockId + "...");

        UnlockBikeResponseDTO res = dockController.unlockBike(
                stationId, dockId, bikeId,
                currentUser.userId(),
                currentUser.isSubscriber(),
                currentUser.paymentMethod(),
                now
        );

        if (res.success()) {
            System.out.println("SUCESSO: " + res.message());
            System.out.println("  ID da Viagem: " + res.tripId());
            System.out.println("  Hora de inicio: " + now.format(TIME_FMT));
            if (currentUser.isSubscriber()) {
                System.out.println("  Os primeiros 45 minutos sao gratuitos.");
            }
        } else {
            System.out.println("FALHA: " + res.message());
        }
    }

    private void handleViewActiveTrip() {
        Optional<TripDTO> tripOpt = tripController.getActiveTrip(currentUser.userId());
        if (tripOpt.isEmpty()) {
            System.out.println("\nNao tens nenhuma viagem ativa de momento.");
        } else {
            TripDTO t = tripOpt.get();
            long elapsed = java.time.Duration.between(t.startTime(), LocalDateTime.now()).toMinutes();
            System.out.println("\n+---------------- VIAGEM ATIVA ----------------+");
            System.out.printf("| ID Viagem:      %-29s |%n", t.tripId());
            System.out.printf("| Bicicleta:      %-29s |%n", t.bikeId());
            System.out.printf("| Estacao saida:  %-29s |%n", t.startStationId());
            System.out.printf("| Hora de inicio: %-29s |%n", t.startTime().format(TIME_FMT));
            System.out.printf("| Em curso ha:    %-29s |%n", elapsed + " min");
            if (currentUser.isSubscriber() && elapsed < 45) {
                System.out.printf("| Tempo gratis:   %-29s |%n", "restam " + (45 - elapsed) + " min");
            }
            System.out.println("+-----------------------------------------------+");
        }
    }

    private void handleReturnBike() {
        System.out.println("\n--- DEVOLVER BICICLETA ---");

        Optional<TripDTO> activeTripOpt = tripController.getActiveTrip(currentUser.userId());
        if (activeTripOpt.isEmpty()) {
            System.out.println("Nao tens nenhuma viagem ativa para devolver.");
            return;
        }

        TripDTO activeTrip = activeTripOpt.get();
        System.out.println("Viagem em curso: " + activeTrip.tripId()
                + " | Bicicleta: " + activeTrip.bikeId()
                + " | Saiu de: " + activeTrip.startStationId());

        listStations();
        System.out.print("\nCodigo da estacao de devolucao (default: ST-02): ");
        String stationId = scanner.nextLine().trim().toUpperCase();
        if (stationId.isEmpty()) stationId = "ST-02";

        Optional<StationOccupancyDTO> stationOpt = stationController.getStationOccupancy(stationId);
        if (stationOpt.isEmpty()) {
            System.out.println("ERRO: Estacao nao encontrada.");
            return;
        }
        if (stationOpt.get().freeDocks() <= 0) {
            System.out.println("ERRO: A estacao " + stationId + " esta cheia (0 docas livres).");
            return;
        }

        System.out.print("Numero da doca onde encaixas a bicicleta (default: DOCK-05): ");
        String dockId = scanner.nextLine().trim();
        if (dockId.isEmpty()) dockId = "DOCK-05";

        LocalDateTime returnTime = LocalDateTime.now();
        String eventId = "EVT-RET-" + System.currentTimeMillis();

        System.out.println("\n>> A simular encaixe mecanico na doca " + dockId + "...");
        ReturnBikeResponseDTO res = dockController.returnBike(
                eventId, stationId, dockId,
                activeTrip.bikeId(),
                returnTime,
                currentUser.isSubscriber(),
                currentUser.paymentMethod()
        );

        System.out.println("RESULTADO: " + res.message());
        if (res.tripId() != null && !res.tripId().startsWith("UNKNOWN")) {
            System.out.println("  Viagem concluida: " + res.tripId());
            System.out.println("\n>> A consultar fatura gerada automaticamente...");
            billingController.getInvoiceByTrip(res.tripId()).ifPresent(this::printInvoice);
        }
    }

    private void handleViewInvoices() {
        List<InvoiceDTO> invoices = billingController.getUserInvoices(currentUser.userId());
        if (invoices.isEmpty()) {
            System.out.println("\nNenhuma fatura encontrada para a tua conta.");
        } else {
            System.out.println("\nFaturas emitidas para " + currentUser.username()
                    + " (" + invoices.size() + " fatura(s)):");
            for (InvoiceDTO inv : invoices) {
                printInvoice(inv);
            }
        }
    }

    private void handleSimulateOfflineReplay() {
        System.out.println("\n--- SIMULACAO DE RESILIENCIA OFFLINE & IDEMPOTENCIA (ADR-004) ---");
        System.out.println("Cenario: doca em zona sem rede armazenou evento de devolucao e reenvia-o varias vezes.");

        String bikeId = "BIKE-OFFLINE-SIM";
        String simUserId = "USER-SIM-" + currentUser.userId();

        dockController.unlockBike("ST-01", "DOCK-01", bikeId, simUserId,
                currentUser.isSubscriber(), currentUser.paymentMethod(), LocalDateTime.now());
        System.out.println("1. Bicicleta " + bikeId + " desbloqueada (utilizador de simulacao).");

        String eventId = "OFFLINE-EVT-SIM-" + System.currentTimeMillis();
        LocalDateTime returnTime = LocalDateTime.now().plusMinutes(60);

        System.out.println("2. Primeiro envio do evento offline (" + eventId + "):");
        ReturnBikeResponseDTO r1 = dockController.returnBike(eventId, "ST-02", "DOCK-02",
                bikeId, returnTime, currentUser.isSubscriber(), currentUser.paymentMethod());
        System.out.println("   -> " + r1.message());

        System.out.println("3. Reenvio imediato do MESMO evento (retry de rede):");
        ReturnBikeResponseDTO r2 = dockController.returnBike(eventId, "ST-02", "DOCK-02",
                bikeId, returnTime, currentUser.isSubscriber(), currentUser.paymentMethod());
        System.out.println("   -> " + r2.message());

        int count = billingController.getUserInvoices(simUserId).size();
        System.out.printf("4. Faturas emitidas para utilizador de simulacao = %d (esperado: 1)%n", count);
        System.out.println(count == 1
                ? ">> SUCESSO: Idempotencia garantida! Sem cobranca duplicada."
                : ">> FALHA: Foi gerada fatura duplicada!");
    }

    private void runAutoDemo() {
        System.out.println("\n--- DEMONSTRACAO AUTOMATICA COMPLETA ---");
        listStations();

        String demoUserId = "USER-DEMO-AUTO";
        String bikeId = "BIKE-AUTO-DEMO";
        LocalDateTime start = LocalDateTime.now();

        System.out.println("\n1. Desbloquear bicicleta no Cais do Sodre:");
        UnlockBikeResponseDTO unl = dockController.unlockBike(
                "ST-01", "DOCK-01", bikeId, demoUserId, true, "MBWAY", start);
        System.out.println("   " + unl.message() + " | Viagem: " + unl.tripId());

        System.out.println("\n2. Estado da viagem ativa:");
        tripController.getActiveTrip(demoUserId).ifPresent(t ->
                System.out.println("   Viagem ativa: " + t.tripId() + " as " + t.startTime().format(TIME_FMT)));

        System.out.println("\n3. Devolver no Saldanha apos 55 min (45 min gratis + 1 bloco 0,50 EUR):");
        ReturnBikeResponseDTO ret = dockController.returnBike(
                "EVT-DEMO-001", "ST-02", "DOCK-03", bikeId,
                start.plusMinutes(55), true, "MBWAY");
        System.out.println("   " + ret.message());

        System.out.println("\n4. Fatura gerada e registada na Autoridade Tributaria:");
        billingController.getInvoiceByTrip(unl.tripId()).ifPresent(this::printInvoice);
    }

    private void printInvoice(InvoiceDTO inv) {
        System.out.println("+-------------------- RECIBO FISCAL (AT) --------------------+");
        System.out.printf("| Fatura ID:    %-44s |%n", inv.invoiceId());
        System.out.printf("| Utilizador:   %-44s |%n", inv.userId());
        System.out.printf("| Viagem ID:    %-44s |%n", inv.tripId());
        System.out.printf("| Valor Total:  %-44s |%n", String.format("%.2f EUR", inv.amountCents() / 100.0));
        System.out.printf("| IVA (23%%):    %-44s |%n", String.format("%.2f EUR", inv.taxCents() / 100.0));
        System.out.printf("| Pagamento:    %-44s |%n", inv.paymentMethod());
        System.out.printf("| Estado:       %-44s |%n", inv.status());
        System.out.printf("| Validacao AT: %-44s |%n", inv.atFiscalCode());
        System.out.println("| Retencao:     10 anos garantidos (ADR-006)                  |");
        System.out.println("+-------------------------------------------------------------+");
    }
}
