# Prompts 

## Prompt 1
Por favor, com base na documentação presente em ARCHITECTURE.md e na pasta docs/ (01-caracteristicas.md, 02-componentes.md e 03-particionamento-e-vistas.md), implementa a fundação do monólito modular em Java 21 para o sistema Lisboa Pedala.

Implementa as seguintes User Stories:
• US-1 (Obrigatória): Como assinante, desbloqueio e devolvo uma bicicleta e a viagem fica registada e cobrada. Envolve os módulos dock_access, trip_lifecycle e tariff_billing. 
• US-2: Como técnico, recebo uma ordem de manutenção quando uma bicicleta reporta avaria. Envolve os módulos sensor_ingestion e maintenance_orders. 
• US-4: Como gestor municipal, obtenho o relatório de utilização por freguesia, envolve os módulos trip_lifecycle e reporting_audit.

Tarefas a executar:
• Criar a estrutura de pacotes base pt.ulisboa.lisboapedala respeitando os módulos de domínio definidos nos documentos.
• Implementar as Fachadas públicas (*Facade.java) de cada módulo envolvido, garantindo que nenhum componente interno é acedido diretamente.
• Configurar os esquemas de base de dados SQLite (schema.sql) segregados por módulo. 
• Escrever os testes unitários e de integração que validem o fluxo transacional da US-1 (incluindo a idempotência e o tratamento de eventos).
• Implementar os testes arquiteturais (ex: ArchUnit em tests/architecture/) para validar o isolamento modular.
• Os pagamentos são simulados.

Cria um plano de criação por 3 fases:
• Fase 1: Implementação da User Story 1
• Fase 2: Implementação da User Story 2
• Fase 3: Implementação da User Story 4

Primeiro mostra me o plano de cada fase e só depois de eu aprovar implementas um de cada de vez

## Prompt 2
[Contexto fornecido: ARCHITECTURE.md, adr/ADR-003.md, adr/ADR-004.md, adr/ADR-005.md, docs/03-particionamento-e-vistas.md]

Por favor, implementa exclusivamente o módulo de domínio `dock_access` em Java 21 para o sistema Lisboa Pedala, suportando a validação de desbloqueio, o envio de comandos físicos às docas e a gestão do buffer de sincronização offline para a US-1.

Deves respeitar rigorosamente as diretrizes arquiteturais definidas em ARCHITECTURE.md e nos ADRs referidos, bem como a seguinte estrutura:

1. Estrutura de Pacotes e Componentes Internos (em pt.ulisboa.lisboapedala.dock_access):
   - Fachada Pública (DockAccessFacade.java): Métodos como unlockBike(), returnBike(), syncOfflineEvents().
   - Camada de Apresentação (presentation/DockController.java): Endpoints REST para pedidos da app móvel nas estações.
   - Camada de Lógica de Negócio (business/): Validação de QR/token, UnlockingService, DockingService e BufferService (suporte a eventos offline até 24h conforme ADR-004).
   - Camada de Persistência (persistence/DockEventRepository.java): Tabela privada num schema.sql exclusivo do módulo.

2. Regras Estritas de Dependência e Allowed-to-Use (ADR-005):
   - Permitido: dock_access apenas pode depender de trip_lifecycle e station_availability.
   - Proibido: dependências de tariff_billing, sensor_ingestion ou outros módulos de domínio.
   - Isolamento: Proibido aceder a pacotes internos (.business/.persistence) de outros módulos ou efetuar queries SQL cruzadas.

Valida a implementação garantindo que o código passa sem erros no teste arquitetural ArchUnit (ModularMonolithArchitectureTest).

## Prompt 3
[Contexto fornecido: ARCHITECTURE.md, adr/ADR-002.md, adr/ADR-004.md, adr/ADR-005.md, docs/03-particionamento-e-vistas.md]

Por favor, implementa exclusivamente o módulo de domínio `trip_lifecycle` em Java 21 para o sistema Lisboa Pedala, gerindo o ciclo de vida integral das viagens (início, duração, fecho e idempotência) para a US-1.

Deves respeitar rigorosamente as diretrizes arquiteturais definidas em ARCHITECTURE.md e nos ADRs referidos, bem como a seguinte estrutura:

1. Estrutura de Pacotes e Componentes Internos (em pt.ulisboa.lisboapedala.trip_lifecycle):
   - Fachada Pública (TripLifecycleFacade.java): Métodos como startTrip(), completeTrip(), getActiveTrip(), getTripHistory(), getTripsByPeriod().
   - Camada de Apresentação (presentation/TripController.java): Endpoints REST para consultas da app.
   - Camada de Lógica de Negócio (business/): TripService.java, entidade Trip.java (com estados IN_PROGRESS, COMPLETED, CANCELLED) e garantia de idempotência para o reenvio de eventos offline (ADR-004).
   - Camada de Persistência (persistence/TripRepository.java): Tabela privada num schema.sql exclusivo do módulo.

2. Regras Estritas de Dependência e Allowed-to-Use (ADR-005):
   - Permitido: trip_lifecycle apenas pode depender de tariff_billing e station_availability.
   - Proibido: dependências de dock_access, sensor_ingestion ou outros módulos.
   - Isolamento: Proibido aceder a pacotes internos de outros módulos ou realizar queries SQL cruzadas.

Valida a implementação garantindo que o código passa sem erros no teste arquitetural ArchUnit (ModularMonolithArchitectureTest).

## Prompt 4
[Contexto fornecido: ARCHITECTURE.md, adr/ADR-005.md, adr/ADR-006.md, docs/03-particionamento-e-vistas.md]

Por favor, implementa exclusivamente o módulo de domínio `tariff_billing` em Java 21 para o sistema Lisboa Pedala, atuando como módulo folha para cálculo de tarifas (45 min grátis para passes anuais e blocos de 15 min), pagamentos e faturação eletrônica com comunicação à AT para a US-1.

Deves respeitar rigorosamente as diretrizes arquiteturais definidas em ARCHITECTURE.md e nos ADRs referidos, bem como a seguinte estrutura:

1. Estrutura de Pacotes e Componentes Internos (em pt.ulisboa.lisboapedala.tariff_billing):
   - Fachada Pública (TariffBillingFacade.java): Métodos como processTripBilling(), getInvoiceByTripId(), getUserInvoices().
   - Camada de Apresentação (presentation/BillingController.java): Endpoints REST para faturas.
   - Camada de Lógica de Negócio (business/): TariffCalculator.java, BillingService.java, Invoice.java e adaptadores simulados (PaymentGatewayAdapter.java, TaxAuthorityAdapter.java).
   - Camada de Persistência (persistence/InvoiceRepository.java): Tabela privada com retenção fiscal de 10 anos num schema.sql exclusivo do módulo (ADR-006).

2. Regras Estritas de Dependência e Allowed-to-Use (ADR-005):
   - Permitido: Nenhum outro módulo de domínio (módulo folha).
   - Proibido: Depender de qualquer outro módulo de domínio.
   - Isolamento: Proibido aceder a pacotes internos de outros módulos ou partilhar tabelas de base de dados.

Valida a implementação garantindo que o código passa sem erros no teste arquitetural ArchUnit (ModularMonolithArchitectureTest).


### Prompt 6
[Contexto fornecido: ARCHITECTURE.md, adr/ADR-002.md, adr/ADR-005.md, adr/ADR-006.md, docs/03-particionamento-e-vistas.md]

Por favor, implementa exclusivamente o módulo de domínio `history_reader` em Java 21 para o sistema Lisboa Pedala, atuando como componente de consulta para o cliente aceder ao seu histórico unificado de viagens e respetivas faturas, enquadrado no suporte à US-1 e US-8.

Deves respeitar rigorosamente as diretrizes arquiteturais definidas em ARCHITECTURE.md e nos ADRs referidos (em especial o papel exclusivo de leitura sem propriedade de escrita sobre viagens ou faturas), bem como a seguinte estrutura:

1. Estrutura de Pacotes e Componentes Internos (em pt.ulisboa.lisboapedala.history_reader):
   - Fachada Pública (HistoryReaderFacade.java): Métodos como getUserCompleteHistory() e getTripReceiptDetails().
   - Camada de Apresentação (presentation/HistoryController.java): Endpoints REST para a app móvel consultar o histórico do utilizador.
   - Camada de Lógica de Negócio (business/): HistoryAggregationService.java (consulta as fachadas de trip_lifecycle e tariff_billing e faz o "merge" dos dados sem possuir persistência própria de domínio, conforme ADR-002) e UserTripHistoryDTO.java.
   - Camada de Persistência / Consulta: Acesso exclusivamente via leitura através das APIs públicas dos respetivos módulos de domínio, sem assumir a propriedade ou alteração de dados de viagens/faturas.

2. Regras Estritas de Dependência e Allowed-to-Use (ADR-005 e ARCHITECTURE.md):
   - Permitido: `history_reader` apenas pode utilizar os módulos `trip_lifecycle` e `tariff_billing` em modo de consulta de leitura.
   - Proibido: Efetuar alterações de estado, comandos de escrita ou queries SQL diretas a tabelas de outros módulos.
   - Isolamento: Proibido aceder a pacotes internos (.business/.persistence) de outros módulos.

Valida a implementação garantindo que o código passa sem erros no teste arquitetural ArchUnit (ModularMonolithArchitectureTest).

## Prompt 7

[Contexto fornecido: ARCHITECTURE.md, docs/03-particionamento-e-vistas.md]
Vamos dar o inicio a implementação da USER STORY 2, começando pelo componente sensor_ingestion

Por favor, implementa exclusivamente o módulo de domínio `sensor_ingestion` em Java 21 para o sistema Lisboa Pedala, suportando a ingestão de telemetria das bicicletas elétricas, processamento de sinais e deteção de anomalias/avarias.

Deves respeitar rigorosamente as seguintes diretrizes arquiteturais extraídas de `docs/03-particionamento-e-vistas.md`:

1. Estrutura de Pacotes e Componentes Internos (em `pt.ulisboa.lisboapedala.sensor_ingestion`):
   - **Fachada Pública (`SensorIngestionFacade.java`)**: Único ponto de entrada para outros módulos consultarem dados de telemetria e anomalias em tempo real (ex: `getLatestTelemetry()`, `getUnresolvedMalfunctions()`).
   - **Camada de Apresentação / Ingestão (`presentation/TelemetryController.java`)**: Endpoint REST / Webhook para receber os pings periódicos das bicicletas elétricas em movimento.
   - **Camada de Lógica de Negócio (`business/`)**:
     - `TelemetryParserService.java`: Descodifica o pacote de dados bruto (GPS, nível de bateria, velocidade, estado dos travões).
     - `LostBikeDetectorService.java`: Deteta falhas prolongadas de sinal (ex: > 2h sem ping fora da doca).
     - `MalfunctionService.java`: Avalia componentes e identifica avarias técnicas (bateria crítica < 15%, travões avariados).
     - `MalfunctionEmitter.java`: Dispara alertas para o módulo de manutenção de forma desacoplada.
     - `SignalDispatcherService.java`: Encaminha os dados processados para os módulos autorizados.
   - **Camada de Persistência (`persistence/TelemetryLogRepository.java`)**: Gere a tabela privada num ficheiro `schema.sql` exclusivo do módulo para guardar o histórico de telemetria.

2. Regras Estritas de Dependência e Allowed-to-Use:
   - **Permitido:** `sensor_ingestion` apenas pode depender do módulo `station_availability` (conforme a matriz Allowed-to-Use da secção 2.4).
   - **Proibido:** É estritamente proibido depender de `trip_lifecycle`, `tariff_billing` ou de qualquer outro módulo de domínio não listado.
   - **Encapsulamento:** Nenhuma classe deste módulo pode aceder a pacotes internos (`.business` ou `.persistence`) de outros módulos. O acesso externo a outros módulos deve ser feito exclusivamente através das respetivas Fachadas Públicas (`*Facade`).
   - **Isolamento de Dados:** Nenhuma query ou chave estrangeira cruzada pode ser criada para tabelas fora do domínio deste módulo.

Valida a implementação garantindo que o código passa sem erros no teste arquitetural ArchUnit (ModularMonolithArchitectureTest).