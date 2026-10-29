# Relatório de Reflexão Arquitetural: Governação e Uso de IA

**Unidade Curricular:** Design de Software (Mestrado em Engenharia Informática)  
**Projeto:** TP1 — Especificação de Arquitetura e Implementação do Monólito Modular *Lisboa Pedala*  

---

## 1. O que a Especificação Inicial Não Previu

Durante o processo de geração iterativa do sistema através de ferramentas de Inteligência Artificial e consequente validação experimental do protótipo funcional, identificaram-se omissões e ambiguidades na especificação inicial que exigiram refinamento arquitetural:

1. **Gestão de Sessão e Credenciais na Camada de Apresentação (`LisboaPedalaApp`):**  
   A especificação inicial concentrou-se profundamente nos módulos de domínio da US-1 (`dock_access`, `trip_lifecycle`, `tariff_billing`). Contudo, não foi definido um módulo dedicado para gestão de utilizadores e credenciais de login. Ao desenvolver a interface de utilizador (CLI), a IA gerou fluxos onde o `userId` era solicitado de forma ad-hoc em cada operação, mantendo o estado de sessão em memória volátil, o que originou perda de dados entre execuções da aplicação.
2. **Divergência de Dependências: Documento 03 vs. `ARCHITECTURE.md`:**  
   No documento `03-particionamento-e-vistas.md` (Secção 2.4 e 3), a tabela de *Allowed-to-Use* autorizava o módulo `sensor_ingestion` a aceder a `station_availability` e sugeria que a classe `MalfunctionEmitter` disparasse alertas diretamente para a fachada de manutenção. Pelo contrário, o `ARCHITECTURE.md` e a regra de consolidação (ADR-002) classificam `sensor_ingestion` como módulo folha/fonte com instabilidade nula ($I=0.0$), sem qualquer dependência eferente permitida. Esta ambiguidade induziu a IA a gerar acoplamentos proibidos.
3. **Persistência de Estado e Inicialização Idempotente da Base de Dados:**  
   Não estava formalizado como a aplicação interativa deveria garantir que dados de sementeira (estações, docas, bicicletas) e registos de utilizadores persistiam sem duplicar dados nem apagar o ficheiro `lisboapedala.db` a cada arranque.

---

## 2. Casos Documentados de Violação da Especificação pelo Código Gerado

Conforme estipulado no enunciado, documentam-se quatro casos em que o código gerado por IA violou os requisitos arquiteturais e de especificação:

### Caso 1: Perda de Persistência e Volatilidade de Estado no `LisboaPedalaApp`
* **Descrição da Violação:** Na primeira implementação da interface de consola (CLI), a IA gerou os controladores e o menu principal sem assegurar a persistência duradoura do utilizador autenticado nem o registo de faturas entre diferentes execuções do programa. Ao fechar e reiniciar o programa, o histórico e as faturas associadas ao utilizador desapareciam, violando o requisito funcional da US-1 e a garantia de retenção e durabilidade.
* **Quem Detetou:** **Revisão Humana** (testes exploratórios do aluno ao executar a aplicação).
* **Por que a Fitness Function falhou em detetar:** A fitness function (ArchUnit) analisa exclusivamente o grafo estático de dependências entre classes compiladas no bytecode Java. Não executa a aplicação nem valida se o ciclo de vida dos dados persiste em disco no SQLite.
* **Ação Corretiva:** Criação de um mecanismo de registo e login com hashing de palavra-passe e verificação prévia de viagens ativas antes de permitir novos desbloqueios.

### Caso 2: Violação do Isolamento de Domínio — Inserção de `UserRepository` no Pacote `common`
* **Descrição da Violação:** Para solucionar rapidamente o problema de autenticação do Caso 1, a IA adicionou o repositório `UserRepository.java` diretamente no pacote `pt.ulisboa.lisboapedala.common`. Esta solução violou o princípio de particionamento por domínio e a Secção 1 do `ARCHITECTURE.md`, uma vez que o módulo `common` foi desenhado estritamente para utilitários técnicos transversais agnósticos ao negócio (`DatabaseManager`), e não para suportar tabelas de entidades de utilizadores com persistência própria sem fachada.
* **Quem Detetou:** **Revisão Humana / Análise Crítica Arquitetural** (detalhado no documento de análise de conformidade do repositório).
* **Por que a Fitness Function falhou em detetar:** A regra arquitetural `no_access_to_internal_packages_of_other_modules` em [`ModularMonolithArchitectureTest.java`](file:///c:/Users/Diogo%20Correia/Documents/FCUL_Mestrado/DesignSoftware/Trabalho1/TP1_DS/tests/architecture/ModularMonolithArchitectureTest.java) contém a condição `if (sourceModule == null || sourceModule.equals("common")) return;`, tratando o pacote `common` como uma exceção cega. A fitness function não validou se `common` continha entidades ou repositórios indevidos.
* **Ação Corretiva:** Documentação do trade-off e planeamento da extração de um módulo formal de utilizadores/autenticação ou subordinação a um módulo de gestão de clientes.

### Caso 3: Violação de *Allowed-to-Use* e Inversão de Dependência em `sensor_ingestion`
* **Descrição da Violação:** Na geração da infraestrutura de telemetria da US-2, a IA seguiu a descrição do documento `03-particionamento-e-vistas.md` (onde se referia que `MalfunctionEmitter` avisava a manutenção) e importou a fachada `MaintenanceOrdersFacade` diretamente dentro do módulo `sensor_ingestion`. Esta chamada síncrona gerou acoplamento eferente proibido e risco de dependência circular.
* **Quem Detetou:** **Fitness Function (ArchUnit)**.
* **Comportamento da Fitness Function:** O teste automatizado `sensor_ingestion_should_not_depend_on_other_domain_modules` falhou imediatamente na execução de `mvn test` com `AssertionError`, indicando que a classe `MalfunctionEmitter` violava a regra de proibição de dependências sobre `maintenance_orders`.
* **Ação Corretiva:** Rejeição do código gerado e ajuste do prompt: o `sensor_ingestion` passou a guardar internamente as anomalias detetadas e a disponibilizá-las via `SensorIngestionFacade`, cabendo ao módulo `maintenance_orders` consultar a telemetria ou subscrever o evento.


### Caso 4: Violação de *Allowed-to-Use* no Módulo `maintenance_orders`
* **Descrição da Violação:** Durante a geração do módulo `maintenance_orders`, a IA concedeu indevidamente acesso ao módulo `station_availability`, injetando a sua fachada pública (`StationAvailabilityFacade`) no construtor da `MaintenanceOrdersFacade` e no serviço interno `TicketService`. Contudo, esta dependência nunca teve utilização funcional no código de gestão de ordens e violava a matriz normativa do `ARCHITECTURE.md` (Secção 3) e do `03-particionamento-e-vistas.md` (Secção 2.4), os quais estipulam que o módulo `maintenance_orders` pode depender única e exclusivamente de `sensor_ingestion` (para recolha e sincronização das anomalias técnicas das bicicletas).
* **Quem Detetou:** **Revisão Humana / Análise Crítica de Conformidade Arquitetural**.
* **Por que a Fitness Function falhou em detetar:** A *fitness function* inicial de ArchUnit (`maintenance_orders_allowed_dependencies`) foi codificada com base num rascunho anterior da especificação que admitia erradamente este acoplamento. Por essa razão, a regra excluiu deliberadamente `station_availability` da lista de pacotes proibidos e continha um comentário tolerando o acesso indevido (`// maintenance_orders só pode usar sensor_ingestion e station_availability`). Desta forma, a própria *fitness function* continha um ângulo morto por decalque de documentação desatualizada, validando falsamente como correto um acoplamento que violava o princípio do menor privilégio e as regras *Allowed-to-Use*.
* **Ação Corretiva:** 
  1. Remoção imediata do parâmetro e da referência a `StationAvailabilityFacade` em `MaintenanceOrdersFacade.java` e `TicketService.java`, bem como dos respetivos pontos de instanciação (`LisboaPedalaApp.java` e `MaintenanceOrdersTest.java`).
  2. Correção da regra 7 no ArchUnit (`ModularMonolithArchitectureTest.java`), acrescentando `pt.ulisboa.lisboapedala.station_availability..` à lista de pacotes proibidos e garantindo por *fitness function* automatizada que `maintenance_orders` depende estritamente de `sensor_ingestion`.


---

## 3. Avaliação das Fitness Functions: Forças e Ângulos Mortos

A avaliação das fitness functions implementadas em [`ModularMonolithArchitectureTest.java`](file:///c:/Users/Diogo%20Correia/Documents/FCUL_Mestrado/DesignSoftware/Trabalho1/TP1_DS/tests/architecture/ModularMonolithArchitectureTest.java) revelou importantes forças, mas também fragilidades críticas:

| Aspeto Arquitetural | Capacidade da Fitness Function | Eficácia Observada |
| :--- | :--- | :--- |
| **Ausência de Ciclos (`slices().beFreeOfCycles()`)** | **Forte** | Detetou eficazmente tentativas da IA de criar referências cruzadas entre `dock_access` e `trip_lifecycle`. |
| **Matriz *Allowed-to-Use* Estática** | **Forte** | Bloqueou com sucesso imports diretos de módulos folha (`tariff_billing` e `sensor_ingestion`). |
| **Encapsulamento de Pacotes Internos** | **Média / Fraca** | Apenas verifica violações se o pacote de destino contiver explicitamente `.business` ou `.persistence`. Se a IA criar uma classe interna na raiz do módulo (`pt.ulisboa.lisboapedala.trip_lifecycle.TripHelper`), o ArchUnit deixa passar. |
| **Governação do Pacote `common`** | **Fraca (Ângulo Morto)** | A regra ignora o pacote `common`, permitindo que código impróprio de negócio ou repositórios sejam aí colocados sem disparar erro. |
| **Transacionalidade e Persistência Dinâmica** | **Nula (Inaplicável)** | Não é capaz de validar se transações de base de dados foram commitadas, nem a retenção de dados após reinício da aplicação. |
| **Acesso Cruzado a Tabelas SQL** | **Nula** | O ArchUnit analisa imports estáticos Java. Se um módulo executar uma query SQL direta (`SELECT * FROM trips`) a uma tabela de outro módulo via JDBC puro, a violação passa despercebida pela fitness function. |

---

## 4. Conclusão

A experiência comprova o pressuposto do enunciado: **as ferramentas de IA aceleram a geração de código, mas tendem consistentemente a adotar atalhos arquiteturais**, quebrando limites modulares quando os prompts são ambíguos ou quando a especificação tem divergências entre vistas. 

As *fitness functions* provaram ser indispensáveis como primeira linha de defesa automatizada no pipeline de integração. Contudo, demonstrou-se que **fitness functions puramente estruturais não substituem a revisão humana crítica** nem os testes de integração funcionais para salvaguardar a semântica dos dados, a persistência e o isolamento de domínio.
