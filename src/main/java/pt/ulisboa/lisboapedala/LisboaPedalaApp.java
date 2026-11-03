package pt.ulisboa.lisboapedala;

import pt.ulisboa.lisboapedala.common.DatabaseManager;
import pt.ulisboa.lisboapedala.common.UserRepository;
import pt.ulisboa.lisboapedala.common.UserRepository.UserProfile;
import pt.ulisboa.lisboapedala.dock_access.DockAccessFacade;
import pt.ulisboa.lisboapedala.dock_access.ReturnBikeResponseDTO;
import pt.ulisboa.lisboapedala.dock_access.UnlockBikeResponseDTO;
import pt.ulisboa.lisboapedala.dock_access.presentation.DockController;
import pt.ulisboa.lisboapedala.history_reader.HistoryReaderFacade;
import pt.ulisboa.lisboapedala.history_reader.UserTripHistoryDTO;
import pt.ulisboa.lisboapedala.history_reader.presentation.HistoryController;
import pt.ulisboa.lisboapedala.maintenance_orders.MaintenanceOrderDTO;
import pt.ulisboa.lisboapedala.maintenance_orders.MaintenanceOrdersFacade;
import pt.ulisboa.lisboapedala.maintenance_orders.presentation.MaintenanceController;
import pt.ulisboa.lisboapedala.reporting_audit.ParishUsageReportDTO;
import pt.ulisboa.lisboapedala.reporting_audit.ReportingAuditFacade;
import pt.ulisboa.lisboapedala.reporting_audit.presentation.ReportController;
import pt.ulisboa.lisboapedala.sensor_ingestion.BikeHealthStatusDTO;
import pt.ulisboa.lisboapedala.sensor_ingestion.SensorIngestionFacade;
import pt.ulisboa.lisboapedala.sensor_ingestion.presentation.TelemetryController;
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
    private final HistoryController historyController;
    private final SensorIngestionFacade sensorFacade;
    private final TelemetryController telemetryController;
    private final MaintenanceOrdersFacade maintenanceFacade;
    private final MaintenanceController maintenanceController;
    private final ReportingAuditFacade reportingFacade;
    private final ReportController reportController;
    private final Scanner scanner;

    /** Utilizador autenticado na sessao actual. */
    private UserProfile currentUser;

    public LisboaPedalaApp() {
        // Persistencia em ficheiro SQLite — os dados sobrevivem ao fecho da aplicacao
        this.dbManager = DatabaseManager.createForFile(DB_FILE);
        this.userRepository = new UserRepository(dbManager);

        StationAvailabilityFacade stationFacade = new StationAvailabilityFacade(dbManager);
        TariffBillingFacade billingFacade = new TariffBillingFacade(dbManager);
        TripLifecycleFacade tripFacade = new TripLifecycleFacade(dbManager, billingFacade);
        DockAccessFacade dockFacade = new DockAccessFacade(dbManager, tripFacade, stationFacade);
        HistoryReaderFacade historyFacade = new HistoryReaderFacade(tripFacade, billingFacade);
        SensorIngestionFacade sensorFacade = new SensorIngestionFacade(dbManager);
        MaintenanceOrdersFacade maintenanceFacade = new MaintenanceOrdersFacade(dbManager, sensorFacade);
        ReportingAuditFacade reportingFacade = new ReportingAuditFacade(dbManager, tripFacade);

        this.stationController = new StationController(stationFacade);
        this.dockController = new DockController(dockFacade);
        this.tripController = new TripController(tripFacade);
        this.billingController = new BillingController(billingFacade);
        this.historyController = new HistoryController(historyFacade);
        this.sensorFacade = sensorFacade;
        this.telemetryController = new TelemetryController(sensorFacade);
        this.maintenanceFacade = maintenanceFacade;
        this.maintenanceController = new MaintenanceController(maintenanceFacade);
        this.reportingFacade = reportingFacade;
        this.reportController = new ReportController(reportingFacade);
        this.scanner = new Scanner(System.in);

        seedStationsIfNeeded(stationFacade);
        registerMunicipalStations(reportingFacade);
        seedDefaultAccountsIfNeeded();
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

    /**
     * Regista os metadados das estacoes no cadastro de relatórios municipais.
     */
    private void registerMunicipalStations(ReportingAuditFacade reportingFacade) {
        reportingFacade.registerStation("ST-01", "Cais do Sodre",     "Misericordia",      10, 6,  4);
        reportingFacade.registerStation("ST-02", "Saldanha",           "Avenidas Novas",    12, 3,  9);
        reportingFacade.registerStation("ST-03", "Rossio",             "Santa Maria Maior",  8, 4,  4);
        reportingFacade.registerStation("ST-04", "Belem",              "Belem",             15, 2, 13);
        reportingFacade.registerStation("ST-05", "Marques de Pombal",  "Santo Antonio",     10, 5,  5);
        reportingFacade.registerStation("ST-06", "Intendente",         "Arroios",           10, 3,  7);
    }

    /**
     * Inicializa conta de demonstração para Gestor Municipal se não existir.
     */
    private void seedDefaultAccountsIfNeeded() {
        if (!userRepository.usernameExists("gestor")) {
            userRepository.register("MGR-001", "gestor", "gestor", false, "MBWAY", "MANAGER");
        }
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
            if (currentUser.isTechnician()) {
                running = handleTechnicianMenu();
            } else if (currentUser.isManager()) {
                running = handleManagerMenu();
            } else {
                running = handleCyclistMenu();
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
        if (currentUser.isTechnician()) {
            System.out.println("\nBem-vindo(a), Técnico(a) " + currentUser.username() + "!  [Painel de Manutenção]");
        } else if (currentUser.isManager()) {
            System.out.println("\nBem-vindo(a), Gestor(a) " + currentUser.username() + "!  [Painel de Gestão Municipal]");
        } else {
            System.out.println("\nBem-vindo(a), " + currentUser.username() + "!"
                    + (currentUser.isSubscriber() ? "  [Assinante Passe Anual]" : "  [Utilizador Ocasional]"));
        }
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

        System.out.println("\nTipo de conta:");
        System.out.println("  [1] Ciclista (Cliente / Assinante)");
        System.out.println("  [2] Técnico de Manutenção (US-2)");
        System.out.println("  [3] Gestor Municipal (US-4 / Relatórios)");
        System.out.print("Escolhe o tipo (default: 1): ");
        String roleInput = scanner.nextLine().trim();
        String userType = switch (roleInput) {
            case "2" -> "TECHNICIAN";
            case "3" -> "MANAGER";
            default  -> "CYCLIST";
        };

        String prefix = switch (userType) {
            case "TECHNICIAN" -> "TECH-";
            case "MANAGER"    -> "MGR-";
            default           -> "USER-";
        };
        String userId = prefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        boolean ok = userRepository.register(userId, username, password, isSubscriber, payMethod, userType);
        if (ok) {
            String roleName = switch (userType) {
                case "TECHNICIAN" -> "Técnico";
                case "MANAGER"    -> "Gestor";
                default           -> "Ciclista";
            };
            System.out.println("Conta criada com sucesso (" + roleName + ")! Já podes iniciar sessão.");
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

    private boolean handleCyclistMenu() {
        printMainMenu();
        String option = scanner.nextLine().trim();
        switch (option) {
            case "1" -> listStations();
            case "2" -> handleUnlockBike();
            case "3" -> handleViewActiveTrip();
            case "4" -> handleReturnBike();
            case "5" -> handleViewUserHistory();
            case "6" -> handleViewInvoices();
            case "7" -> handleSimulateOfflineReplay();
            case "8" -> runAutoDemo();
            case "0" -> {
                System.out.println("\nSessao terminada. Ate a proxima, " + currentUser.username() + "!");
                return false;
            }
            default -> System.out.println("Opcao invalida. Por favor escolhe uma das opcoes do menu.");
        }
        return true;
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
        System.out.println(" [5] Consultar historico unificado de viagens e faturas (history_reader)");
        System.out.println(" [6] Consultar as minhas faturas");
        System.out.println(" [7] Simular resiliencia offline e idempotencia (ADR-004)");
        System.out.println(" [8] Executar demonstracao automatica completa");
        System.out.println(" [0] Terminar sessao");
        System.out.print("Escolhe uma opcao: ");
    }

    // -------------------------------------------------------------------------
    // Opcoes do menu Ciclista
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

    private void handleViewUserHistory() {
        System.out.println("\n--- HISTÓRICO UNIFICADO DE VIAGENS E FATURAS (history_reader - US-8) ---");
        List<UserTripHistoryDTO> history = historyController.getUserHistory(currentUser.userId());
        if (history.isEmpty()) {
            System.out.println("Ainda não tens viagens registadas no teu histórico.");
            return;
        }

        System.out.printf("Total de viagens registadas para %s: %d%n", currentUser.username(), history.size());
        int index = 1;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (UserTripHistoryDTO entry : history) {
            TripDTO trip = entry.trip();
            InvoiceDTO inv = entry.invoice();

            System.out.println("\n+-------------------------------------------------------------------------------+");
            System.out.printf("| [%d] VIAGEM: %-32s  Estado: %-15s |%n", index++, trip.tripId(), trip.status());
            System.out.printf("|     Bicicleta: %-16s | Levantada em: %-25s |%n", trip.bikeId(), trip.startStationId());
            System.out.printf("|     Início:    %-16s | Fim:          %-25s |%n",
                    trip.startTime() != null ? trip.startTime().format(dtf) : "N/A",
                    trip.endTime() != null ? trip.endTime().format(dtf) : "(Em curso)");
            System.out.printf("|     Devolvida em: %-13s | Duração:      %-25s |%n",
                    trip.endStationId() != null ? trip.endStationId() : "N/A",
                    trip.durationMinutes() != null ? trip.durationMinutes() + " minutos" : "N/A");

            if (entry.hasInvoice()) {
                System.out.println("| . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . |");
                System.out.printf("|     FATURA FISCAL: %-18s | Valor: %.2f EUR (IVA: %.2f EUR)       |%n",
                        inv.invoiceId(), inv.amountCents() / 100.0, inv.taxCents() / 100.0);
                System.out.printf("|     Método:        %-18s | Código AT: %-23s |%n",
                        inv.paymentMethod(), inv.atFiscalCode());
                System.out.printf("|     Emitida em:    %-18s | Estado Fatura: %-19s |%n",
                        inv.issuedAt(), inv.status());
            } else if ("IN_PROGRESS".equals(trip.status())) {
                System.out.println("|     [Viagem em curso - fatura será emitida automaticamente após devolução]     |");
            } else {
                System.out.println("|     [Sem fatura associada]                                                    |");
            }
            System.out.println("+-------------------------------------------------------------------------------+");
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

    // -------------------------------------------------------------------------
    // Painel e Operações do Gestor Municipal (US-4)
    // -------------------------------------------------------------------------

    private boolean handleManagerMenu() {
        System.out.println("\n================================================================================");
        System.out.println("             PAINEL DE GESTÃO MUNICIPAL - GESTOR (US-4 / AUDITORIA)             ");
        System.out.println(" Sessão: " + currentUser.username() + "  |  Gestor Municipal");
        System.out.println("================================================================================");
        System.out.println(" [1] Ver relatório de utilização por freguesia");
        System.out.println(" [0] Terminar sessão");
        System.out.print("Escolhe uma opção: ");

        String option = scanner.nextLine().trim();
        switch (option) {
            case "1" -> handleViewParishUsageReport();
            case "0" -> {
                System.out.println("\nSessão de gestor terminada. Até à próxima, " + currentUser.username() + "!");
                return false;
            }
            default -> System.out.println("Opção inválida. Por favor escolhe uma das opções do menu.");
        }
        return true;
    }

    private void handleViewParishUsageReport() {
        System.out.println("\n--- RELATÓRIO DE UTILIZAÇÃO POR FREGUESIA (US-4 - reporting_audit) ---");
        ParishUsageReportDTO report = reportController.getParishUsageReport();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(java.time.ZoneId.systemDefault());
        String formattedDate = report.generatedAt() != null ? dtf.format(report.generatedAt()) : "N/A";

        System.out.println("========================================================================================================");
        System.out.println("                         RELATÓRIO MUNICIPAL DE UTILIZAÇÃO POR FREGUESIA                                ");
        System.out.printf(" Gerado em: %-19s | Total Viagens: %-5d | Duração: %-8s | CO2 Poupado: %-10s%n",
                formattedDate,
                report.totalTrips(),
                report.totalDurationMinutes() + " min",
                String.format("%.2f kg", report.totalCo2SavedKg()));
        System.out.println("========================================================================================================");

        if (report.parishStats() == null || report.parishStats().isEmpty()) {
            System.out.println("Ainda não existem viagens concluídas registadas no sistema para agregar por freguesia.");
            System.out.println("--------------------------------------------------------------------------------------------------------");
            return;
        }

        System.out.println("+----------------------+-----------------+---------------+---------------------+------------------------+");
        System.out.printf("| %-20s | %-15s | %-13s | %-19s | %-22s |%n",
                "Freguesia", "Viagens Início", "Viagens Fim", "Duração Total (min)", "CO2 Poupado Estimado");
        System.out.println("+----------------------+-----------------+---------------+---------------------+------------------------+");

        for (ParishUsageReportDTO.ParishStatRecord stat : report.parishStats()) {
            System.out.printf("| %-20s | %15d | %13d | %19d | %20.2f kg |%n",
                    stat.parish(),
                    stat.tripsStarted(),
                    stat.tripsEnded(),
                    stat.totalDurationMinutes(),
                    stat.estimatedCo2SavedKg());
        }
        System.out.println("+----------------------+-----------------+---------------+---------------------+------------------------+");
        System.out.println("Nota: Agregação anónima sem PII e métricas calculadas em conformidade com ADR-006 (RGPD).");
    }

    // -------------------------------------------------------------------------
    // Painel e Operações do Técnico de Manutenção (US-2)
    // -------------------------------------------------------------------------

    private boolean handleTechnicianMenu() {
        System.out.println("\n================================================================================");
        System.out.println("                 PAINEL DE CAMPO - TÉCNICO DE MANUTENÇÃO (US-2)                ");
        System.out.println(" Sessão: " + currentUser.username() + "  |  Técnico de Manutenção");
        System.out.println("================================================================================");
        System.out.println(" [1] Ver ordens de manutenção ativas (Pool geral de avarias)");
        System.out.println(" [2] Concluir ordem de manutenção (marcar bicicleta como reparada)");
        System.out.println(" [3] Simular bicicleta a reportar avaria (Telemetria IoT - Avaria A)");
        System.out.println(" [4] Consultar telemetria e estado de saúde de uma bicicleta");
        System.out.println(" [5] Ver disponibilidade das estações");
        System.out.println(" [0] Terminar sessão");
        System.out.print("Escolhe uma opção: ");

        String option = scanner.nextLine().trim();
        switch (option) {
            case "1" -> handleViewActiveMaintenanceOrders();
            case "2" -> handleCompleteMaintenanceOrder();
            case "3" -> handleSimulateBikeMalfunction();
            case "4" -> handleViewBikeHealth();
            case "5" -> listStations();
            case "0" -> {
                System.out.println("\nSessão de técnico terminada. Até à próxima, " + currentUser.username() + "!");
                return false;
            }
            default -> System.out.println("Opção inválida. Por favor escolhe uma das opções do menu.");
        }
        return true;
    }

    private void handleViewActiveMaintenanceOrders() {
        System.out.println("\n--- ORDENS DE MANUTENÇÃO ATIVAS NO SISTEMA (VISÍVEL A TODOS OS TÉCNICOS) ---");
        List<MaintenanceOrderDTO> active = maintenanceController.getActiveOrders();
        if (active.isEmpty()) {
            System.out.println("Excelente! Não existem avarias pendentes de reparação no momento.");
            return;
        }
        System.out.printf("Total de avarias ativas que requerem intervenção: %d%n", active.size());
        printOrdersTable(active);
    }

    private void handleCompleteMaintenanceOrder() {
        System.out.println("\n--- CONCLUIR ORDEM DE MANUTENÇÃO (REPARAÇÃO CONCLUÍDA) ---");
        List<MaintenanceOrderDTO> active = maintenanceController.getActiveOrders();
        if (active.isEmpty()) {
            System.out.println("Não existem ordens de manutenção ativas para concluir.");
            return;
        }
        printOrdersTable(active);

        System.out.print("\nIntroduz o ID da ordem resolvida (ex: MO-...): ");
        String orderId = scanner.nextLine().trim().toUpperCase();
        if (orderId.isBlank()) {
            System.out.println("ID inválido.");
            return;
        }

        try {
            MaintenanceOrderDTO resolved = maintenanceController.completeOrder(orderId);
            System.out.println("SUCESSO: Ordem " + resolved.orderId() + " marcada como RESOLVIDA!");
            System.out.println("  Bicicleta: " + resolved.bikeId() + " reparada e pronta a circular.");
            System.out.println("  Data/Hora de resolução: " + resolved.resolvedAt());
            if (resolved.anomalyId() != null) {
                System.out.println("  A anomalia IoT (" + resolved.anomalyId() + ") foi marcada como limpa e resolvida em sensor_ingestion.");
            }
        } catch (Exception e) {
            System.out.println("FALHA: " + e.getMessage());
        }
    }

    private void handleSimulateBikeMalfunction() {
        System.out.println("\n--- SIMULAÇÃO: BICICLETA REPORTA AVARIA (AVARIA A - TELEMETRIA IoT) ---");
        System.out.println("Como a bicicleta reporta a avaria automaticamente?");
        System.out.println("  [1] Telemetria IoT: Bateria Crítica (8.5%) em andamento -> Deteção automática por sensor_ingestion");
        System.out.println("  [2] Telemetria IoT: Falha grave de Travões (BRAKE_FAULT) -> Deteção automática por sensor_ingestion");
        System.out.print("Escolhe o tipo de telemetria IoT (1-2): ");
        String simOpt = scanner.nextLine().trim();

        System.out.print("Identificador da bicicleta (ex: BIKE-E-100, default: BIKE-E-88): ");
        String bikeId = scanner.nextLine().trim().toUpperCase();
        if (bikeId.isBlank()) {
            bikeId = "BIKE-E-88";
        }

        switch (simOpt) {
            case "1" -> {
                System.out.println("\n>> 1. Bicicleta " + bikeId + " transmite telemetria com Bateria = 8.5%...");
                sensorFacade.ingestTelemetry(new pt.ulisboa.lisboapedala.sensor_ingestion.TelemetryDTO(
                        bikeId, 8.5, 38.710, -9.140, 15.0, "OK", java.time.Instant.now()
                ));
                System.out.println(">> 2. sensor_ingestion processa pacote e deteta anomalia crítica: CRITICAL_BATTERY!");
                int created = maintenanceController.syncSensorAnomalies();
                System.out.printf("🚨 3. ORDEM DE MANUTENÇÃO GERADA AUTOMATICAMENTE! (%d nova ordem)%n", created);
                System.out.println("   Alerta emitido: todos os técnicos têm agora acesso a esta ordem na pool de ordens ativas.");
                System.out.println(">> Consulta as avarias ativas na opção [1] do menu.");
            }
            case "2" -> {
                System.out.println("\n>> 1. Bicicleta " + bikeId + " transmite código de falha nos travões (BRAKE_FAULT)...");
                sensorFacade.ingestTelemetry(new pt.ulisboa.lisboapedala.sensor_ingestion.TelemetryDTO(
                        bikeId, 75.0, 38.720, -9.150, 12.0, "BRAKE_FAULT", java.time.Instant.now()
                ));
                System.out.println(">> 2. sensor_ingestion processa pacote e deteta anomalia crítica: BRAKE_FAILURE!");
                int created = maintenanceController.syncSensorAnomalies();
                System.out.printf("🚨 3. ORDEM DE MANUTENÇÃO GERADA AUTOMATICAMENTE! (%d nova ordem)%n", created);
                System.out.println("   Alerta emitido: todos os técnicos têm agora acesso a esta ordem na pool de ordens ativas.");
                System.out.println(">> Consulta as avarias ativas na opção [1] do menu.");
            }
            default -> System.out.println("Opção de telemetria inválida.");
        }
    }

    private void handleViewBikeHealth() {
        System.out.println("\n--- CONSULTAR ESTADO DE SAÚDE DE BICICLETA (sensor_ingestion) ---");
        System.out.print("Identificador da bicicleta: ");
        String bikeId = scanner.nextLine().trim().toUpperCase();
        if (bikeId.isBlank()) {
            System.out.println("ID inválido.");
            return;
        }

        BikeHealthStatusDTO health = sensorFacade.getBikeHealthStatus(bikeId);
        int alertsCount = health.activeAlerts() != null ? health.activeAlerts().size() : 0;
        boolean suitable = "OPERATIONAL".equalsIgnoreCase(health.healthStatus());

        System.out.println("+---------------- ESTADO OPERACIONAL DA BICICLETA ----------------+");
        System.out.printf("| Bicicleta ID:        %-42s |%n", health.bikeId());
        System.out.printf("| Nível Bateria:       %-42s |%n", health.batteryLevel() >= 0 ? String.format("%.1f %%", health.batteryLevel()) : "Sem telemetria");
        System.out.printf("| Estado de Saúde:     %-42s |%n", health.healthStatus());
        System.out.printf("| Estado dos Travões:  %-42s |%n", health.brakeStatus());
        System.out.printf("| Anomalias Ativas:    %-42d |%n", alertsCount);
        System.out.printf("| Aptidão p/ Viagem:   %-42s |%n", suitable ? "SIM (Segura)" : "NÃO (Requer Manutenção)");
        System.out.println("+-----------------------------------------------------------------+");
    }

    private void printOrdersTable(List<MaintenanceOrderDTO> orders) {
        System.out.println("+---------------------------------------------------------------------------------------------------------------+");
        System.out.printf("| %-22s | %-14s | %-12s | %-45s |%n",
                "Ordem ID", "Bicicleta", "Estado", "Descrição");
        System.out.println("+---------------------------------------------------------------------------------------------------------------+");
        for (MaintenanceOrderDTO o : orders) {
            String desc = o.description().length() > 45 ? o.description().substring(0, 42) + "..." : o.description();
            System.out.printf("| %-22s | %-14s | %-12s | %-45s |%n",
                    o.orderId(), o.bikeId(), o.status(), desc);
        }
        System.out.println("+---------------------------------------------------------------------------------------------------------------+");
    }
}
