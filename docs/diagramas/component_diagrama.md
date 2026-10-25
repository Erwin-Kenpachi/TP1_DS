# C4 Nível 3 — Diagrama de Componentes

Este diagrama de componentes detalha os 8 módulos de domínio internos que compõem o **Backend Lisboa Pedala (Monólito Modular)** em Java, as suas interações internas (*Allowed-to-Use*), a persistência na base de dados relacional e a comunicação com os atores e serviços externos, conforme ilustrado na imagem `DiagramaComponent.jpg`.

```mermaid
flowchart TB
    %% Definição de Estilos
    classDef actorStyle fill:#FF9933,stroke:#CC6600,stroke-width:2px,color:#000000;
    classDef moduleStyle fill:#F4C430,stroke:#D4A017,stroke-width:2px,color:#000000,font-weight:bold;
    classDef dbStyle fill:#4CAF50,stroke:#2E7D32,stroke-width:2px,color:#FFFFFF,font-weight:bold;
    classDef externalStyle fill:#8A2BE2,stroke:#5A189A,stroke-width:2px,color:#FFFFFF;
    classDef boundaryStyle fill:none,stroke:#0099FF,stroke-width:2px,stroke-dasharray: 5 5,color:#0077CC;
    classDef dbLink stroke:#2E7D32,stroke-width:1.5px;

    %% Atores Externos
    Tecnico["Técnico"]:::actorStyle
    Distribuicao["Membro da distribuição"]:::actorStyle
    Gestor["Gestor Municipal"]:::actorStyle
    User["Utilizador/Assinante"]:::actorStyle

    %% Serviços Externos
    DocaFisica["Doca física"]:::externalStyle
    Pagamentos["Pagamentos"]:::externalStyle
    MapasRotas["Serviço de mapas/rotas"]:::externalStyle
    AutoridadeTributaria["Autoridade Tributária"]:::externalStyle

    %% Limite do Sistema Backend (Monólito Modular)
    subgraph Backend ["Backend Lisboa Pedala (Monolito Modular) - Java"]
        Fleet["fleet_redistribution"]:::moduleStyle
        Station["station_availability"]:::moduleStyle
        Dock["dock_access"]:::moduleStyle
        Trip["trip_lifecycle"]:::moduleStyle
        Sensor["sensor_ingestion"]:::moduleStyle
        Billing["tariff_billing"]:::moduleStyle
        Maintenance["maintenance_orders"]:::moduleStyle
        HistoryQuery["history_reader"]:::moduleStyle
        HistoryUser["history_reader"]:::moduleStyle
        Reporting["reporting_audit"]:::moduleStyle
        DB[("Base de Dados Relacional")]:::dbStyle
    end

    %% Ligações dos Atores aos Módulos
    Distribuicao --> Fleet
    Tecnico --> Maintenance
    Gestor -->|HTTP| Reporting
    User --> Dock
    User --> HistoryUser

    %% Ligações dos Módulos com Serviços Externos (REST)
    DocaFisica -->|HTTP / REST| Dock
    Billing -->|HTTP / REST| Pagamentos
    Backend -.->|HTTP / REST| MapasRotas
    Billing -->|HTTP / REST| AutoridadeTributaria

    %% Comunicação entre Módulos de Domínio
    Fleet -->|Consultar ocupação das estações| Station
    Dock -->|Envia estado da doca| Station
    Dock -->|Regista e gere inicio e fim da viagem| Trip
    Trip --> Station
    Trip -->|Obtem os dados da viagem| Billing
    Sensor -->|Envia os dados das bicicletas| Maintenance
    Maintenance -->|Atualiza estado da bicicleta| Sensor
    Sensor -->|Envia dados da viagem| Trip
    Billing -->|Dados de faturação| HistoryQuery
    Trip -->|Envia dados da viagem| HistoryQuery
    HistoryQuery -->|Obtem dados de faturação| Reporting

    %% Ligações à Base de Dados Relacional (Persistência)
    Fleet ---> DB
    Dock ---> DB
    Station ---> DB
    Trip ---> DB
    Sensor ---> DB
    Billing ---> DB
    Maintenance ---> DB
    Reporting ---> DB
    HistoryQuery ---> DB
    HistoryUser ---> DB

    %% Subgrafo de Legenda
    subgraph Legenda ["Legenda"]
        LegExt["Serviços externos"]:::externalStyle
        LegBoundary["Limite do Sistema"]:::boundaryStyle
        LegActor["Atores"]:::actorStyle
        LegDB[("Base de Dados")]:::dbStyle
        LegMod["Módulos dentro do backend"]:::moduleStyle
    end
```

---

## Descrição dos Componentes e Interações

### 1. Módulos de Domínio Internos
* **`dock_access`**: Ponto de contacto com a doca física. Coordena a trancagem/destrancagem física e delega em `trip_lifecycle` e `station_availability`.
* **`trip_lifecycle`**: Núcleo transacional que orquestra o estado das viagens (`IN_PROGRESS`, `COMPLETED`), notificando `tariff_billing` para faturação.
* **`station_availability`**: Regista a ocupação em tempo real (bicicletas disponíveis e docas livres).
* **`tariff_billing`**: Módulo folha responsável pelo cálculo tarifário (regras de escalões de 15 min, passe gratuito) e interface com a Autoridade Tributária e gateways de pagamento.
* **`sensor_ingestion`**: Recolha de telemetria IoT das bicicletas elétricas (bateria, odómetro, travões).
* **`maintenance_orders`**: Gestão do ciclo de vida das ordens de reparação atribuídas aos técnicos.
* **`fleet_redistribution`**: Algoritmos de sugestão de rotas de rebalanceamento para as carrinhas municipais.
* **`reporting_audit`**: Produção de relatórios consolidados de utilização por freguesia e emissões de CO₂ para a Câmara Municipal.
* **`history_reader`**: Componente de leitura e agregação otimizada para consultas de histórico pelo utilizador e dados consolidados para auditoria.

### 2. Base de Dados Relacional
* Suporte de persistência único no monólito (SQLite em ficheiro), com esquemas de tabelas logicamente segregados por módulo (`schema.sql` em cada componente), garantindo a ausência de acoplamento direto de tabelas.
