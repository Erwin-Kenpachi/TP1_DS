# C4 Nível 1 — Diagrama de Contexto do Sistema

Este diagrama de contexto representa o sistema **Lisboa Pedala**, os seus utilizadores/atores principais e as integrações com serviços e entidades externas, conforme especificado na imagem `Diagrama_Context.jpg`.

```mermaid
flowchart TB
    %% Definição de Estilos
    classDef actorStyle fill:#FF9933,stroke:#CC6600,stroke-width:2px,color:#000000;
    classDef appStyle fill:#33A1FD,stroke:#0077CC,stroke-width:2px,color:#FFFFFF,font-weight:bold;
    classDef externalStyle fill:#8A2BE2,stroke:#5A189A,stroke-width:2px,color:#FFFFFF;
    classDef legendStyle fill:#FFFFFF,stroke:#CCCCCC,stroke-width:1px,color:#000000;

    %% Atores (Topo e Laterais)
    User["Utilizador/Assinante"]:::actorStyle
    Tecnico["Técnico"]:::actorStyle
    Distribuicao["Membro da distribuição"]:::actorStyle
    Gestor["Gestor Municipal"]:::actorStyle

    %% Sistema Central
    System["kata Lisboa Pedala"]:::appStyle

    %% Serviços Externos (Fundo)
    Pagamentos["Pagamentos"]:::externalStyle
    DocaFisica["Doca(fisica)"]:::externalStyle
    SMS["Serviço de Notificações SMS"]:::externalStyle

    %% Relações do Utilizador/Assinante
    User -->|Desbloqueia bicicleta| System
    System -->|Confirma bicicleta| User
    User -->|Pede histórico| System
    System -->|Recebe histórico| User

    %% Relações do Técnico
    System -->|Recebe ordem| Tecnico
    Tecnico -->|Atualiza estado| System

    %% Relações do Membro da Distribuição
    System -->|Recebe Rota| Distribuicao

    %% Relações do Gestor Municipal
    System -->|Recebe relatorio| Gestor
    Gestor -->|Altera tarifário| System

    %% Relações com Serviços Externos
    System -->|Processa Mb way e multibanco| Pagamentos
    System -->|Pedido abrir/fechar| DocaFisica
    DocaFisica -->|Confirma abrir/fechar| System
    System -->|Envia notificação| SMS

    %% Subgrafo de Legenda
    subgraph Legenda ["Legenda"]
        LegExt["Serviços externos"]:::externalStyle
        LegApp["App"]:::appStyle
        LegActor["Atores"]:::actorStyle
    end
```

---

## Descrição dos Elementos

### 1. Sistema Central
* **kata Lisboa Pedala**: Plataforma central de gestão do serviço de bicicletas partilhadas de Lisboa.

### 2. Atores
* **Utilizador/Assinante**: Interage com o sistema para desbloquear/devolver bicicletas e consultar o histórico de viagens.
* **Técnico**: Recebe ordens de intervenção em bicicletas avariadas e atualiza o estado de manutenção.
* **Membro da distribuição**: Recebe rotas de redistribuição de carrinhas para equilibrar a ocupação das estações.
* **Gestor Municipal**: Consulta relatórios de utilização e emissões de CO₂ e configura regras de tarifário.

### 3. Serviços Externos
* **Pagamentos**: Gateway externo para processamento de pagamentos MB WAY e cartões bancários.
* **Doca (física)**: Hardware IoT eletromecânico das estações com comunicação móvel.
* **Serviço de Notificações SMS**: Gateway para envio de alertas operacionais e recibos.
