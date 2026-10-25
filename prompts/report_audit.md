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
- **Decisão:** Aceite integralmente.
- **Justificação:** A implementação gerada foi validada com sucesso através da suite de testes automatizados em [`ReportingAuditTest.java`](file:///c:/Users/Diogo%20Correia/Documents/FCUL_Mestrado/DesignSoftware/Trabalho1/TP1_DS/src/test/java/pt/ulisboa/lisboapedala/reporting_audit/ReportingAuditTest.java) (4 testes com 100% de sucesso) e pelos testes arquiteturais de ArchUnit:
  1. **Relatório de Utilização por Freguesia (US-4) (`shouldGenerateParishUsageReportSuccessfully`):** Validação da agregação correta de viagens concluídas por freguesias de origem e destino (Avenidas Novas, Alvalade, Belém), contabilizando durações e poupança de CO₂ de forma anónima e sem dados pessoais (ADR-006 / RGPD).
  2. **Relatório Mensal Consolidado da Câmara (Requisito 7) (`shouldGenerateAndPersistMonthlyMunicipalReport`):** Confirmação da geração de relatórios com totais de viagens, distâncias estimadas e CO₂ evitado, persistindo duradouramente na tabela `reporting_audit_monthly_reports` para auditoria.
  3. **Portal de Dados Abertos (`shouldGenerateStationOpenData`):** Publicação de métricas de ocupação em percentagem por estação para o concelho de Lisboa.
  4. **Camada de Apresentação (`controllerShouldExposeFacadeCorrectly`):** Integração do `ReportController` comunicando estritamente através da `ReportingAuditFacade`.
  5. **Conformidade Global de Arquitetura:** Execução de `mvn test` totalizando **32 testes com 0 falhas**, confirmando o cumprimento da regra de *Allowed-to-Use* 8 do ArchUnit (`reporting_audit` apenas consome `trip_lifecycle` e `station_availability` em modo leitura).

## 3. Deteção de Violações Arquiteturais
- **Houve violação da arquitetura?:** Não.
- **Quem a detetou?:** Não aplicável.
- **Descrição da ocorrência:** O código gerado respeitou rigorosamente os limites arquiteturais do Monólito Modular: não acedeu a pacotes internos (`.business` ou `.persistence`) de outros módulos nem realizou queries SQL diretas a tabelas alheias. Toda a informação foi obtida através dos métodos públicos `TripLifecycleFacade.getCompletedTripsBetween()` e `StationAvailabilityFacade.getAllStations()`, mantendo o ArchUnit 100% verde.

