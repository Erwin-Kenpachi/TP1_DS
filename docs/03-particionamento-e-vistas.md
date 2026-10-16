# 03 — Particionamento e Vistas Arquiteturais

## 1. Introdução

Este documento especifica a organização estrutural e as vistas arquiteturais do sistema **Lisboa Pedala**, tendo por base os componentes lógicos identificados em `02-componentes.md` e os requisitos do enunciado.

O objetivo é definir a organização do código-fonte em tempo de desenvolvimento, explicitar as dependências permitidas entre módulos e descrever a colaboração entre componentes durante a execução dos principais casos de utilização. Pretende-se reduzir o acoplamento, aumentar a coesão e facilitar a evolução e a verificação da arquitetura.

O documento apresenta as seguintes vistas:

1. **Particionamento de topo e trade-offs:** comparação entre uma organização técnica por camadas e uma organização por domínio, justificando a opção selecionada.
2. **Vista de módulos e regras `Allowed-to-Use`:** organização dos módulos de topo e definição das dependências permitidas e proibidas.
3. **Vista de Componentes e Conectores (C&C)** 
4. **Vista de Alocação:** distribuição dos componentes lógicos pelos elementos de execução previstos para o sistema.

---

## 2. Particionamento de Topo

O particionamento de topo define a primeira divisão do código-fonte em módulos e influencia a forma como as alterações futuras são distribuídas pela implementação.

Para o Lisboa Pedala, foram consideradas duas alternativas: o particionamento técnico, que organiza o código por camadas, e o particionamento por domínio, que organiza o código de acordo com as responsabilidades funcionais do sistema.

### 2.1. Comparação de alternativas: técnico vs. domínio

#### Alternativa A — Particionamento técnico (*Layered Architecture*)

Nesta abordagem, o código é organizado de acordo com a sua função técnica, por exemplo, apresentação, lógica de negócio e persistência.

```text
lisboa_pedala/
├── presentation/
│   ├── dock_access/
│   ├── trip_lifecycle/
│   ├── tariff_billing/
|   ├── station_availability/
|   ├── telemetry_ingestion/
│   ├── fleet_redistribution/ 
│   ├── maintenance_orders/ 
│   └── reporting_audit/
├── business/
│   ├── dock_access/
│   ├── trip_lifecycle/
│   ├── tariff_billing/
|   ├── station_availability/
|   ├── telemetry_ingestion/
│   ├── fleet_redistribution/ 
│   ├── maintenance_orders/ 
│   └── reporting_audit/
├── persistence/
│   ├── dock_access/
│   ├── trip_lifecycle/
│   ├── tariff_billing/
|   ├── station_availability/
|   ├── telemetry_ingestion/
│   ├── fleet_redistribution/ 
│   ├── maintenance_orders/ 
│   └── reporting_audit/
│   
└── database
```

**Vantagens:**

- Torna explícita a separação entre responsabilidades técnicas.
- Facilita a identificação de funcionalidades relacionadas com a interface, a lógica de negócio ou o acesso aos dados.
- Pode simplificar alterações tecnológicas transversais, desde que as dependências entre camadas estejam bem definidas.

**Desvantagens no Lisboa Pedala:**

- **Dispersão funcional:** uma alteração ao processo de faturação ou às regras de devolução pode obrigar a modificar ficheiros em várias camadas.
- **Menor localidade da mudança:** os elementos que implementam uma funcionalidade não ficam necessariamente próximos no código-fonte.
- **Risco de *Architecture Sinkhole*:** algumas chamadas podem atravessar várias camadas sem acrescentar lógica relevante, funcionando apenas como encaminhamento entre componentes.

Esta alternativa favorece a organização por responsabilidade técnica, mas pode tornar menos evidente a localização das regras de negócio. Para alem disto a divisão layered, convem ser evitada aquando o desenvolvimento de sistemas e aplicações grandes, como é o caso da Lisboa Pedala. 

#### Alternativa B — Particionamento por domínio (*Modular Monolith*)

Nesta abordagem, o primeiro nível de organização corresponde às responsabilidades funcionais do sistema. Cada módulo reúne os elementos necessários para implementar a sua área do domínio, mantendo contratos explícitos para a comunicação com outros módulos.

```text
lisboa_pedala/
├── dock_access/
├── trip_lifecycle/
├── tariff_billing/
├── station_availability/
├── sensor_ingestion/
├── fleet_redistribution/
├── maintenance_orders/
└── reporting_audit/
```

**Vantagens no Lisboa Pedala:**

- **Localidade da mudança:** alterações às regras de tarifação ficam concentradas em `tariff_billing`; alterações ao ciclo de vida das viagens ficam concentradas em `trip_lifecycle`.
- **Coesão funcional:** cada módulo agrupa responsabilidades relacionadas com uma capacidade do sistema.
- **Fronteiras explícitas:** as dependências entre módulos podem ser documentadas e verificadas automaticamente.
- **Evolução controlada:** uma eventual necessidade de separar um módulo para outro processo ou serviço poderá ser avaliada posteriormente, caso existam razões concretas para isso.

**Desvantagens:**

- Alterações transversais, como a migração do mecanismo de persistência, podem afetar vários módulos.
- Sem regras de dependência e encapsulamento, os módulos podem começar a aceder diretamente aos detalhes internos uns dos outros, degradando a modularidade.
- A divisão por domínio exige cuidado na definição da propriedade dos dados e das interfaces entre módulos.
-  Evitar quando existe um exigência elevada de escalabilidade, elasticidade, disponibilidade, tolerância a falhas ou desempenho


### 2.2. Avaliação dos trade-offs e seleção

No contexto deste trabalho optou-se pelo **particionamento por domínio, implementado como monólito modular**.

A principal razão para a escolha do monólito modular prende-se com a natureza das alterações funcionais previstas para o Lisboa Pedala. A gestão de viagens, a tarifação, a disponibilidade das estações e a manutenção representam responsabilidades distintas que beneficiam de módulos com fronteiras bem definidas. Desta forma, uma alteração às regras de tarifação tende a ficar concentrada em ``tariff_billing``, enquanto, numa arquitetura organizada por camadas técnicas, os elementos dessa funcionalidade podem encontrar-se distribuídos por várias camadas, reduzindo a localidade da mudança.

Esta vantagem implica, contudo, um maior cuidado na definição e aplicação das dependências entre módulos. Além disso, as alterações tecnológicas transversais, como a substituição da tecnologia de persistência, podem afetar vários módulos. Assim, o particionamento por domínio não é necessariamente a melhor opção para todos os tipos de alteração, devendo a escolha refletir as prioridades do sistema.

A manutenção de uma única unidade de implantação é também compatível com a dimensão da equipa e com o orçamento disponível, permitindo evitar, nesta fase, a complexidade operacional associada a uma arquitetura distribuída. Em contrapartida, os módulos não podem ser implantados nem escalados de forma independente, o que limita a capacidade de responder isoladamente a aumentos de carga numa área específica do sistema.

Embora os materiais fornecidos recomendem evitar o monólito modular quando existem exigências elevadas de elasticidade, esta característica deve ser ponderada à luz do contexto concreto do Lisboa Pedala. Ao contrário de plataformas como a Ticketmaster, que podem enfrentar picos muito elevados de procura durante a venda de bilhetes para eventos populares, o Lisboa Pedala tem uma dimensão operacional prevista de 1 500 bicicletas distribuídas por 180 estações. Mesmo considerando possíveis aumentos da procura durante eventos como os Santos Populares, não se considera, à partida, que estes picos justifiquem, por si só, a adoção de uma arquitetura distribuída. Esta avaliação constitui, no entanto, uma hipótese de projeto que deverá ser validada através de estimativas de carga e, posteriormente, de dados de utilização reais.

Deste modo, embora a elasticidade continue a ser uma característica relevante, a escala prevista do sistema não constitui, nesta fase, um motivo suficiente para rejeitar o monólito modular. A decisão deverá ser reavaliada caso a utilização real revele picos de carga, requisitos de disponibilidade ou necessidades de escalabilidade que a arquitetura adotada não consiga satisfazer de forma adequada.

Conclui-se, assim, que o monólito modular representa um compromisso adequado às necessidades atualmente conhecidas do Lisboa Pedala, privilegiando a coesão funcional e a localidade das alterações ao domínio, sem introduzir antecipadamente os custos operacionais de uma arquitetura distribuída. A concretização destas vantagens depende, contudo, da definição clara das fronteiras entre módulos, da aplicação de regras de encapsulamento e da utilização de testes arquiteturais que permitam detetar e prevenir a degradação da modularidade ao longo do desenvolvimento.

### 2.3. Topologia dos módulos de domínio

Com base nos componentes definidos em `02-componentes.md`, propõe-se a seguinte organização de topo:

```text
lisboa_pedala/
├── dock_access/
├── trip_lifecycle/
├── tariff_billing/
├── station_availability/
├── sensor_ingestion/
├── fleet_redistribution/
├── maintenance_orders/
└── reporting_audit/
```

As responsabilidades são as seguintes:

| Módulo | Responsabilidade |
|---|---|
| `dock_access` | Validar pedidos de desbloqueio e devolução e comunicar com as docas físicas. Inclui o tratamento dos eventos recebidos após períodos de desconexão. |
| `trip_lifecycle` | Gerir o início, a duração e o fecho das viagens, incluindo os dados necessários para consultar o histórico de viagens. |
| `tariff_billing` | Calcular tarifas, processar pagamentos, aplicar regras tarifárias e gerir a emissão e comunicação de faturas. |
| `station_availability` | Manter a representação atualizada da ocupação das estações e da disponibilidade de bicicletas e docas. |
| `sensor_ingestion` | Receber e processar telemetria proveniente das bicicletas e dos dispositivos associados. |
| `fleet_redistribution` | Calcular propostas de redistribuição de bicicletas e apoiar a consulta e execução das rotas das carrinhas. |
| `maintenance_orders` | Criar, acompanhar e concluir ordens de manutenção, incluindo as originadas por reportes de avarias ou anomalias detetadas. |
| `reporting_audit` | Agregar informação operacional para produzir relatórios e apoiar a auditoria. |


### 3.1. Regras de Dependência

As dependências entre os módulos de domínio obedecem às seguintes regras restritivas (*Allowed-to-Use*):

| Módulo Origem | Módulos Permitidos (*Allowed-to-Use*) | Módulos Proibidos |
| :--- | :--- | :--- |
| `dock_access` | `trip_lifecycle`, `station_availability` | `tariff_billing`, `sensor_ingestion` |
| `trip_lifecycle` | `tariff_billing`, `station_availability` | `dock_access`, `sensor_ingestion` |
| `tariff_billing` | *(Nenhum - módulo folha/orquestrado)* | Todos os outros |
| `station_availability` | *(Nenhum)* | Todos os outros |
| `sensor_ingestion` | `station_availability` | `trip_lifecycle`, `tariff_billing` |
| `fleet_redistribution` | `station_availability` | `trip_lifecycle`, `tariff_billing` |
| `maintenance_orders` | `station_availability` | Módulos de negócio crítico |
| `reporting_audit` | Todos os módulos de leitura (em modo de agregação) | Modificação direta de estado |

Estas regras serão transpostas para testes arquiteturais automáticos (*fitness functions* na pasta `tests/architecture/`) para impedir violações no código-fonte[cite: 7].




```text
dock_access/
├── DockAccessFacade.java · API pública     # Fachada que expõe apenas os contratos permitidos para os outros módulos
│   ├── unlockDock()                        # Destranca uma doca específica para levantamento de bicicleta
│   ├── confirmDocking()                    # Confirma o encaixe bem-sucedido de uma bicicleta numa doca
│   └── getDockStatus()                     # Consulta o estado atual de uma doca (livre/ocupada/avariada)
├── presentation/                           # Camada de Apresentação
│   └── DockController.java                 # Controlador REST que recebe os pedidos da App móvel nas estações
├── business/                               # Camada de Lógica de Negócio (Domínio)
│   ├── DockService.java                    # Gere o estado geral e ciclo de vida das docas
│   ├── UnlockingService.java               # Lógica específica de validação de tokens e destrancagem segura
│   ├── DockingService.java                 # Lógica de confirmação de encaixe e leitura de sensores da doca
│   └── BufferService.java                  # Gere o buffer temporário ou reentras de comunicação offline
└── persistence/                            # Camada de Persistência de Dados
    └── DockRepository.java                 # Comunica com a base de dados para atualizar o estado físico da doca

station_availability/
├── StationAvailabilityFacade.java · API pública # Fachada que expõe apenas os contratos permitidos para os outros módulos
│   ├── getStationOccupancy()               # Devolve quantas docas livres (sem bicicleta) e ocupadas (com bicicleta) existem
│   └── updateAvailability()                # Atualiza o estado da estação quando uma doca muda de livre para ocupada (ou vice-versa)
├── presentation/                           # Camada de Apresentação
│   └── StationController.java              # Expõe os endpoints REST para o frontend consultar a ocupação do mapa em tempo real
├── business/                               # Camada de Lógica de Negócio (Domínio)
│   ├── AvailabilityService.java            # Calcula a ocupação atual: doca livre = sem bicicleta; doca ocupada = com bicicleta
│   └── StationService.java                 # Gere os dados de localização e capacidade máxima das 180 estações
└── persistence/                            # Camada de Persistência de Dados
    ├── StationRepository.java              # Comunica com a base de dados para ler e atualizar o estado das estações
    └── schema.sql (1 tabela)               # Tabela que guarda a identidade da estação, localização e contadores consolidados

sensor_ingestion/
├── SensorIngestionFacade.java · API pública      # Ponto de entrada para outros módulos consultarem dados em tempo real
│   └── getLatestTelemetry()
├── presentation/                                 # Camada de Apresentação / Ingestão de Rede
│   └── TelemetryController.java                  # Endpoint / Webhook para receber os pings das 1.050 bikes (~35 req/s)
├── business/                                     # Camada de Lógica de Negócio (Processamento e IA/Regras)
│   ├── TelemetryParserService.java               # Descodifica o pacote binário/JSON bruto (GPS, bateria, velocidade)
│   ├── LostBikeDetectorService.java              # ⭐ Deteta falhas prolongadas de sinal (> 2h sem ping fora da doca)
│   ├── MalfunctionService.java                   # ⭐ Avalia componentes e deteta avarias mecânicas/elétricas
│   ├── MalfunctionEmitter.java                   # ⭐ Dispara o alerta para a fachada de manutenção de forma assíncrona
│   └── SignalDispatcherService.java              # Encaminha os dados limpos para os módulos interessados
└── persistence/                                  # Camada de Persistência de Dados
    └── TelemetryLogRepository.java               # Guarda histórico de telemetria e rastreio de percurso

fleet_redistribution/
├── Fleet_redistribution.java (ou Pacote Fachada) · API pública
│   └── getActiveRoutes()      
├── presentation/
│   └── RouteReader.java           # Controlador REST / Ponto de entrada
├── business/
│   ├── CalculateRoutes.java             # Lógica central do algoritmo de otimização de frota
│   └── RoutingClientService.java       # Cena para comunicar com mapa ns se é preciso
└── persistence/
    └── RedistributionRepository.java # Acesso à base de dados

maintenance_orders/
├── MaintenanceFacade.java · API pública     # Fachada que expõe os contratos permitidos para os outros módulos
│   ├── createMaintenanceOrder()            # Permite reportar uma avaria (numa doca ou bicicleta) e abrir ordem de reparação
│   └── getPendingOrders()                  # Devolve a lista de manutenções pendentes para a equipa técnica
├── presentation/                           # Camada de Apresentação
│   └── MaintenanceController.java          # Controlador REST para a app/painel dos técnicos de manutenção em campo
├── business/                               # Camada de Lógica de Negócio (Domínio)
│   ├── TicketService.java                  # Gere o ciclo de vida da avaria (aberta, em progresso, resolvida)
│   └── TechnicianAssignmentService.java    # Lógica de atribuição de ordens de serviço aos técnicos disponíveis
└── persistence/                            # Camada de Persistência de Dados
    ├── MaintenanceRepository.java          # Comunica com a base de dados para gravar o registo de avarias
    └── schema.sql (1 tabela)               # Tabela que guarda os detalhes das ordens de manutenção, estado e histórico
```
