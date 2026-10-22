# ADR-002: Consolidação dos Componentes Lógicos em Domínios Coesos de Granularidade Média

* **Estado:** **Aceite** (Substitui e revoga [ADR-001](./ADR-001-decomposicao-granularidade-fina.md))
* **Data:** Semana 2 (Iteração de Reestruturação)
* **Contexto dos Documentos:** [docs/02-componentes.md](../02-componentes.md#L35-L48) e [docs/03-particionamento-e-vistas.md](../03-particionamento-e-vistas.md#L140-L151)

## Contexto
Durante a análise dos acoplamentos aferentes e eferentes e do cálculo da métrica de instabilidade ((I = Ce / (Ca + Ce))), a equipa concluiu que a hiperfragmentação identificada no ADR-001 dificultava a manutenção do sistema e prejudicava a coesão das responsabilidades funcionais. Conforme descrito na secção Reestruturação de 02-componentes.md, tornou-se necessário rever a decomposição inicial, agrupando componentes demasiado específicos em componentes de granularidade intermédia, organizados de acordo com as respetivas responsabilidades funcionais e os domínios do sistema.

## Decisão
Substituir a decomposição fina por uma matriz consolidada de **8 componentes lógicos centrais de granularidade média** (organizados por domínios coesos), com uma responsabilidade única clara por componente:
1. `dock_access`: Validação e execução de desbloqueios e encaixes físicos nas docas, e gestão do buffer de sincronização *offline*.
2. `trip_lifecycle`: Controlo integral do ciclo de vida das viagens (início, duração, alertas de 45 min, fecho e garantia de idempotência).
3. `tariff_billing`: Cálculo de tarifas (regra dos 45 min grátis para passes anuais e blocos de 15 min), pagamentos e faturação com comunicação à AT.
4. `station_availability`: Unificação do mapa de disponibilidade em tempo real, agregando tanto a consulta de bicicletas livres como a gestão de docas vagas por estação (absorvendo os antigos `bike_availability` e `station_availability` periféricos).
5. `sensor_ingestion`: Ingestão e processamento da telemetria de 1 050 bicicletas elétricas a cada 30 segundos, com deteção de anomalias técnicas.
6. `maintenance_orders`: Ciclo de vida e gestão de ordens de manutenção preventiva e corretiva para técnicos.
7. `fleet_redistribution`: Cálculo de desvios de frota e geração de rotas sugeridas para as 6 carrinhas de redistribuição.
8. `reporting_audit`: Agregação mensal de indicadores operacionais por freguesia, estimativa de $CO_2$ evitado e exportação de dados abertos para a autarquia.
9. `history_reader `: Responsável por permitir ao utilizador consultar o seu histórico de viagens e as respetivas faturas. Para isso, consulta os componentes trip_lifecycle e tariff_billing, sem assumir a responsabilidade pela gestão do ciclo de vida das viagens, pelo cálculo das tarifas ou pela faturação.


## Consequências
* **Positivas:**
  * **Equilíbrio saudável de acoplamento:** Os componentes transacionais centrais atingem estabilidade comprovada (`sensor_ingestion` $I=0.0$, `trip_lifecycle` $I=0.25$, `station_availability` $I=0.33$), enquanto os módulos periféricos mantêm máxima flexibilidade ($I=1.0$);
  * **Eliminação de chamadas redundantes:** Redução substancial do tráfego inter-módulos e facilidade na aplicação de transações atómicas locais;
  * **Adequação ao tamanho da equipa:** 8 componentes lógicos podem ser facilmente distribuídos pelos 6 membros da equipa através do `CODEOWNERS`;
  * **Coesão Funcional Alta:** Elimina o risco de dispersão mantendo total conformidade com o princípio da responsabilidade única (SRP).
* **Negativas:**
  * Cada componente agrupa um conjunto ligeiramente mais amplo de classes, exigindo organização interna clara em camadas (`presentation`, `business`, `persistence`).

## Alternativas Rejeitadas
* **Manutenção da Granularidade Fina (ADR-001):** Rejeitada pelos motivos detalhados no ADR-001 (complexidade descontrolada e acoplamento eferente em teia de aranha).
