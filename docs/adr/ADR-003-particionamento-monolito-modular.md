# ADR-003: Adoção do Estilo Monólito Modular (*Modular Monolith*) como Particionamento de Topo

* **Estado:** **Aceite**
* **Data:** Semana 2
* **Contexto dos Documentos:** [docs/03-particionamento-e-vistas.md](../03-particionamento-e-vistas.md#L18-L123)

## Contexto
O sistema Lisboa Pedala tem de gerir uma infraestrutura composta por 180 estações e 1 500 bicicletas, das quais 70% são elétricas. Cerca de 70% das viagens concentram-se nas horas de ponta (8h00–9h30 e 17h30–19h00), prevendo-se também que a procura possa quadruplicar durante eventos festivos. A estas exigências juntam-se as limitações de uma equipa fixa de apenas seis pessoas e de um orçamento reduzido. Além disso, prevê-se que, no futuro, outras câmaras municipais possam aderir à plataforma, cada uma com os seus próprios tarifários e regras fiscais e municipais.

Tendo em conta estes requisitos, foi necessário tomar uma decisão fundamental sobre a arquitetura de topo do sistema: optar por uma organização orientada às camadas técnicas (Layered Architecture) ou por uma organização baseada nas responsabilidades funcionais e nos domínios do negócio (Modular Monolith).

## Decisão
Adotar o **Particionamento por Domínio** implementado através do estilo arquitetural Monólito Modular (Modular Monolith). O sistema será constituído por uma única unidade de implantação (single deployment unit), organizada em módulos de primeiro nível que correspondem aos principais domínios funcionais, como `dock_access`, `trip_lifecycle` e `tariff_billing`. A comunicação entre módulos será realizada através de chamadas síncronas a métodos em memória (in-process calls), limitadas às respetivas fachadas públicas (Public Facades), de forma a controlar as dependências e preservar o encapsulamento de cada módulo.

## Consequências
* **Positivas:**
  * **Localidade da Mudança:** A esmagadora maioria das evoluções previstas incide sobre regras de negócio do domínio (ex.: novas tarifas em `tariff_billing`, regras de devolução em `dock_access`/`trip_lifecycle`). Com o monólito modular, as alterações ficam contidas num único módulo de domínio;
  * **Custo Operacional Reduzido:** Evita a sobrecarga de gerir infraestruturas distribuídas complexas (Kubernetes, service discovery, distributed tracing, circuit breakers remotos);
  * **Latência Mínima e Desempenho:** A comunicação entre domínios faz-se a nível de nanossegundos/microssegundos em memória, sem custos de serialização JSON ou latência cumulativa de rede;
  * **Alinhamento com a Lei de Conway:** Uma equipa multifuncional pequena comunica de forma mais eficiente sobre um repositório e unidade de entrega comuns.
* **Negativas:**
  * **Quanta Arquitetural Único ($quanta = 1$):** Todo o sistema partilha o mesmo ciclo de deploy. A escalabilidade exige a replicação do monólito completo (problema de sistemas monoliticos);
  * **Exigência de Governação Automática Rigorosa:** Sem barreiras físicas de processo ou compilação separada, há o risco permanente de desenvolvedores criarem atalhos ilegais entre módulos, conduzindo ao antipadrão *Big Ball of Mud*.

## Alternativas Rejeitadas
1. **Particionamento Técnico (*Layered Architecture*):** *Rejeitada.* Conforme detalhado em `03-particionamento-e-vistas.md`, a arquitetura por camadas espalha cada funcionalidade de negócio por múltiplas camadas técnicos (`presentation`, `business`, `persistence`). Este estilo tambem poderia induzir para o antipadrão *Architecture Sinkhole* (camadas que apenas reencaminham chamadas sem acrescentar valor). Apesar de ser um estilo que contem os papeis e as suas responsabilidades bem defenidas, tem como problema a dispersão do dominio. 
2. **Arquitetura de Microsserviços / Distribuída:** *Rejeitada.* Richards & Ford (2025) e as *Onze Falácias da Computação Distribuída* (Aula 5) demonstram que microsserviços introduzem complexidade extrema (latência não-nula, falhas parciais de rede, transações distribuídas/Sagas). Para a dimensão do Lisboa Pedala (~35 req/s de telemetria e ~75 000 utilizadores), a dispersão em 20+ microsserviços seria incomportável para uma equipa de pequena de pessoas e um orçamento fixo.
