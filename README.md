# Lisboa Pedala — Monólito Modular

**Unidade Curricular:** Design de Software (Mestrado em Engenharia Informática — FCUL)  
**Projeto:** Trabalho Prático 1 (TP1) — Especificação Arquitetural e Implementação de um Monólito Modular  
**Modelo de IA Utilizado:** **Gemini 3.8 Flash (High)** (assistente de programação agentic via Google Antigravity)

---

## 1. Visão Geral da Arquitetura

O **Lisboa Pedala** é um sistema de partilha de bicicletas elétricas e convencionais concebido como um **Monólito Modular particionado por Domínio**, implementado em **Java 21** e com persistência relacional em ficheiro local **SQLite** (`lisboapedala.db`).

O sistema é empacotado numa **única unidade de implantação** (JAR), onde cada módulo representa uma fronteira lógica de domínio encapsulada:

```
pt.ulisboa.lisboapedala
├── common                 # Utilitários técnicos transversais (DatabaseManager, UserRepository)
├── dock_access            # Doca física, trinco, buffer e sincronização offline (US-1, US-3)
├── trip_lifecycle         # Gestão transacional do ciclo de vida das viagens (US-1)
├── tariff_billing         # Faturação, aplicação de regras tarifárias e retenção fiscal de 10 anos (US-1)
├── station_availability   # Ocupação em tempo real e contadores consolidados de bicicletas/docas
├── sensor_ingestion       # Telemetria IoT periódica (35 req/s) e deteção de anomalias técnicas (US-2)
├── maintenance_orders     # Registo, atribuição à pool geral e resolução de avarias (US-2)
├── reporting_audit        # Relatórios municipais por freguesia, CO₂ evitado e dados abertos (US-4)
├── history_reader         # Consulta agregada de histórico do utilizador e recibos fiscais (US-8)
└── fleet_redistribution   # Esqueleto de propostas de rotas de redistribuição para carrinhas (US-5)
```

### Princípios Arquiteturais Fundamentais
1. **Encapsulamento Estrito por Fachadas Públicas (ADR-005):** Cada módulo expõe exclusivamente uma classe de fachada pública (`*Facade.java`) e DTOs imutáveis (Java Records). As classes de negócio (`.business`) e persistência (`.persistence`) são estritamente internas e inacessíveis a outros módulos.
2. **Isolamento de Dados por Módulo (ADR-006):** Não existem acessos diretos nem `JOINs` entre tabelas de módulos distintos. Cada módulo detém o seu próprio `schema.sql` com prefixo obrigatório nas tabelas (ex: `trip_lifecycle_trips`, `tariff_billing_invoices`). Referências a entidades de outros módulos são guardadas como identificadores lógicos (`String`), sem chaves estrangeiras físicas intermódulos.
3. **Módulo de Leitura Puro (ADR-002 e ADR-006):** O `history_reader` é um agregador de consulta que consome as APIs públicas de `trip_lifecycle` e `tariff_billing`, não possuindo tabelas na base de dados nem alterando dados.
4. **Governação por Fitness Functions (ArchUnit):** A conformidade arquitetural é validada automaticamente a cada build pelo [`ModularMonolithArchitectureTest.java`](file:///c:/FCUL/1%20semestre/DS/TP1_DS/tests/architecture/ModularMonolithArchitectureTest.java), garantindo a ausência de ciclos e o cumprimento estrito da matriz *Allowed-to-Use*.
5. **Top 3 Características de Qualidade:**
   - **Robustez:** Operação offline das docas até 24 horas, sincronização em lote e idempotência na finalização de viagens e emissão de faturas (ADR-004).
   - **Elasticidade a Pedido:** Desacoplamento do caminho crítico transacional face a tarefas analíticas e ingestão contínua de telemetria IoT.
   - **Disponibilidade:** Caminho de execução enxuto no desbloqueio (>90% de sucesso sem depender de integrações lentas) e simulação de gateways externos (AT e Pagamentos) com adaptadores resilientes.

---

## 2. Documentos Presentes no Repositório

O projeto contém documentação arquitetural exaustiva organizada na raiz e na pasta `docs/`:

| Documento | Descrição e Propósito |
| :--- | :--- |
| [`ARCHITECTURE.md`](file:///c:/FCUL/1%20semestre/DS/TP1_DS/ARCHITECTURE.md) | **Documento normativo de referência:** define o estilo arquitetural, as responsabilidades dos 9 módulos, a matriz obrigatória de *Allowed-to-Use*, regras de transações SQLite e convenções de código. |
| [`docs/01-caracteristicas.md`](file:///c:/FCUL/1%20semestre/DS/TP1_DS/docs/01-caracteristicas.md) | Levantamento das características de qualidade implícitas e explícitas do enunciado, com definição detalhada dos cenários concretos do **Top 3** (Robustez, Elasticidade a Pedido, Disponibilidade). |
| [`docs/02-componentes.md`](file:///c:/FCUL/1%20semestre/DS/TP1_DS/docs/02-componentes.md) | Descrição dos componentes lógicos consolidados, mapeamento face às User Stories e análise de coesão, conascência e métricas de acoplamento/instabilidade ($I$). |
| [`docs/03-particionamento-e-vistas.md`](file:///c:/FCUL/1%20semestre/DS/TP1_DS/docs/03-particionamento-e-vistas.md) | Comparação entre particionamento em camadas vs. domínio, **Vista de Módulos** detalhada com a árvore de ficheiros, **Vista C&C** (fluxo Desbloquear-Devolver-Cobrar) e **Vista de Alocação**. |
| [`docs/reflexao.md`](file:///c:/FCUL/1%20semestre/DS/TP1_DS/docs/reflexao.md) | **Relatório de Reflexão Arquitetural:** análise crítica sobre o uso de IA, 4 casos documentados de violações arquiteturais geradas e avaliação das forças e ângulos mortos das *fitness functions*. |
| [`docs/adr/`](file:///c:/FCUL/1%20semestre/DS/TP1_DS/docs/adr/) | **Architecture Decision Records (ADRs):**<br>• `ADR-001`: Decomposição histórica inicial fina (substituída).<br>• `ADR-002`: Consolidação em 9 componentes lógicos de domínio.<br>• `ADR-003`: Monólito modular com unidade de implantação única em SQLite.<br>• `ADR-004`: Resiliência perante falhas de rede, buffer 24h e idempotência.<br>• `ADR-005`: Fachadas públicas e fitness functions ArchUnit.<br>• `ADR-006`: Isolamento de dados por módulo e retenção fiscal de 10 anos. |
| [`docs/diagramas/`](file:///c:/FCUL/1%20semestre/DS/TP1_DS/docs/diagramas/) | Diagramas visuais C4 (Contexto, Contentores e Componentes) e especificações PlantUML/Mermaid correspondentes. |
| [`prompts/`](file:///c:/FCUL/1%20semestre/DS/TP1_DS/prompts/) | Histórico de prompts executados e os respetivos relatórios de validação arquitetural para cada módulo implementado (Prompts 1 a 8). |

---

## 3. Modelo de IA Utilizado

O desenvolvimento e verificação do código e da especificação foram apoiados pelo modelo **Gemini 3.8 Flash (High)**, integrado no ambiente Google Antigravity.

* **Papel da IA:** Geração assistida de esqueletos de classes, DTOs, controladores de apresentação, adaptadores, esquemas DDL e suites de testes JUnit 5 / ArchUnit.
* **Governação do Modelo:** A equipa manteve o controlo estrito do código através de *prompts estruturados com contexto explícito* (ADRs + `ARCHITECTURE.md`), validação por compilação Java 21 e execução das *fitness functions* automatizadas (`ModularMonolithArchitectureTest`).
* **Casos Críticos Identificados:** Conforme documentado em [`docs/reflexao.md`](file:///c:/FCUL/1%20semestre/DS/TP1_DS/docs/reflexao.md), o modelo tendeu a criar atalhos (ex: persistência volátil em memória, inserção de regras de utilizador no pacote `common`, injeção indevida de dependências entre `sensor_ingestion` e `maintenance_orders`), os quais foram sistematicamente intercetados por revisão humana e pelas regras ArchUnit.

---

## 4. Visão Geral do Código-Fonte

O código-fonte segue a separação em camadas técnicas internas (`presentation`, `business`, `persistence`) dentro de cada pacote de domínio:

```
src/main/java/pt/ulisboa/lisboapedala/
├── LisboaPedalaApp.java                  # Ponto de entrada CLI interativo com login e menus
│
├── dock_access/                          # US-1 & US-3
│   ├── DockAccessFacade.java             # Fachada pública (unlockBike, returnBike, syncOfflineEvents)
│   ├── presentation/DockController.java  # Controlador de apresentação para as estações físicas
│   ├── business/DockAccessService.java   # Coordena validação e buffer temporário offline
│   └── persistence/                      # DockEventRepository e schema.sql (dock_access_events)
│
├── trip_lifecycle/                       # US-1
│   ├── TripLifecycleFacade.java          # Início, fecho e consulta de viagens
│   ├── presentation/TripController.java
│   ├── business/TripService.java         # Idempotência por returnEventId e cálculo de duração
│   └── persistence/                      # TripRepository e schema.sql (trip_lifecycle_trips)
│
├── tariff_billing/                       # US-1 & ADR-006
│   ├── TariffBillingFacade.java          # Cálculo e cobrança de viagens
│   ├── business/TariffCalculator.java    # 45 min grátis assinantes, frações 15 min, avulsos
│   ├── business/BillingService.java      # Comunicação síncrona com AT e Pagamentos
│   ├── business/adapters/                # Mocks de PaymentGatewayAdapter e TaxAuthorityAdapter
│   └── persistence/                      # InvoiceRepository e schema.sql (tariff_billing_invoices)
│
├── station_availability/                 # Ocupação e Mapa
│   ├── StationAvailabilityFacade.java    # Consulta e atualização de docas livres/ocupadas
│   ├── business/StationAvailabilityService.java
│   └── persistence/                      # StationRepository e schema.sql (station_availability_stations)
│
├── sensor_ingestion/                     # US-2
│   ├── SensorIngestionFacade.java        # Receção de pings de GPS, bateria e anomalias
│   ├── presentation/TelemetryController.java
│   ├── business/                         # TelemetryParserService, LostBikeDetector, MalfunctionEmitter
│   └── persistence/                      # TelemetryLogRepository e schema.sql
│
├── maintenance_orders/                   # US-2
│   ├── MaintenanceOrdersFacade.java      # Gestão de tickets de avaria
│   ├── business/TicketService.java       # Atribuição à pool de técnicos e sincronização de anomalias
│   └── persistence/                      # MaintenanceOrderRepository e schema.sql
│
├── reporting_audit/                      # US-4
│   ├── ReportingAuditFacade.java         # Relatórios municipais e portal de dados abertos
│   ├── business/ReportGeneratorService.java # Agregação anónima por freguesia e cálculo de CO₂
│   └── persistence/                      # ReportRepository e schema.sql
│
├── history_reader/                       # US-8
│   ├── HistoryReaderFacade.java          # Consulta pura agregada (viagens + faturas)
│   └── business/HistoryAggregationService.java
│
└── fleet_redistribution/                 # Bare-bones (US-5)
    ├── FleetRedistributionFacade.java    # Consulta síncrona a estações e consulta de rotas
    ├── presentation/RouteReader.java
    ├── business/CalculateRoutes.java     # Esqueleto de cálculo de desequilíbrios
    └── persistence/                      # RedistributionRepository e schema.sql
```

---

## 5. Como Executar e Testar

### Pré-requisitos
* **Java 21** instalado e configurado na variável de ambiente `JAVA_HOME`.
* Acesso à internet para descarregar dependências Maven na primeira execução.

### Executar a Suite de Testes Completa (38 Testes)
O projeto inclui o wrapper Maven (`mvnw.cmd` para Windows ou `./mvnw` para Unix). Todos os testes de unidade, integração e as *fitness functions* de ArchUnit podem ser executados com:

```bash
# Windows (PowerShell / CMD)
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```

> **Verificação:** A execução cobre 38 testes com 0 falhas, incluindo as 10 regras arquiteturais de [`ModularMonolithArchitectureTest`](file:///c:/FCUL/1%20semestre/DS/TP1_DS/tests/architecture/ModularMonolithArchitectureTest.java), validação de ausência de ciclos e testes de idempotência offline (`US1IdempotencyAndOfflineSyncTest`).

### Executar a Aplicação Interativa: `LisboaPedalaApp`

A aplicação em consola (CLI) centraliza todas as funcionalidades do sistema num ambiente interativo, intuitivo e com persistência contínua no ficheiro SQLite local `lisboapedala.db`.

O ponto de entrada principal do projeto é a classe [`LisboaPedalaApp.java`](file:///c:/FCUL/1%20semestre/DS/TP1_DS/src/main/java/pt/ulisboa/lisboapedala/LisboaPedalaApp.java).

#### Método 1: No VS Code ou IDE (Mais Simples — "Run Java")
> **Basta abrir o ficheiro [`LisboaPedalaApp.java`](file:///c:/FCUL/1%20semestre/DS/TP1_DS/src/main/java/pt/ulisboa/lisboapedala/LisboaPedalaApp.java) e carregar no botão "Run Java" (ou "Run") no canto superior direito do editor.**
> 
> A aplicação inicia imediatamente no terminal integrado, pronta a ser utilizada.

#### Método 2: Via Terminal (Linha de Comandos)
Em alternativa, pode ser iniciada através do wrapper Maven:

```bash
# Windows (PowerShell / CMD)
.\mvnw.cmd compile exec:java

# Linux / macOS
./mvnw compile exec:java
```

---

### Funcionalidades Disponíveis na `LisboaPedalaApp`
Ao executar o programa, a consola disponibiliza um menu completo de demonstração:
1. **Autenticação Segura (Ecrã Inicial):**
   - Registo de novo utilizador (perfil assinante com passe ou utilizador ocasional, com hashing de credenciais).
   - Login para restaurar o perfil ativo a partir da base de dados SQLite.
2. **Menu Principal do Ciclista (US-1 e US-8):**
   - **Desbloquear Bicicleta:** Seleção de estação e bicicleta, validação transacional e início da viagem.
   - **Devolver Bicicleta:** Encaixe físico na doca de destino, cálculo exato da duração e faturação imediata.
   - **Consultar Mapa de Estações:** Ocupação em tempo real de bicicletas disponíveis e docas livres (`station_availability`).
   - **Histórico e Recibos Fiscais:** Consulta unificada das viagens realizadas e recibos fiscais com link da AT (`history_reader`).
3. **Menu de Gestão e Operações Técnicas (US-2 e US-4):**
   - **Telemetria de Sensores:** Envio de pings IoT e reporte de anomalias técnicas (`sensor_ingestion`).
   - **Ordens de Manutenção:** Acompanhamento e resolução de tickets pela equipa de técnicos (`maintenance_orders`).
   - **Relatórios Municipais:** Emissão de relatórios de utilização por freguesia de Lisboa, estimativa de CO₂ poupado face ao automóvel e publicação de métricas para o portal de dados abertos (`reporting_audit`).

