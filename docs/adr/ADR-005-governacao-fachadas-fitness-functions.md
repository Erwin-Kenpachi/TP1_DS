# ADR-005: Governação Arquitetural via Fachadas Públicas e Verificação Automática por *Fitness Functions*

* **Estado:** **Aceite**
* **Data:** Semana 2
* **Contexto dos Documentos:** [docs/03-particionamento-e-vistas.md](../03-particionamento-e-vistas.md#L154-L170) e [docs/03-particionamento-e-vistas.md](../03-particionamento-e-vistas.md#L309-L317)

## Contexto
Numa arquitetura de Monólito Modular desenvolvida por uma equipa pequena, a ausência de barreiras físicas de processo ou compilação, facilita a criação inadvertida de dependências ilegais entre módulos (como controllers de um módulo injetarem repositórios ou entidades internas de outro). Como exposto na Aula 5, *"sem regras, a modularidade é apenas uma convenção de pastas"* e o sistema degenera rapidamente para o antipadrão *Big Ball of Mud*.

## Decisão
1. **Encapsulamento Estrito por Fachadas Públicas (*Public Facades*):**
   Cada módulo de domínio disponibiliza um único ponto de contacto público (ex.: `DockAccessFacade`, `TripLifecycleFacade`, `StationAvailabilityFacade`, `TariffBillingFacade`). Todas as classes internas de domínio, lógica e persistência (`*Service`, `*Repository`, entidades ORM) são encapsuladas e mantidas estritamente com visibilidade de pacote/módulo privado.
2. **Matriz Restritiva *Allowed-to-Use*:**
   Formalizar documentalmente as dependências unidirecionais permitidas (ex.: `dock_access` pode invocar `trip_lifecycle` e `station_availability`; mas `tariff_billing` e `station_availability` são módulos-folha que não podem invocar módulos orquestradores).
3. **Governação por *Fitness Functions* Automatizadas:**
   Integrar testes arquiteturais automáticos na pasta `tests/architecture/` (utilizando ferramentas como ArchUnit ou import-linter) executados a cada *build* e no pipeline de CI. O build falhará automaticamente caso surja qualquer ciclo entre módulos ou acesso que contorne a fachada pública.

## Consequências
* **Positivas:**
  * **Garantia Determinística da Arquitetura:** A conformidade com a especificação é validada automaticamente por código e não depende da boa vontade ou memória dos programadores;
  * **Baixo Acoplamento e Alta Coesão:** Respeita a Lei de Deméter e evita conascências acidentais de algoritmo ou execução;
  * **Facilidade de Refatorização:** A implementação interna de um módulo pode ser completamente refeita sem quebrar o resto do sistema, desde que o contrato da fachada seja respeitado.
* **Negativas:**
  * Exige a escrita e manutenção de testes arquiteturais dedicados;
  * Obriga à criação de DTOs explícitos para transferência de dados entre módulos, impedindo a partilha direta de entidades de domínio.

## Alternativas Rejeitadas
1. **Governação Baseada Exclusivamente em Revisões de Código Manuais (*Code Reviews*):** *Rejeitada.* Revisões manuais são suscetíveis a falhas humanas, fadiga e pressões de prazos, não constituindo uma barreira de segurança suficiente.
2. **Barramento de Eventos Assíncronos Interno (Event-Driven / In-Memory Message Bus) para Toda a Comunicação:** *Rejeitada.* Embora garantisse desacoplamento temporal, tornaria as operações transacionais síncronas essenciais (como a confirmação de desbloqueio de doca) excessivamente complexas de seguir, depurar e testar, aumentando desnecessariamente a conascência de tempo.
