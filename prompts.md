# Prompts 

## Prompt 1
Por favor, com base na documentação presente em ARCHITECTURE.md e na pasta docs/ (01-caracteristicas.md, 02-componentes.md e 03-particionamento-e-vistas.md), implementa a fundação do monólito modular em Java 21 para o sistema Lisboa Pedala.

Implementa as seguintes User Stories:
• US-1 (Obrigatória): Como assinante, desbloqueio e devolvo uma bicicleta e a viagem fica registada e cobrada. Envolve os módulos dock_access, trip_lifecycle e tariff_billing. 
• US-2: Como técnico, recebo uma ordem de manutenção quando uma bicicleta reporta avaria. Envolve os módulos sensor_ingestion e maintenance_orders. 
• US-4: Como gestor municipal, obtenho o relatório de utilização por freguesia, envolve os módulos trip_lifecycle e reporting_audit.

Tarefas a executar:
• Criar a estrutura de pacotes base pt.ulisboa.lisboapedala respeitando os módulos de domínio definidos nos documentos.
• Implementar as Fachadas públicas (*Facade.java) de cada módulo envolvido, garantindo que nenhum componente interno é acedido diretamente.
• Configurar os esquemas de base de dados SQLite (schema.sql) segregados por módulo. 
• Escrever os testes unitários e de integração que validem o fluxo transacional da US-1 (incluindo a idempotência e o tratamento de eventos).
• Implementar os testes arquiteturais (ex: ArchUnit em tests/architecture/) para validar o isolamento modular.
• Os pagamentos são simulados.

Cria um plano de criação por 3 fases:
• Fase 1: Implementação da User Story 1
• Fase 2: Implementação da User Story 2
• Fase 3: Implementação da User Story 4

Primeiro mostra me o plano de cada fase e só depois de eu aprovar implementas um de cada de vez