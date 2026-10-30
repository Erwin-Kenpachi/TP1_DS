# ARCHITECTURE.md — Lisboa Pedala

> **Objetivo:** referência curta e verificável para a equipa e para ferramentas de IA. Em caso de conflito, prevalecem os ADR aceites e os contratos públicos dos módulos.

## 1. Estilo arquitetural e organização

O **Lisboa Pedala** é um **monólito modular organizado por domínio**, implementado em **Java 21** e com persistência em **SQLite**. É uma única unidade de implantação; os módulos são fronteiras lógicas dentro da mesma aplicação, não serviços implantados separadamente. A interface disponibilizada é REST API e/ou CLI, sem interface gráfica no âmbito atual. As integrações com docas físicas, pagamentos e Autoridade Tributária são simuladas através de adaptadores.

O pacote-base Java é `pt.ulisboa.lisboapedala`. Cada módulo tem um pacote próprio e expõe uma fachada pública; as classes de domínio, serviços e persistência são internas. A organização interna pode usar `presentation`, `business` e `persistence`, mas estas camadas não substituem as fronteiras entre módulos.

## 2. Componentes e responsabilidades

Os oito componentes centrais são complementados por `history_reader`, um componente de leitura separado. Assim, a arquitetura descrita contém **nove componentes lógicos**.

| Componente / pacote | Responsabilidade (uma frase) | API pública |
|---|---|---|
| `dock_access` | Valida pedidos de acesso e coordena o trancamento/destrancamento das docas, incluindo a receção e sincronização de eventos offline. | `DockAccessFacade` |
| `trip_lifecycle` | Gere o início, a duração, os alertas e o encerramento idempotente das viagens. | `TripLifecycleFacade` |
| `tariff_billing` | Calcula tarifas, processa cobranças, emite faturas e integra com o adaptador da Autoridade Tributária. | `TariffBillingFacade` |
| `station_availability` | Disponibiliza o estado atualizado das bicicletas e das docas livres por estação. | `StationAvailabilityFacade` |
| `sensor_ingestion` | Recebe e processa telemetria das bicicletas elétricas e identifica anomalias técnicas. | `SensorIngestionFacade` |
| `maintenance_orders` | Gere as ordens de manutenção preventiva e corretiva e o respetivo acompanhamento pelos técnicos. | `MaintenanceOrdersFacade` |
| `fleet_redistribution` | Identifica desequilíbrios entre estações e propõe redistribuições e rotas para as carrinhas. | `FleetRedistributionFacade` |
| `reporting_audit` | Produz indicadores operacionais e relatórios municipais, incluindo estimativas de CO₂ evitado e dados abertos. | `ReportingAuditFacade` |
| `history_reader` | Permite ao utilizador consultar o histórico de viagens e as respetivas faturas, lendo os dados através dos módulos responsáveis. | `HistoryReaderFacade` |

**Limite de responsabilidade do `history_reader`:** é um componente de consulta,. Obtém os dados através de `TripLifecycleFacade` e `TariffBillingFacade`; não calcula tarifas, não emite faturas e não altera o ciclo de vida das viagens.

## 3. Regras `allowed-to-use`

As regras seguintes são obrigatórias. Um módulo só pode depender dos módulos indicados e apenas através das respetivas fachadas públicas. As dependências são unidirecionais; não são permitidos ciclos.

| Módulo de origem | Pode usar |
|---|---|
| `dock_access` | `trip_lifecycle`, `station_availability` |
| `trip_lifecycle` | `tariff_billing`, `station_availability` |
| `station_availability` | Nenhum outro módulo de domínio |
| `maintenance_orders` | `sensor_ingestion` |
| `fleet_redistribution` | `station_availability` |
| `reporting_audit` | `trip_lifecycle`, `station_availability` (consultas de leitura) |
| `history_reader` | `trip_lifecycle`, `tariff_billing` (consultas de leitura) |
| `tariff_billing` | Nenhum outro módulo de domínio |
| `sensor_ingestion` | Nenhum outro módulo de domínio |

Regras verificáveis:

1. **Só fachadas públicas:** um módulo não pode importar classes internas (`presentation`, `business`, `persistence`, repositórios, entidades ou serviços) de outro módulo.
2. **Sem dependências não declaradas:** qualquer dependência entre módulos que não conste da tabela é proibida.
3. **Sem ciclos:** o grafo de dependências tem de permanecer acíclico.
4. **Sem acesso cruzado à base de dados:** um módulo não pode consultar, alterar ou criar tabelas pertencentes a outro módulo.
5. **Sem chaves estrangeiras entre módulos:** referências a dados de outro módulo são guardadas como identificadores e validadas através dos contratos apropriados.
6. **Verificação automática:** testes arquiteturais em `tests/architecture/` (por exemplo, com ArchUnit) devem falhar quando alguma destas regras for violada.

## 4. Três características arquiteturais prioritárias

### 1. Robustez — operação perante falhas de rede

- `dock_access` tem de registar de forma duradoura os eventos offline recebidos da doca, com capacidade prevista para até 24 horas.
- Cada evento tem um identificador único; o reenvio do mesmo evento não pode criar uma segunda viagem nem duplicar uma cobrança.
- A devolução física deve poder ser registada localmente quando não existe rede; o desbloqueio exige ligação ao backend para validação.

### 2. Elasticidade a pedido — picos de utilização

- As operações de consulta de disponibilidade devem evitar bloquear as operações transacionais de início e fim de viagem.
- As consultas devem ser limitadas ao necessário; não podem executar trabalho analítico pesado no caminho crítico de desbloqueio ou devolução.
- A telemetria periódica deve ser processada sem introduzir dependências síncronas desnecessárias no fluxo de uma viagem.

### 3. Disponibilidade — concluir operações essenciais

- O fluxo de desbloqueio deve manter o número de dependências em tempo de execução tão baixo quanto possível e tratar explicitamente falhas de comunicação.
- Falhas de serviços externos devem resultar em erros controlados, timeouts e, quando aplicável, tentativas limitadas; nunca devem deixar uma viagem num estado intermédio sem registo recuperável.
- A disponibilidade não autoriza a contornar autenticação, validação de saldo ou regras antifraude.

## 5. Acoplamento temporal e transações

As chamadas entre módulos são síncronas e realizadas em memória (*in-process*). A ordem seguinte é obrigatória para o fluxo principal:

1. **Início da viagem:** `dock_access` valida o pedido e chama `trip_lifecycle` para registar o início; só depois de a autorização necessária estar confirmada envia o comando de desbloqueio. Após confirmação física, atualiza-se `station_availability`. A implementação tem de definir e testar a recuperação de falhas entre estes passos.
2. **Devolução:** a doca confirma o encaixe e o trancamento; `dock_access` entrega o evento identificado a `trip_lifecycle`; este fecha a viagem de forma idempotente e regista o estado final.
3. **Cobrança:** no fecho da viagem, `trip_lifecycle` chama `tariff_billing` para calcular e registar a cobrança e a fatura. A mesma viagem/evento não pode originar uma segunda cobrança.
4. **Sincronização offline:** quando a ligação regressa, os eventos pendentes são enviados com os seus identificadores originais; o backend confirma reenvios já processados sem repetir efeitos.

**Unidade de trabalho:** cada alteração local de estado e os registos necessários para a sua consistência devem ser confirmados numa transação SQLite local. Não se deve manter uma transação de base de dados aberta durante uma chamada de rede a uma doca, gateway de pagamento ou Autoridade Tributária. Como estas chamadas externas não participam na transação SQLite, devem ser tratadas com estados explícitos, timeouts e operações idempotentes. A ordem e a recuperação dos passos têm de ser cobertas por testes.

## 6. Convenções de implementação e limites para a IA

- **Tecnologia:** Java 21; SQLite; REST API e/ou CLI. Não introduzir outra linguagem, base de dados, infraestrutura distribuída ou interface gráfica sem decisão explícita da equipa.
- **Nomes:** usar `snake_case` nos nomes dos módulos e tabelas; usar `UpperCamelCase` para classes Java e nomes `*Facade` para as APIs públicas.
- **Persistência:** cada módulo mantém o seu `schema.sql` em `src/<modulo>/persistence/`; todas as tabelas têm prefixo do módulo (por exemplo, `trip_lifecycle_trip`). Cada módulo é proprietário das suas tabelas.
- **Dados entre módulos:** usar contratos/DTOs públicos; nunca partilhar entidades de persistência nem aceder diretamente aos repositórios de outro módulo.
- **Integrações externas:** usar interfaces adaptadoras e mocks/simuladores no âmbito do projeto; não chamar serviços reais nem incluir credenciais.
- **Testes:** cobrir regras de negócio, idempotência, recuperação de eventos offline e regras arquiteturais.

**A IA não pode:**

1. Importar ou usar classes internas de outro módulo, contornar uma fachada pública ou criar dependências não autorizadas.
2. Criar ciclos entre módulos, consultas SQL cruzadas ou chaves estrangeiras entre módulos.
3. Colocar lógica de negócio em controladores REST/CLI ou duplicar regras pertencentes a outro módulo.
4. Fazer do `history_reader` proprietário de viagens/faturas, adicionar-lhe persistência própria ou permitir-lhe alterar esses dados.
5. Alterar a ordem transacional, remover a idempotência ou repetir cobranças em caso de reenvio.
6. Introduzir microsserviços, interfaces gráficas, dependências externas ou integrações reais sem aprovação explícita.

## 7. ADRs relevantes

| Componente / decisão | ADR(s) | Decisão documentada |
|---|---|---|
| Decomposição histórica | `ADR-001` (substituído) | Regista a decomposição inicial fina e os motivos da sua substituição. |
| Todos os componentes | `ADR-002` | Consolidação em domínios coesos; inclui o papel de `history_reader`. |
| Estilo e implantação | `ADR-003` | Monólito modular, particionamento por domínio e uma unidade de implantação. |
| `dock_access`, `trip_lifecycle`, `tariff_billing` | `ADR-004` | Operação offline das docas, sincronização e idempotência. |
| Todas as fronteiras entre módulos | `ADR-005` | Fachadas públicas, matriz `allowed-to-use` e testes arquiteturais automáticos. |
| Todos os módulos com persistência; em especial `tariff_billing` e `history_reader` | `ADR-006` | Isolamento lógico dos dados, ausência de acesso SQL cruzado e retenção fiscal. |

> **Nota de consistência:** a lista de permissões deste documento é a regra normativa para a implementação. Se o grafo de dependências de `03-particionamento-e-vistas.md` ou algum ADR divergir desta tabela, a equipa deve resolver a divergência e atualizar os documentos antes de alterar o código.
