### Prompt 8 (Esqueleto Estrutural / Bare Bones)
[Contexto fornecido: ARCHITECTURE.md, adr/ADR-002.md, adr/ADR-003.md, adr/ADR-005.md, adr/ADR-006.md, docs/03-particionamento-e-vistas.md]

Por favor, implementa apenas a estrutura base (*bare bones* e pastas essenciais) do módulo de domínio `fleet_redistribution` em Java 21 para o sistema Lisboa Pedala, sem necessidade de implementar a lógica de negócio detalhada de otimização de rotas (US-5).

Deves respeitar rigorosamente as diretrizes arquiteturais dos documentos de especificação e a vista de módulos de `docs/03-particionamento-e-vistas.md`, cumprindo a seguinte organização estrutural:

1. Estrutura de Pacotes e Componentes Internos (em `pt.ulisboa.lisboapedala.fleet_redistribution`):
   - **Fachada Pública (`FleetRedistributionFacade.java`)**: Ponto de contacto estável que expõe os contratos essenciais:
     * `List<Object> getActiveRoutes()`: Método básico de consulta de rotas.
   - **Camada de Apresentação (`presentation/`)**:
     * `RouteReader.java`: Esqueleto inicial de controlador/ponto de entrada.
   - **Camada de Lógica de Negócio (`business/`)**:
     * Classes vazias ou com esqueletos para futuras regras de cálculo de desequilíbrios e consulta síncrona à fachada de disponibilidade.
   - **Camada de Persistência (`persistence/`)**:
     * `RedistributionRepository.java` e `schema.sql`: Definição da tabela privada do módulo num esquema SQL isolado.

2. Regras Estritas de Dependência e Allowed-to-Use (ADR-005 e ARCHITECTURE.md):
   - **Direção do Acoplamento:** O módulo `fleet_redistribution` PODE APENAS depender de `station_availability`, consumindo exclusivamente a sua fachada pública (`StationAvailabilityFacade`).
   - **Isolamento de Fachadas:** É expressamente proibido importar ou aceder a classes internas (`.business` ou `.persistence`) de outros módulos ou efetuar queries SQL diretas a tabelas externas.

3. Verificação Arquitetural Automática:
   - A estrutura deve passar nas *fitness functions* com ArchUnit (`ModularMonolithArchitectureTest`), validando a ausência de ciclos e o cumprimento das regras *Allowed-to-Use*.

---

## 2. Resultado da Geração
- **Decisão:** Aceite integralmente.
- **Justificação:** A estrutura base (*bare bones*) do módulo `fleet_redistribution` foi implementada estritamente de acordo com as diretrizes de ARCHITECTURE.md, ADR-002, ADR-003, ADR-005, ADR-006 e `docs/03-particionamento-e-vistas.md`:
  1. **Fachada Pública (`FleetRedistributionFacade.java`):** Expõe o contrato público com `getActiveRoutes(): List<Object>` e orquestra os componentes internos encapsulados sem vazar entidades de domínio.
  2. **Camada de Apresentação (`presentation/RouteReader.java`):** Ponto de entrada para as equipas de carrinhas de redistribuição, comunicando exclusivamente com a fachada pública do módulo.
  3. **Camada de Lógica de Negócio (`business/CalculateRoutes.java`, `RoutingClientService.java`):** Esqueleto de regras de desequilíbrio e planeamento de rotas, consumindo síncronamente a fachada pública `StationAvailabilityFacade` sem invocar classes internas de outros módulos.
  4. **Camada de Persistência (`persistence/RedistributionRepository.java`, `schema.sql`):** Tabela isolada `fleet_redistribution_routes` com prefixo de módulo em SQLite privado, sem chaves estrangeiras nem dependências cruzadas.
  5. **Verificação Automatizada:** Suite de testes com 38 testes executados com 100% de sucesso (`mvn test`), incluindo testes unitários do módulo (`FleetRedistributionTest`) e as fitness functions de ArchUnit (`ModularMonolithArchitectureTest`).

## 3. Deteção de Violações Arquiteturais
- **Houve violação da arquitetura?:** Não.
- **Descrição da ocorrência:** Nenhuma violação ocorreu. O módulo respeita estritamente a matriz *Allowed-to-Use* (dependendo exclusivamente da API pública de `station_availability`), cumpre o encapsulamento estrito (sem acesso a pacotes `.business` ou `.persistence` de outros módulos) e preserva o grafo de dependências livre de ciclos, conforme validado pela regra 10 do ArchUnit (`fleet_redistribution_allowed_dependencies`), `no_cycles_between_modules` e `no_access_to_internal_packages_of_other_modules`.
