
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

