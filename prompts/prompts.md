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