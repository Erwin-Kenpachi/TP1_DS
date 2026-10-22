# ADR-004: Estratégia de Resiliência das Docas com Operação Offline e Idempotência Estrita

* **Estado:** **Aceite**
* **Data:** Semana 2
* **Contexto dos Documentos:** [docs/01-caracteristicas.md](../01-caracteristicas.md#L16) e [docs/03-particionamento-e-vistas.md](../03-particionamento-e-vistas.md#L343)

## Contexto
As 180 estações de docas comunicam com o backend através de rede móvel. Existem zonas geográficas em Lisboa onde a cobertura de rede móvel sofre falhas frequentes ou intermitentes. O contrato de concessão municipal é estrito: impõe penalizações financeiras (multa), por cada hora em que mais de 10% das estações estejam inoperacionais (não conseguem desbloquear bicicletas). A característica arquitetural eleita em primeiro lugar foi a **Robustez** (`01-caracteristicas.md`). O utilizador não pode ser impedido de concluir uma viagem nem ser cobrado indevidamente se a estação estiver sem conectividade no momento da devolução.

## Decisão
Implementar um padrão de operação desconectada baseado em *Store-and-Forward* local no hardware das estações acoplado a **Idempotência Estrita** no backend:
1. **Devolução Offline Garantida:** O controlador físico da doca valida mecanicamente o encaixe, tranca o fecho e armazena localmente o evento de fecho em memória persistente (com capacidade para reter eventos até 24 horas);
2. **Desbloqueio Requer Conectividade:** O desbloqueio de bicicletas requer comunicação online com o backend (para autenticação de token, validação de saldo do utilizador e prevenção de fraude/furto);
3. **Sincronização com Idempotência Estrita:** Assim que a rede móvel for restabelecida, a doca sincroniza em lote os eventos pendentes com o módulo `dock_access`. Os eventos contêm identificadores únicos de transação da doca (`dock_tx_id` / `event_id`);
4. Os módulos `trip_lifecycle` e `tariff_billing` processam a devolução de forma idempotente: se o evento já tiver sido processado, o backend confirma a receção sem alterar o estado da viagem nem duplicar a faturação.

## Consequências
* **Positivas:**
  * **Tolerância a Falhas e Resiliência Operacional:** 100% de integridade dos registos de viagem após restabelecimento de rede, sem abandono forçado de bicicletas na via pública;
  * Proteção do negócio contra multas de concessão decorrentes de falhas de comunicação da operadora móvel;
  * Cumprimento do cenário do Top 3 de Robustez.
* **Negativas:**
  * Exigência de hardware nas docas com memória não-volátil e relógio de tempo real (RTC) sincronizado;
  * O backend necessita de suportar eventos recebidos fora de ordem temporal e recalcular a duração histórica.

## Alternativas Rejeitadas
1. **Operação Estritamente Conectada (Bloquear Devoluções sem Rede):** *Rejeitada.* Se a doca recusasse o encaixe por falta de rede, o ciclista ficaria impossibilitado de devolver a bicicleta, continuaria a ser cobrado injustamente.
2. **Desbloqueio Totalmente Desconectado (Offline Unlocking):** *Rejeitada.* Permitir destrancar bicicletas sem contactar o servidor impediria a verificação de passes navegantes inválidos, saldos negativos e cartões roubados, expondo a frota a roubos em massa em zonas com má cobertura de rede.
