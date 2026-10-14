# Escolha do Ações por Ator

Optou-se pela abordagem baseada em Ações por Ator (em detrimento de um fluxo puramente sequencial de Workflow), devido à heterogeneidade dos utilizadores do sistema "Lisboa Pedala" — utlizadores, operadores de frota, equipas de manutenção e administradores municipais —, cada um com responsabilidades, objetivos e conjuntos de ações distintos. Esta perspetiva permitiu estruturar os componentes lógicos de forma diretamente alinhada com as interações específicas de cada perfil de ator.

# Mapeamento de Ações por Ator e Componentes Lógicos

Esta tabela organiza as ações identificadas para cada perfil de utilizador/sistema do kata *Lisboa Pedala* e associa-as diretamente aos respetivos componentes lógicos, servindo de base para o desenvolvimento do `02-componentes.md`.

| Ator / Perfil | Ação | Componente Lógico Correspondente |
| :--- | :--- | :--- |
| **Assinante** | Verificar bicicletas livres | `bike_availability` |
| | Desbloquear bicicleta | `dock_access` |
| | Reportar avaria na eventualidade da mesma | `maintenance_orders` |
| | Verificar docas livres | `station_availability` |
| | Devolver a bicicleta | `dock_access` (fecho) |
| | Notificação do limite do tempo gratuito | `endTime_notification` |
| | Pagar tempo de viagem extra | `tariff_billing` |
| | Consultar histórico e faturas | `history_reader` |
| **Técnico** | Receber notificações de avarias de bicicleta | `maintenance_orders/notifications` |
| | Enviar notificação de localização das bicicletas avariadas para as carrinhas | `location notification sender` |
| | Concluir ordem de manutenção e repor a bicicleta no estado "operacional/disponível" | `maintenance_orders` |
| **Sistema** | Efetuar pagamento (Payment) | `tariff_billing` |
| | Enviar telemetria (FleetAndTelemetry) | `station_availability` |
| | Comunicar à autoridade tributária as faturas | `tariff_billing` |
| | Atualizar mapa em tempo real | `station_availability` |
| | Sincronização de eventos offline | `offline-sync` |
| **Carrinhas (Equipas de Redistribuição)** | Receber rotas e tarefas de redistribuição sugeridas pelo sistema | `fleet_redistribution` |
| | Consultar rotas | `route_reader` |
| | Registar recolha de bicicletas numa estação congestionada | `bike_status` |
| | Registar entrega e colocação de bicicletas numa estação com falta de frota | `bike_status` |
| **Gestor Municipal** | Receber relatórios | `report_notification` |
| | Consultar relatórios | `report_notification` |
| | Aplicar tarifários diferentes com base na localização | `tariff schedule` |

# Componentes Lógicos Finais

| Componente Lógico | Responsabilidade Única (Coesão) | Ca (Entradas) | Ce (Saídas) | Instabilidade ($I$) |
| :--- | :--- | :---: | :---: | :---: |
| `dock_access` | Gerir/Organizar e validar os comandos de desbloqueio e encaixe, comunicando com as docas. | 1 | 1 | 0.5 |
| `trip_lifecycle` | Controlar o estado e a duração das viagens, fechando-as quando a doca confirma a devolução. | 3 | 1 | 0.25 |
| `tariff_billing` | Calcular tarifas, processar cobranças e gerir a emissão e comunicação de faturas. | 1 | 1 | 0.5 |
| `station_availability` | Manter a visão atualizada da ocupação e da disponibilidade de bicicletas e docas por estação. | 2 | 1 | 0.33 |
| `sensor_ingestion` | Receber e processar a telemetria das bicicletas elétricas. | 2 | 0 | 0 |
| `maintenance_orders` | Criar, acompanhar e concluir ordens de manutenção preventiva e corretiva. | 0 | 1 | 1.0 |
| `fleet_redistribution` | Calcular e sugerir redistribuições de bicicletas e respetivas rotas para as carrinhas. | 0 | 1 | 1.0 |
| `reporting_audit` | Agregar métricas operacionais para os relatórios municipais mensais. | 0 | 1 | 1.0 |
| `history_reader` | Permitir ao utilizador consultar o seu histórico de viagens e faturas. | 0 | 2 | 1.0 |

![Diagrama da Arquitetura Lógica do Lisboa Pedala](./assets/diagrama-final.png)

# Análises

## Coesão
   No que respeita à Análise de Coesão, todos os componentes lógicos identificados no sistema 'Lisboa Pedala' demonstram Alta Coesão Funcional, cumprindo o Princípio da Responsabilidade Única. A decomposição estrutural evitou deliberadamente a 'Entity Trap', estruturando os módulos com base em fluxos de ação (workflows orientados a atores) e não em meras entidades de dados. Como resultado, cada componente agrupa exclusivamente o comportamento e os dados estritamente necessários para cumprir o seu propósito operacional — isolando domínios críticos como o ciclo de vida das viagens (trip_lifecycle), a telemetria (sensor_ingestion) e a faturação (tariff_billing) —, o que simplifica a manutenção evolutiva do sistema.


##  Acoplamento aferente e eferente
   Em relação ao acompolamento a arquitetura apresenta um equilíbrio saudável e intencional entre componentes estáveis e componentes voláteis.
   ### Componentes Estáveis

   Os componentes mais estáveis da arquitetura são: `trip_lifecycle` ($I = 0.25$), `station_availability` ($I = 0.33$) e `sensor_ingestion` ($I = 0.0$). Trata-se de componentes centrais que agregam informações e ações-chave da aplicação. Por exemplo, o `trip_lifecycle` gere o ciclo de vida da viagem e alimenta diretamente o histórico do utilizador,os relatórios municipais e o cálculo dos pagamentos, o que justifica a sua alta estabilidade ($Ca$ elevado) para evitar efeitos de propagação de erro para os componentes que dele dependem.

   ### Componentes equilibrados

   Os componentes que apresentam um nível de instabilidade intermédio ($I = 0.5$) — nomeadamente o dock_access e o tariff_billing — desempenham um papel crucial de mediação e transição na arquitetura do sistema.Estes módulos caracterizam-se por um equilíbrio simétrico entre o acoplamento aferente e eferente ($Ca = 1$ e $Ce = 1$), o que significa que dependem estritamente de um componente externo para executar a sua lógica, mas são, por sua vez, diretamente referenciados por outro elemento central da aplicação.
   Por exemplo, o dock_access atua como ponto de articulação entre a camada física das docas e o núcleo transacional (trip_lifecycle), processando comandos de encaixe e desencaixe de forma equilibrada

   ### Componentes Voláteis / Periféricos

   Em contrapartida, os componentes nas extremidades da arquitetura — como `maintenance_orders`, `fleet_redistribution`, `reporting_audit` e `history_reader` — apresentam uma instabilidade máxima ($I = 1.0$). Como não possuem dependências de entrada diretas ($Ca = 0$), funcionam como módulos periféricos flexíveis que podem ser alterados ou refatorizados sem colocar em risco o núcleo transacional do sistema.


# Exemplos de Acoplamento Temporal

1. **`dock_access` e `trip_lifecycle`:** 
   O componente `trip_lifecycle` depende temporalmente e de forma síncrona do momento exato em que a bicicleta é desbloqueada ou encaixada na doca. No instante do início da viagem e no instante do fecho da viagem, o `dock_access` envia uma chamada síncrona para o `trip_lifecycle` para confirmar o estado físico do encaixe, servindo para respetivamente começar a contagem e terminar a contagem do tempo da viagem.

2. **`fleet_redistribution` e `station_availability`:** 
   Para que o cálculo de rotas e sugestões de redistribuição para as carrinhas funcione, o componente `fleet_redistribution` precisa de consultar diretamente e em tempo real o componente `station_availability`. Esta consulta exige uma resposta imediata e síncrona para conhecer quais as estações que têm défice ou excesso de bicicletas naquele preciso momento operacional.

# Restruturação 

   Numa fase inicial, a aplicação estrita da decomposição por Ações por Ator conduziu a uma fragmentação excessiva, gerando uma proliferação de micro-componentes periféricos e sobrepostos (como bike_availability, endTime_notification, route_reader, etc.). Esta granularidade extrema violava criava um acoplamento eferente excessivamente complexo e difícil de manter, tendo cada componente ter de chamar vários componentes para executar as suas funções

   Ação Tomada:
   Procedeu-se a uma iteração de reestruturação e consolidação arquitetural, fundindo componentes de consulta e notificação dispersos nos respetivos domínios principais. Por exemplo, as lógicas de verificação de disponibilidade de bicicletas e docas foram unificadas no componente station_availability.

   Resultado:
   Esta refatorização originou a matriz de Componentes Lógicos Finais, garantindo um equilíbrio ótimo entre a alta coesão funcional, a redução da complexidade estrutural e a viabilidade de implementação do sistema Lisboa Pedala.
