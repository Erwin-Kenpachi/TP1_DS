### Prompt 7

[Contexto fornecido: ARCHITECTURE.md, adr/ADR-002.md, adr/ADR-003.md, adr/ADR-005.md, adr/ADR-006.md, docs/03-particionamento-e-vistas.md]

Por favor, implementa exclusivamente o módulo de domínio `maintenance_orders` em Java 21 para o sistema Lisboa Pedala, suportando o registo, priorização, atribuição a técnicos e conclusão de ordens de manutenção originadas por avarias reportadas (US-2).

Deves respeitar rigorosamente as diretrizes arquiteturais definidas em ARCHITECTURE.md e nos ADRs referidos, bem como a seguinte estrutura de componentes:

1. Estrutura de Pacotes e Componentes Internos (em pt.ulisboa.lisboapedala.maintenance_orders):
   - Fachada Pública (MaintenanceOrdersFacade.java): Ponto de entrada para outros módulos e operações de gestão:
     * createOrderFromAlert(String bikeId, String issueDescription): Cria nova ordem de reparação;
     * List<MaintenanceOrderDTO> getPendingOrders(): Devolve a lista de manutenções pendentes para os técnicos;
     * assignOrderToTechnician(String orderId, String technicianId): Atribui o ticket a um técnico;
     * completeOrder(String orderId): Marca a ordem como resolvida.
   - Camada de Apresentação (presentation/MaintenanceController.java): Endpoints REST para o painel de campo dos técnicos de manutenção.
   - Camada de Lógica de Negócio (business/):
     * TicketService.java: Gere o ciclo de vida das avarias (aberta, em progresso, resolvida);
     * TechnicianAssignmentService.java: Regras de atribuição e priorização de ordens de serviço;
     * Consome de forma síncrona a SensorIngestionFacade (do módulo sensor_ingestion) para verificar anomalias e falhas técnicas pendentes.
   - Camada de Persistência (persistence/):
     * MaintenanceOrderRepository.java: Repositório para gravação dos tickets de manutenção;
     * schema.sql: Definição da tabela privada `maintenance_orders_tickets` em SQLite exclusivo do módulo.

2. Regras Estritas de Dependência e Allowed-to-Use (ADR-005):
   - **Direção do Acoplamento:** O módulo `maintenance_orders` tem permissão para depender de `sensor_ingestion` (invocando exclusivamente a `SensorIngestionFacade`) e de `station_availability`. 
   - **Isolamento de Fachadas:** É estritamente proibido aceder a pacotes internos (`.business` ou `.persistence`) de outros módulos. Toda a interação com sensores é feita estritamente através do contrato público da fachada.
   - **Independência Inversa:** O módulo `sensor_ingestion` não pode conhecer nem depender de `maintenance_orders` (estabilidade máxima $I = 0.0$).

3. Verificação Arquitetural Automática:
   - A implementação tiene de passar integralmente nas fitness functions com ArchUnit (`ModularMonolithArchitectureTest`), validando a ausência de ciclos e o cumprimento das regras *Allowed-to-Use*.

- Na camada de negócio (business/), o serviço responsável pelos tickets (ex: TicketService.java) deve invocar ativamente a SensorIngestionFacade.getUnresolvedAnomalies() do módulo sensor_ingestion para recolher as anomalias técnicas pendentes e convertê-las em ordens de manutenção (respeitando a direção de acoplamento permitida em ARCHITECTURE.md).

Cria na interface uma maneira de me autenticar na app como utilizador basta uma simples pergunta nova no registo a perguntar que tipo de utilizador é.
O técnico pode ver as ordens de manutenção que tem e atualizar o estado de uma bike.

## 2. Resultado da Geração
- **Decisão:**
- **Justificação:**
## 3. Deteção de Violações Arquiteturais


### Main
### Prompt 7
[Contexto fornecido: ARCHITECTURE.md, adr/ADR-002.md, adr/ADR-003.md, adr/ADR-005.md, adr/ADR-006.md, docs/03-particionamento-e-vistas.md]

Por favor, implementa exclusivamente o módulo de domínio `maintenance_orders` em Java 21 para o sistema Lisboa Pedala, suportando a US-2: **Como técnico, recebo uma ordem de manutenção quando uma bicicleta reporta avaria**.

Deves respeitar rigorosamente as diretrizes arquiteturais definidas em ARCHITECTURE.md e nos ADRs referidos, bem como a seguinte estrutura essencial:

1. Estrutura de Pacotes e Componentes Internos (em `pt.ulisboa.lisboapedala.maintenance_orders`):
   - **Fachada Pública (`MaintenanceOrdersFacade.java`)**: Ponto de entrada com os métodos essenciais:
     * `void registerOrder(String bikeId, String issueDescription)`: Regista/cria uma nova ordem de reparação a partir da anomalia detetada.
     * `List<MaintenanceOrderDTO> getPendingOrders()`: Devolve as ordens de manutenção pendentes para os técnicos.
     * `void updateBikeStatus(String bikeId, String status)`: Permite ao técnico atualizar o estado de uma bicicleta avariada.
   - **Camada de Apresentação (`presentation/MaintenanceController.java`)**: 
     * Endpoints REST para o painel de campo dos técnicos de manutenção e inclusão de uma forma simples de autenticação/registo por papel (perfil de utilizador vs. técnico).
   - **Camada de Lógica de Negócio (`business/`)**:
     * `TicketService.java`: Consulta de forma síncrona a `SensorIngestionFacade.getUnresolvedAnomalies()` do módulo `sensor_ingestion` para recolher anomalias técnicas pendentes e convertê-las em ordens de manutenção locais através de `registerOrder`.
     * `TechnicianAssignmentService.java`: Gestão básica de atribuição/visualização de ordens para os técnicos.
   - **Camada de Persistência (`persistence/`)**:
     * `MaintenanceOrderRepository.java`: Repositório para gravação dos tickets de manutenção.
     * `schema.sql`: Definição da tabela privada `maintenance_orders_tickets` em SQLite exclusivo do módulo.

2. Regras Estritas de Dependência e Allowed-to-Use (ADR-005):
   - **Direção do Acoplamento:** O módulo `maintenance_orders` tem permissão para depender de `sensor_ingestion` (invocando exclusivamente a `SensorIngestionFacade`) e de `station_availability`.
   - **Isolamento de Fachadas:** É estritamente proibido aceder a pacotes internos (`.business` ou `.persistence`) de outros módulos. Toda a interação com sensores é feita estritamente através do contrato público da fachada.
   - **Independência Inversa:** O módulo `sensor_ingestion` não pode conhecer nem depender de `maintenance_orders` (estabilidade máxima $I = 0.0$).

3. Verificação Arquitetural Automática:
   - A implementação tem de passar integralmente nas *fitness functions* com ArchUnit (`ModularMonolithArchitectureTest`), validando a ausência de ciclos e o cumprimento estrito das regras *Allowed-to-Use*.

---

## 2. Resultado da Geração
- **Decisão:*Quase totalmente positivo* 
- **Justificação:*Unico defeito foi ter adicionado acesso à Station avaiblity na facade* 

## 3. Deteção de Violações Arquiteturais
- **Houve violação da arquitetura?:*Houve* 
- **Descrição da ocorrência:*Deu acesso à Station avaiblity na facade*