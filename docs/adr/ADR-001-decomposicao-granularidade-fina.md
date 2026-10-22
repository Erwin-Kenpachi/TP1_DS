# ADR-001: Decomposição Inicial por Ações dos Atores com Granularidade Fina

- **Estado:** Substituído por [ADR-002](./ADR-002-consolidacao-componentes-logicos.md)
- **Data:** Semana 1 (Modelação Inicial)
- **Contexto dos Documentos:** [docs/02-componentes.md](../02-componentes.md#L98-L106)

## Contexto

Na fase inicial da modelação lógica do sistema *Lisboa Pedala*, procurou-se identificar os componentes a partir das ações realizadas pelos diferentes atores, seguindo a abordagem *Ações por Ator* abordada na Aula 4. O objetivo era atribuir responsabilidades claras aos componentes, evitando organizar a arquitetura apenas em torno das entidades de dados do sistema, uma abordagem que poderia levar ao antipadrão *Entity Trap*.

Assim, para cada ação identificada nos requisitos, como verificar a disponibilidade de bicicletas, destrancar uma doca, alertar para o limite de 45 minutos de viagem, consultar rotas de carrinhas ou aplicar tarifas, foram inicialmente definidos componentes lógicos autónomos e com responsabilidades bastante específicas.

No entanto, esta primeira decomposição originou um número elevado de componentes, alguns dos quais tinham responsabilidades demasiado limitadas. Durante a tentativa posterior de agrupar e reorganizar estes componentes, surgiu também o risco de passar a organizar a arquitetura em torno de entidades do domínio, através de componentes como `BikeManager`, em vez de agrupar as responsabilidades de acordo com as funcionalidades e os comportamentos do sistema.

Esta abordagem não resolvia os problemas da decomposição inicial e poderia conduzir ao antipadrão *Entity Trap*. Por esse motivo, tornou-se necessário rever a organização dos componentes e definir uma estrutura mais coerente, documentada no ADR-002.

## Decisão

Na fase inicial, optou-se por criar componentes lógicos de granularidade fina, cada um associado a uma ação ou responsabilidade específica. Entre os componentes identificados encontravam-se:

- `bike_availability`: verificar a disponibilidade de bicicletas;
- `dock_access`: controlar as operações de destrancamento e trancamento das docas;
- `endTime_notification`: emitir alertas associados ao limite de duração de uma viagem;
- `location_notification_sender`: enviar a localização de bicicletas com avarias;
- `route_reader`: consultar rotas das carrinhas de redistribuição;
- `tariff_schedule`: gerir a aplicação de tarifas e respetivos horários;
- `report_notification`: enviar notificações relacionadas com relatórios.

Esta decomposição permitiu identificar responsabilidades de forma explícita, mas a sua granularidade revelou-se excessiva para a dimensão e os objetivos do projeto. Na tentativa de consolidar os componentes, considerou-se também uma organização centrada em entidades, que poderia originar componentes como `BikeManager`. Esta solução foi rejeitada por não representar adequadamente as responsabilidades funcionais do sistema.

Assim, a abordagem inicial foi substituída por uma organização que agrupa responsabilidades relacionadas, tendo em conta as funcionalidades do domínio e as dependências entre componentes. A decisão adotada na reformulação encontra-se descrita no ADR-002.

## Consequências

- **Positivas:**
  - As responsabilidades iniciais ficaram identificadas de forma clara, facilitando a compreensão dos requisitos e das ações dos atores;
  - A granularidade fina permitiu identificar funcionalidades específicas que poderiam passar despercebidas numa decomposição menos detalhada;
  - A análise das limitações da abordagem inicial contribuiu para fundamentar a reformulação da arquitetura.

- **Negativas:**
  - **Fragmentação excessiva:** a criação de muitos componentes pequenos aumentou a complexidade da sua coordenação e dificultou a organização do trabalho de uma equipa de seis pessoas;
  - **Acoplamento entre componentes:** operações como a devolução de uma bicicleta podiam exigir a interação de vários componentes, aumentando o número de dependências necessárias para concluir uma operação;
  - **Dependência da sequência de execução:** a colaboração entre componentes muito específicos tornava alguns fluxos dependentes da ordem pela qual as operações eram executadas;
  - **Maior esforço de manutenção e testes:** a existência de muitos componentes aumentava o esforço necessário para testar as interações e verificar o cumprimento das regras arquiteturais;
  - **Risco de cair no *Entity Trap* durante a consolidação:** a tentativa de simplificar a estrutura através de componentes organizados por entidades, como `BikeManager`, poderia concentrar responsabilidades distintas num mesmo componente e dificultar a separação da lógica de negócio.

## Alternativas Rejeitadas

1. **Organização por entidades (*Entity Trap*):** organizar os componentes em torno das entidades do domínio, por exemplo, `BikeManager`, `StationManager`, `UserManager` ou `InvoiceManager`. Esta abordagem foi considerada durante a tentativa de consolidação, mas rejeitada por não garantir que as responsabilidades ficassem agrupadas de acordo com as funcionalidades do sistema. Poderia concentrar comportamentos distintos em componentes demasiado abrangentes e dificultar a manutenção e a evolução da arquitetura.

2. **Manter a decomposição inicial sem alterações:** conservar todos os componentes de granularidade fina. Esta alternativa foi rejeitada porque mantinha a fragmentação excessiva, aumentava o número de dependências entre componentes e tornava a implementação e os testes mais complexos.

A reformulação da organização dos componentes e a solução adotada encontram-se documentadas no ADR-002.