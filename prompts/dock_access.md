

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
