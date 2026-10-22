Apresento o Plano Estruturado em 3 Fases para a fundação e implementação do Monólito Modular em Java 21 e SQLite do sistema Lisboa Pedala, estritamente alinhado com o `ARCHITECTURE.md`, os documentos em `docs/` e o teste arquitetural ArchUnit existente em `ModularMonolithArchitectureTest.java`.

---

## Pré-requisito / Fundação Base (Preparação do Ambiente)
Antes do arranque das regras de negócio, será garantida a infraestrutura do projeto:

* **Configuração Maven (`pom.xml`):**
  * Configurar o compilador para Java 21.
  * Dependências: `sqlite-jdbc` (persistência), `junit-jupiter` 5.10+, `archunit-junit5` 1.3+ e suporte de assertions/testes.
  * Configuração do Maven Surefire para executar tanto os testes em `tests/architecture` como os testes unitários/integração em `src/test/java`.
* **Pacote Raiz:** `pt.ulisboa.lisboapedala` e módulo transversal `common` (ex: gestão de conexões SQLite em memória/ficheiro e DTOs base).

---

## Fase 1: Implementação da User Story 1 (Obrigatória)
> *"Como assinante, desbloqueio e devolvo uma bicicleta e a viagem fica registada e cobrada."*

* **Módulos envolvidos:** `dock_access`, `trip_lifecycle`, `tariff_billing` (com suporte de `station_availability` para contadores de docas/bicicletas).

### 1. Estrutura de Pacotes e Fachadas Públicas
* **`pt.ulisboa.lisboapedala.dock_access`**:
  * Fachada Pública: `DockAccessFacade` (`unlockBike`, `returnBike`, `syncOfflineEvents`).
  * Interno: `business` (validação de token/QR, coordenação do comando físico, buffer de 24h para resiliência offline) e `persistence` (`DockEventRepository`).
* **`pt.ulisboa.lisboapedala.trip_lifecycle`**:
  * Fachada Pública: `TripLifecycleFacade` (`startTrip`, `completeTrip`, `getActiveTrip`, `getTripById`).
  * Interno: `business` (`TripService`, `Trip` com estados `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, cálculo de duração) e `persistence` (`TripRepository`).
* **`pt.ulisboa.lisboapedala.tariff_billing`**:
  * Fachada Pública: `TariffBillingFacade` (`processTripBilling`, `getInvoiceByTripId`, `getUserInvoices`).
  * Interno: `business` (`TariffCalculator` com 45 min grátis para assinantes + blocos 15 min, `BillingService`, simuladores de adaptadores `PaymentGatewayAdapter` e `TaxAuthorityAdapter`) e `persistence` (`InvoiceRepository`).
* **`pt.ulisboa.lisboapedala.station_availability`**:
  * Fachada Pública: `StationAvailabilityFacade` (`updateAvailability`, `getStationOccupancy`).

### 2. Esquemas SQLite Segregados (`schema.sql` por módulo)
* `dock_access/persistence/schema.sql`: Tabela `dock_access_events` (`event_id`, `station_id`, `dock_id`, `bike_id`, `action_type`, `occurred_at`, `synced`).
* `trip_lifecycle/persistence/schema.sql`: Tabela `trip_lifecycle_trips` (`id`, `user_id`, `bike_id`, `start_station_id`, `end_station_id`, `start_time`, `end_time`, `duration_minutes`, `status`, `event_id`).
* `tariff_billing/persistence/schema.sql`: Tabela `tariff_billing_invoices` (`id`, `trip_id`, `user_id`, `amount_cents`, `tax_cents`, `issued_at`, `status`, `at_fiscal_code`).
* `station_availability/persistence/schema.sql`: Tabela `station_availability_stations` (`station_id`, `name`, `total_docks`, `available_bikes`, `free_docks`).

### 3. Regras de Negócio e Robustez (ADR-004 e ADR-006)
* **Desbloqueio:** Requer validação online; viagem é criada no estado `IN_PROGRESS` antes de autorizar a doca.
* **Devolução Offline & Idempotência:** Devolução registada localmente na doca; quando sincronizada via `dock_access`, `trip_lifecycle` e `tariff_billing` processam o `event_id` garantindo que envios duplicados não recriam viagens nem originam cobranças duplicadas.
* **Simulação Externa:** Gateway de pagamentos simulado e comunicação mock com a Autor

### 4. Testes da Fase 1
* **Unitários:** Cálculo de tarifas (assinante < 45m = 0€; assinante > 45m com blocos adicionais; utilizador ocasional); transições de ciclo de vida da viagem.
* **Integração:** Fluxo completo transacional ponta a ponta (desbloquear → devolver → faturar → cobrar).
* **Resiliência e Idempotência:** Simulação de reenvio do mesmo evento offline e confirmação de não duplicação de faturas nem viagens.
* **Arquiteturais:** Execução do `ModularMonolithArchitectureTest.java` para validar ausência de ciclos e encapsulamento dos módulos da Fase 1.

---

## Fase 2: Implementação da User Story 2
> *"Como técnico, recebo uma ordem de manutenção quando uma bicicleta reporta avaria."*

* **Módulos envolvidos:** `sensor_ingestion`, `maintenance_orders`.

### 1. Estrutura de Pacotes e Fachadas Públicas
* **`pt.ulisboa.lisboapedala.sensor_ingestion`**:
  * Fachada Pública: `SensorIngestionFacade` (`ingestTelemetry`, `getLatestTelemetry`, `getUnresolvedMalfunctions`).
  * Interno: `business` (`TelemetryParser`, deteção de bateria crítica < 15%, travões avariados, temperatura anómala) e `persistence` (`TelemetryLogRepository`).
* **`pt.ulisboa.lisboapedala.maintenance_orders`**:
  * Fachada Pública: `MaintenanceOrdersFacade` (`createOrderFromAlert`, `getPendingOrders`, `assignOrderToTechnician`, `completeOrder`).
  * Interno: `business` (`TicketService`, regras de priorização de ordens técnicas de reparação) e `persistence` (`MaintenanceOrderRepository`).

### 2. Esquemas SQLite Segregados
* `sensor_ingestion/persistence/schema.sql`: Tabela `sensor_ingestion_telemetry` (`id`, `bike_id`, `battery_level`, `brakes_ok`, `latitude`, `longitude`, `timestamp`, `has_malfunction`).
* `maintenance_orders/persistence/schema.sql`: Tabela `maintenance_orders_tickets` (`id`, `bike_id`, `issue_description`, `priority`, `technician_id`, `status`, `created_at`, `resolved_at`).

### 3. Regras de Negócio e Acoplamento
* `sensor_ingestion` é um módulo folha (não depende de `maintenance_orders`).
* `maintenance_orders` consome dados de anomalias exclusivamente através da `SensorIngestionFacade` (respeitando a regra `maintenance_orders -> sensor_ingestion`).
* Técnico pode listar ordens pendentes, assumir uma ordem e marcá-la como resolvida.

### 4. Testes da Fase 2
* **Unitários:** Deteção de anomalias a partir de telemetria bruta; transições de estado dos tickets de manutenção.
* **Integração:** Ingestão de telemetria defeituosa que resulta na abertura e visualização de ordem de manutenção para a equipa técnica.
* **Arquiteturais:** Verificação pelo ArchUnit das regras relativas a `sensor_ingestion` e `maintenance_orders`.

---

## Fase 3: Implementação da User Story 4
> *"Como gestor municipal, obtenho o relatório de utilização por freguesia."*

* **Módulos envolvidos:** `trip_lifecycle`, `reporting_audit` (e `station_availability`).

### 1. Estrutura de Pacotes e Fachadas Públicas
* **`pt.ulisboa.lisboapedala.reporting_audit`**:
  * Fachada Pública: `ReportingAuditFacade` (`generateMonthlyReport`, `getParishUsageReport`, `getAuditReports`).
  * Interno: `business` (`ReportGeneratorService`, `CarbonSavingsCalculator` para estimativa de CO₂ evitado face ao transporte motorizado, `ParishUsageAggregator`) e `persistence` (`ReportRepository`).
* **Extensões na fachada de `trip_lifecycle`**:
  * Método de leitura `getCompletedTripsInPeriod(start, end)` (retornando DTOs públicos de viagem, sem partilhar entidades internas).
* **Extensões na fachada de `station_availability`**:
  * Método de leitura `getStationsWithParish()` (mapeamento estação → freguesia).

### 2. Esquemas SQLite Segregados
* `reporting_audit/persistence/schema.sql`: Tabela `reporting_audit_monthly_reports` (`id`, `year_month`, `total_trips`, `co2_saved_kg`, `generated_at`, `details_json`).

### 3. Regras de Negócio e Retenção
* Sem joins SQL entre módulos: `reporting_audit` consulta as viagens via `TripLifecycleFacade`, agrega por freguesia na camada aplicacional e persiste o relatório para auditoria municipal.

### 4. Testes da Fase 3
* **Unitários:** Fórmulas de estimativa de CO₂ evitado e lógica de agrupamento por freguesia.
* **Integração:** Geração do relatório mensal a partir de dados reais de viagens e estações da base de dados.
* **Arquiteturais:** Execução completa da suite ArchUnit garantindo 100% de conformidade com todos os módulos (`Allowed-to-Use`, ausência de ciclos e encapsulamento estrito).

---

Por favor, confirma se estás de acordo com este plano ou se pretendes algum ajuste, para darmos início à Fase 1.