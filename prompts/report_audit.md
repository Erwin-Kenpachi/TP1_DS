### Prompt 8

[Contexto fornecido: ARCHITECTURE.md, adr/ADR-002.md, adr/ADR-003.md, adr/ADR-005.md, adr/ADR-006.md, docs/01-caracteristicas.md, docs/02-componentes.md, docs/03-particionamento-e-vistas.md]

Por favor, implementa exclusivamente o módulo de domínio `reporting_audit` em Java 21 para o sistema Lisboa Pedala, suportando a agregação de dados operacionais, o cálculo de emissões de CO₂ evitado e a emissão de relatórios municipais de utilização por freguesia para Gestores Municipais (US-4).

Deves respeitar rigorosamente as diretrizes arquiteturais definidas em ARCHITECTURE.md e nos ADRs referidos, bem como a vista de módulos de docs/03-particionamento-e-vistas.md, com a seguinte estrutura de componentes:

1. Estrutura de Pacotes e Componentes Internos (em pt.ulisboa.lisboapedala.reporting_audit):
   - Fachada Pública (ReportingAuditFacade.java): Ponto de entrada estável e exclusivo para operações de auditoria e relatórios municipais:
     * ParishUsageReportDTO getParishUsageReport(): Relatório de utilização agregada por freguesia de Lisboa (US-4);
     * MonthlyMunicipalReportDTO generateMonthlyReport(int year, int month): Relatório mensal oficial consolidado (viagens totais, freguesias, CO₂ evitado face ao transporte motorizado);
     * StationOpenDataDTO getStationOpenData(): Métricas consolidadas e anónimas de ocupação de estações para o portal de dados abertos da Câmara Municipal.
   - DTOs Públicos: ParishUsageReportDTO.java, MonthlyMunicipalReportDTO.java, StationOpenDataDTO.java.
   - Camada de Apresentação (presentation/ReportController.java): Endpoints REST/CLI dedicados aos Gestores Municipais (ex: GET /api/v1/admin/reports/parishes, GET /api/v1/admin/reports/monthly).
   - Camada de Lógica de Negócio (business/):
     * ReportGeneratorService.java: Orquestra a extração e consolidação analítica, consumindo síncronamente em modo de leitura as fachadas públicas de trip_lifecycle e station_availability;
     * CarbonSavingsCalculator.java: Aplica a fórmula do CO₂ evitado com base na distância/tempo estimado das viagens concluídas em relação ao transporte individual automóvel motorizado;
     * ParishUsageAggregator.java: Agrupa e totaliza viagens por freguesia (origem e destino geográfico em Lisboa);
     * MunicipalReport.java: Entidade de domínio interna representativa do relatório mensal gerado.
   - Camada de Persistência (persistence/):
     * ReportRepository.java: Repositório para gravação e histórico dos relatórios mensais gerados para fins de auditoria e conformidade municipal;
     * schema.sql: Definição da tabela privada `reporting_audit_monthly_reports` em SQLite exclusivo do módulo.

2. Regras Estritas de Dependência e Allowed-to-Use (ADR-005 e ARCHITECTURE.md Secção 3):
   - Módulo Analítico Periférico: O `reporting_audit` apresenta instabilidade máxima ($I = 1.0$, $Ca = 0$, $Ce = 2$), dependendo unicamente de leitura de outros módulos.
   - Dependências Permitidas: PODE APENAS consultar as fachadas públicas de `trip_lifecycle` (para histórico de viagens concluídas) e `station_availability` (para mapeamento geográfico de estações por freguesia).
   - Modo Exclusivo de Leitura: É expressamente PROIBIDO ao `reporting_audit` efetuar alterações de estado (escritas, cancelamentos ou finalizações de viagens) em qualquer outro módulo.
   - Dependências Proibidas: Proibido importar ou depender de `dock_access`, `tariff_billing`, `sensor_ingestion`, `maintenance_orders`, `fleet_redistribution` ou `history_reader`.
   - Encapsulamento Estrito: Proibido aceder a classes internas (`.business` ou `.persistence`) ou tabelas SQL de outros módulos.

3. Restrições de Privacidade, RGPD e Desempenho (docs/01-caracteristicas.md e ADR-006):
   - **Privacidade e RGPD:** Conforme o enunciado ("O percurso de cada viagem é dado pessoal"), os relatórios produzidos para a Câmara Municipal e para dados abertos são estritamente estatísticos e agregados por freguesia. É proibido expor nomes, NIFs, IDs de utilizadores ou percursos individuais nos relatórios.
   - **Desempenho e Elasticidade:** As consultas analíticas e cálculos de CO₂ não podem bloquear nem degradar o caminho crítico transacional de desbloqueio e devolução de viagens.

4. Verificação Arquitetural Automática:
   - A implementação tem de passar integralmente nas fitness functions com ArchUnit (`ModularMonolithArchitectureTest.reporting_audit_allowed_dependencies`, `no_cycles_between_modules` e `no_access_to_internal_packages_of_other_modules`).

---

## 2. Resultado da Geração
- **Decisão:** [Aceite / Rejeitado / Aceite com Correções]
- **Justificação:** [Exemplo: A ferramenta gerou o módulo reporting_audit suportando a geração de relatórios de utilização por freguesia (US-4) e métricas de CO₂ evitado, respeitando a integração exclusiva por leitura via TripLifecycleFacade e StationAvailabilityFacade, com persistência isolada no schema.sql.]

## 3. Deteção de Violações Arquiteturais
- **Houve violação da arquitetura?:** [Sim / Não]
- **Quem a detetou?:** [Fitness Function (ArchUnit) / Revisão Humana / Ninguém até mais tarde]
- **Descrição da ocorrência:** [Exemplo: Se a IA tentou fazer uma query direta SQL SELECT * FROM trips em vez de chamar TripLifecycleFacade.getTripsByPeriod(), a violação foi barrada pela revisão humana ou pelas regras de isolamento de persistência do monólito modular.]
