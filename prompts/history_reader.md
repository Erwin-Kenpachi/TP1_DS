

### Prompt 5
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

## 2. Resultado da Geração
- **Resultado:** Positivo, correu bem sem problemas.
- **Justificação:** A ferramenta gerou o módulo `history_reader` respeitando estritamente o princípio de componente de leitura puro. O código utiliza exclusivamente as fachadas públicas de `trip_lifecycle` e `tariff_billing`, emprega DTOs imutáveis (`UserTripHistoryDTO`, `TripReceiptDetailsDTO`) e não introduz qualquer esquema de persistência próprio, cumprindo na íntegra o ADR-002, ADR-005 e ADR-006.

## 3. Deteção de Violações Arquiteturais
- **Violações Detetadas:** Nenhuma.
- **Mecanismo de Verificação:** A suite de testes arquiteturais (ArchUnit / `ModularMonolithArchitectureTest`) correu com sucesso e validou que o módulo `history_reader` cumpre a matriz *Allowed-to-Use* (dependendo apenas de `trip_lifecycle` e `tariff_billing` para efeitos de leitura) sem efetuar acessos diretos a pacotes internos ou bases de dados externas.
