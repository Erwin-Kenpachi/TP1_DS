### Prompt 6
[Contexto fornecido: ARCHITECTURE.md, adr/ADR-002.md, adr/ADR-003.md, adr/ADR-005.md, adr/ADR-006.md, docs/01-caracteristicas.md, docs/02-componentes.md, docs/03-particionamento-e-vistas.md]

Por favor, implementa exclusivamente o módulo de domínio `fleet_redistribution` em Java 21 para o sistema Lisboa Pedala, com o propósito de calcular desequilíbrios na rede de 180 estações e gerar rotas otimizadas de rebalanceamento de bicicletas para as 6 carrinhas municipais (US-5), prevenindo ruturas de stock ou sobrelotação de docas que ameacem o contrato de concessão.

Deves respeitar rigorosamente as diretrizes arquiteturais dos documentos de especificação e a vista de módulos de docs/03-particionamento-e-vistas.md, cumprindo a seguinte organização:

1. Estrutura de Pacotes e Componentes Internos (em pt.ulisboa.lisboapedala.fleet_redistribution):
   - Fachada Pública (FleetRedistributionFacade.java): Ponto de contacto estável para os clientes e outros módulos:
     * List<RedistributionRouteDTO> getActiveRoutes(): Consulta as rotas e tarefas de recolha/entrega atribuídas às 6 carrinhas;
     * RedistributionRouteDTO generateOptimalRoutes(): Aciona o cálculo heurístico de rebalanceamento com base na ocupação atual;
     * void reportTaskCompletion(String routeId, String taskId): Confirma a execução de recolha/entrega física de bicicletas numa estação.
   - Camada de Apresentação (presentation/):
     * RouteReader.java (ou FleetController.java): Endpoints REST/CLI para as equipas das carrinhas consultarem as suas folhas de rota em campo.
   - Camada de Lógica de Negócio (business/):
     * CalculateRoutes.java: Lógica central do algoritmo de otimização de frota. Identifica estações críticas (ocupação < 20% com risco de ficar sem bicicletas ou > 80% sem docas livres) e agrupa-as em pares recolha/entrega;
     * RoutingClientService.java: Adaptador de simulação para o serviço externo de mapas e cálculo de distâncias/trânsito (conforme os diagramas C4 de Contentores e Componentes);
     * FleetBalancingPolicy.java: Regras operacionais para priorizar estações de maior afluência em horas de ponta (8h00–9h30 e 17h30–19h00).
   - Camada de Persistência (persistence/):
     * RedistributionRepository.java: Acesso à base de dados para guardar rotas ativas, tarefas das carrinhas e histórico de transferências;
     * schema.sql: Tabela privada `fleet_redistribution_routes` e `fleet_redistribution_tasks` num esquema SQL isolado.

2. Regras Estritas de Dependência e Allowed-to-Use (ADR-005 e ARCHITECTURE.md):
   - Módulo Periférico Flexível: O módulo apresenta instabilidade máxima ($I = 1.0$, $Ca = 0$, $Ce = 1$). Nenhum outro módulo de negócio depende dele.
   - Dependência Permitida: O `fleet_redistribution` PODE APENAS depender de `station_availability`, consumindo exclusivamente a sua fachada pública (`StationAvailabilityFacade.getStationOccupancy()`) para ler o número de docas e bicicletas livres.
   - Dependências Proibidas: É expressamente proibido importar ou aceder a `dock_access`, `trip_lifecycle`, `tariff_billing` ou `sensor_ingestion`.
   - Encapsulamento: Proibido aceder a classes internas (`.business` ou `.persistence`) de `station_availability` ou efetuar queries SQL diretas à tabela de estações.

3. Impacto nas Características Arquiteturais (docs/01-caracteristicas.md):
   - **Disponibilidade (>90% das estações operacionais):** O módulo deve prevenir que mais de 10% das estações fiquem sem bicicletas para desbloquear, evitando multas contratuais ao operador municipal.
   - **Elasticidade a Pedido (Picos de Procura):** O algoritmo deve focar o rebalanceamento dinâmico nos desequilíbrios provocados pelos picos de afluência (Santos Populares, maratonas ou movimentos pendulares casa-trabalho).
   - **Desempenho / Não Bloqueio:** O cálculo de otimização de rotas não pode bloquear as transações de início e fim de viagens do monólito.

4. Verificação Arquitetural Automática:
   - A implementação deve passar integralmente nos testes ArchUnit (`ModularMonolithArchitectureTest.no_cycles_between_modules` e `no_access_to_internal_packages_of_other_modules`).

---

## 2. Resultado da Geração
- **Decisão:** [Aceite / Rejeitado / Aceite com Correções]
- **Justificação:** [Exemplo: O código gerou a fachada FleetRedistributionFacade e os algoritmos de cálculo sem criar dependências cíclicas, respeitando a chamada síncrona exclusiva a StationAvailabilityFacade.]

## 3. Deteção de Violações Arquiteturais
- **Houve violação da arquitetura?:** [Sim / Não]
- **Quem a detetou?:** [Fitness Function (ArchUnit) / Revisão Humana / Ninguém até mais tarde]
- **Descrição da ocorrência:** [Exemplo: A IA tentou aceder diretamente à base de dados SQLite ou ao StationRepository para poupar uma chamada à fachada pública, tendo sido travada pela revisão humana ou pela regra de encapsulamento do ArchUnit.]

