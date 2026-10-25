# C4 Nível 2 — Diagrama de Contentores

Este diagrama de contentores ilustra os limites arquiteturais do sistema **Lisboa Pedala**, os contentores de software que o compõem, os protocolos de comunicação e os limites de integração, conforme representado na imagem `DiagramaContainer.jpg`.

```mermaid
flowchart TB
    %% Definição de Classes e Estilos
    classDef actorStyle fill:#FF9933,stroke:#CC6600,stroke-width:2px,color:#000000;
    classDef appStyle fill:#F4C430,stroke:#D4A017,stroke-width:2px,color:#000000,font-weight:bold;
    classDef dbStyle fill:#4CAF50,stroke:#2E7D32,stroke-width:2px,color:#FFFFFF,font-weight:bold;
    classDef externalStyle fill:#8A2BE2,stroke:#5A189A,stroke-width:2px,color:#FFFFFF;
    classDef systemBoundary fill:none,stroke:#0099FF,stroke-width:2px,stroke-dasharray: 5 5,color:#0077CC;

    %% Atores no Topo
    User["Utilizador/Assinante"]:::actorStyle
    Tecnico["Técnico"]:::actorStyle

    %% Atores na Base
    Gestor["Gestor Municipal"]:::actorStyle
    Distribuicao["Membro da distribuição"]:::actorStyle

    %% Serviços Externos
    DocaFisica["Doca física"]:::externalStyle
    Pagamentos["Pagamentos"]:::externalStyle
    MapasRotas["Serviço de mapas/rotas"]:::externalStyle
    AutoridadeTributaria["Autoridade Tributária"]:::externalStyle

    %% Limite do Sistema
    subgraph SystemBoundary ["Kata Lisboa Pedala"]
        MobileApp["App móvel / interface do utilizador"]:::appStyle
        Backend["Backend Lisboa Pedala - Java<br/>(Contêm os 8 módulos de dominio)"]:::appStyle
        DB[("Base de Dados Relacional")]:::dbStyle
    end

    %% Ligações dos Atores
    User <-->|HTTP| MobileApp
    Tecnico <-->|HTTP| Backend
    Gestor <-->|HTTP| Backend
    Distribuicao <-->|HTTP| Backend

    %% Ligação Hardware com Mobile / Frontend
    DocaFisica <-->|TCP| MobileApp

    %% Ligações Internas entre Contentores
    MobileApp <--> Backend
    Backend <-->|SQL| DB

    %% Integrações com Serviços Externos (REST)
    Backend -->|HTTP/REST| Pagamentos
    Backend -->|HTTP/REST| MapasRotas
    Backend -->|HTTP/REST| AutoridadeTributaria

    %% Subgrafo de Legenda
    subgraph Legenda ["Legenda"]
        LegExt["Serviços externos"]:::externalStyle
        LegBoundary["Limite do Sistema"]:::systemBoundary
        LegActor["Atores"]:::actorStyle
        LegDB[("Base de Dados")]:::dbStyle
        LegApp["App"]:::appStyle
    end
```

---

## Descrição dos Contentores

### 1. Limite do Sistema (Kata Lisboa Pedala)
* **App móvel / interface do utilizador**: Ponto de interação do cliente/assinante para desbloqueio por QR Code, leitura de cartão Navegante e consulta de estações. Comunica via TCP com a doca física em proximidade e via HTTP com o backend.
* **Backend Lisboa Pedala - Java**: Aplicação central estruturada como monólito modular (Java 21), agregando os 8 módulos de domínio de negócio.
* **Base de Dados Relacional**: Persistência partilhada em SQLite/PostgreSQL com esquemas lógicos segregados para cada módulo de domínio.

### 2. Integrações Externas
* **Doca física**: Hardware das estações com protocolo de comunicação TCP.
* **Pagamentos**: Gateway de liquidação financeira (MB WAY / cartões).
* **Serviço de mapas/rotas**: API externa para cálculo de georreferenciação e otimização de rotas de carrinhas.
* **Autoridade Tributária**: Emissão e arquivo fiscal de faturas de viagens (retenção a 10 anos).
