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
| `trip_lifecycle` | Controlar o estado e a duração das viagens, fechando-as quando a doca confirma a devolução. | 2 | 2 | 0.5 |
| `tariff_billing` | Calcular tarifas, processar cobranças e gerir a emissão e comunicação de faturas. | 1 | 1 | 0.5 |
| `station_availability` | Manter a visão atualizada da ocupação e da disponibilidade de bicicletas e docas por estação. | 2 | 1 | 0.33 |
| `sensor_ingestion` | Receber e processar a telemetria das bicicletas elétricas. | 0 | 2 | 1.0 |
| `maintenance_orders` | Criar, acompanhar e concluir ordens de manutenção preventiva e corretiva. | 1 | 0 | 0.0 |
| `fleet_redistribution` | Calcular e sugerir redistribuições de bicicletas e respetivas rotas para as carrinhas. | 0 | 1 | 1.0 |
| `reporting_audit` | Agregar métricas operacionais para os relatórios municipais mensais. | 1 | 0 | 0.0 |
| `user_history` | Permitir ao utilizador consultar o seu histórico de viagens e faturas. | 0 | 2 | 1.0 |

![Diagrama da Arquitetura Lógica do Lisboa Pedala](./assets/diagrama-final.png)