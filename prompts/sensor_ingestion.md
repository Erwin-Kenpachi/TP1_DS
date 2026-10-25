### Prompt 5
[Contexto fornecido: ARCHITECTURE.md, adr/ADR-002.md, adr/ADR-003.md, adr/ADR-005.md, adr/ADR-006.md, docs/01-caracteristicas.md, docs/03-particionamento-e-vistas.md]

Por favor, implementa exclusivamente o módulo de domínio `sensor_ingestion` em Java 21 para o sistema Lisboa Pedala, com o propósito de receber e processar a telemetria periódica das bicicletas elétricas (1 050 bicicletas a cada 30 segundos em andamento), detetar anomalias técnicas (US-2) e identificar bicicletas perdidas fora de doca.

Deves respeitar rigorosamente as diretrizes arquiteturais definidas em ARCHITECTURE.md e a vista detalhada de módulos de docs/03-particionamento-e-vistas.md, com a seguinte estrutura de componentes:

1. Estrutura de Pacotes e Componentes Internos (em pt.ulisboa.lisboapedala.sensor_ingestion):
   - Fachada Pública (SensorIngestionFacade.java): Único ponto de entrada para outros módulos consultarem dados em tempo real:
     * TelemetryDTO getLatestTelemetry(String bikeId): Devolve o último registo de telemetria da bicicleta;
     * BikeHealthStatusDTO getBikeHealthStatus(String bikeId): Estado de saúde e alertas técnicos ativos;
     * List<BikeAnomalyEventDTO> getUnresolvedAnomalies(): Lista de anomalias detetadas pendentes de manutenção.
   - Camada de Apresentação / Ingestão de Rede (presentation/):
     * TelemetryController.java: Endpoint REST/Webhook preparado para suportar o débito contínuo de pings IoT das 1 050 bicicletas (~35 req/s).
   - Camada de Lógica de Negócio (business/):
     * TelemetryParserService.java: Descodifica o payload bruto enviado pelo hardware IoT (GPS, percentagem de bateria, odómetro, temperatura, integridade de travões);
     * LostBikeDetectorService.java: Deteta falhas prolongadas de sinal (> 2h sem ping e fora de doca) e sinaliza suspeita de furto/abandono;
     * MalfunctionService.java: Avalia os limites operacionais (ex: bateria crítica < 15%, anomalia nos travões) e classifica anomalias mecânicas/elétricas;
     * MalfunctionEmitter.java: Regista internamente e expõe eventos de anomalia técnica para consumo desacoplado, SEM acoplamento direto ou chamada síncrona a módulos externos;
     * SignalDispatcherService.java: Despacha e encaminha leituras sanitizadas para os repositórios locais.
   - Camada de Persistência (persistence/):
     * TelemetryLogRepository.java: Repositório para gravação e consulta otimizada de leituras de telemetria;
     * schema.sql: Definição da tabela privada `sensor_ingestion_telemetry` em SQLite exclusivo do módulo.

2. Regras Estritas de Dependência e Resolução de Divergências (*Allowed-to-Use*):
   - **ATENÇÃO À REGRA NORMATIVA (ARCHITECTURE.md vs Doc 03):** Embora o documento 03-particionamento-e-vistas.md na tabela 2.4 indique permissão para usar `station_availability` e refira que `MalfunctionEmitter` dispara para a manutenção, prevalece a regra de ARCHITECTURE.md (Secção 3 e 7) e do ArchUnit:
     * O `sensor_ingestion` é um módulo fonte com estabilidade máxima ($I = 0.0$). **NÃO PODE DEPENDER de nenhum outro módulo de domínio** (proibido importar ou injetar classes de `station_availability`, `dock_access`, `trip_lifecycle` ou `maintenance_orders`).
     * É o módulo `maintenance_orders` que deve consultar `SensorIngestionFacade` para abrir as ordens de reparação da US-2, e nunca o inverso.
   - **Isolamento de Classes:** Todas as classes dos subpacotes `business/`, `persistence/` e `presentation/` são privadas/protegidas ao módulo. Nenhuma classe interna de outro módulo pode ser importada.

3. Restrições de Privacidade e Dados (ADR-006 e RGPD):
   - Os registos de telemetria e posições GPS devem ser associados estritamente ao identificador da bicicleta (`bike_id`), sem referenciar utilizadores, nomes, NIFs ou dados pessoais de identificação.

4. Verificação Arquitetural Automática:
   - A implementação tem de passar integralmente nas fitness functions com ArchUnit (`ModularMonolithArchitectureTest.sensor_ingestion_should_not_depend_on_other_domain_modules` e `no_access_to_internal_packages_of_other_modules`).

---

## 2. Resultado da Geração
- **Decisão:** Aceite com correções de isolamento de dependências.
- **Justificação:** A IA gerou a estrutura funcional de ingestão e os serviços especializados (`TelemetryParserService`, `LostBikeDetectorService`, `MalfunctionService`), mas inicialmente tentou importar a fachada de `maintenance_orders` dentro do `MalfunctionEmitter`. Foi necessário corrigir para manter a dependência unidirecional correta exigida pela regra normativa do monólito modular.

## 3. Deteção de Violações Arquiteturais
- **Houve violação da arquitetura?:** Sim.
- **Quem a detetou?:** A Fitness Function (ArchUnit) e Revisão Humana.
- **Descrição da ocorrência:** Ao gerar o `MalfunctionEmitter.java`, a IA seguiu a menção informal do texto de `03-particionamento-e-vistas.md` e tentou invocar diretamente `MaintenanceOrdersFacade.createOrder()`. Ao correr a fitness function `sensor_ingestion_should_not_depend_on_other_domain_modules`, o ArchUnit lançou uma violação de arquitetura imediata (`AssertionError`), bloqueando o commit até a dependência ser removida e invertida para o consumidor `maintenance_orders`.
