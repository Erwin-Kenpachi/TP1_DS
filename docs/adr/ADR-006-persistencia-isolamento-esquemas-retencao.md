# ADR-006: Isolamento Lógico de Esquemas de Dados e Política de Retenção Fiscal de 10 Anos

* **Estado:** **Aceite**
* **Data:** Semana 2
* **Contexto dos Documentos:** [docs/01-caracteristicas.md](../01-caracteristicas.md#L8-L10) e [docs/03-particionamento-e-vistas.md](../03-particionamento-e-vistas.md#L236-L256)

## Contexto
O enquadramento legal e contratual do *Lisboa Pedala* exige a comunicação obrigatória de todas as faturas à Autoridade Tributária (AT) e o seu arquivo seguro e imutável durante um período mínimo de 10 anos (característica explícita de **Arquivabilidade**). Adicionalmente, as viagens e histórico de localização representam dados pessoais de ciclistas sujeitos ao RGPD (característica explícita de **Privacidade**).
Por outro lado, permitir que vários módulos realizem consultas e *joins* SQL diretamente sobre as mesmas tabelas de base de dados geraria forte conascência de valor e identidade, destruindo as fronteiras do monólito modular a nível da camada de dados.

## Decisão
1. **Esquemas Lógicos Privados por Módulo de Domínio:**
   A persistência utiliza uma base de dados relacional comum (SQL), mas cada módulo de domínio tens as suas própias tabelas. Nenhum módulo pode efetuar queries diretas ou operações DDL sobre tabelas de outro módulo.
2. **Isolamento e Retenção Fiscal de 10 Anos em `tariff_billing`:**
   O módulo `tariff_billing` guarda registos imutáveis de cada fatura emitida com o respetivo código fiscal devolvido pela AT, garantindo que mesmo que uma conta de utilizador seja anonimizada no futuro, o documento fiscal permanece intacto pelo prazo legal.
3. **Proteção da Privacidade (RGPD):**
   Os dados de percurso e telemetria recebidos em `sensor_ingestion` são desvinculados de dados pessoais do utilizador na camada de dados, sendo a correlação efetuada unicamente via identificador opaco da viagem.

## Consequências
* **Positivas:**
  * **Conformidade Legal Estrita:** Cumprimento integral das obrigações fiscais perante a Autoridade Tributária e dos regulamentos de privacidade do RGPD;
  * **Proteção do Modelo de Dados:** Mudanças no esquema interno de uma tabela de um módulo não provocam falhas nos restantes;
  * **Preparação para Evolução Futura:** Se no futuro um módulo tiver de ser extraído para um serviço autónomo, o seu esquema de dados já se encontra perfeitamente delimitado e independente.
* **Negativas:**
  * Consultas analíticas ou agregadas (como relatórios municipais em `reporting_audit` ou a página unificada em `history_reader`) não podem efetuar *joins* SQL diretos entre `trips` e `invoices`, tendo de agregar a informação ao nível da camada de aplicação através das APIs públicas dos módulos.

## Alternativas Rejeitadas
1. **Base de Dados com Tabelas Partilhadas Livremente (*Shared Database Model*):** *Rejeitada.* Permitir *joins* e acessos diretos entre módulos geraria um acoplamento de dados crónico. Qualquer alteração de coluna ou índice por parte de um programador poderia quebrar queries noutros 3 módulos sem aviso.
2. **Bases de Dados Físicas Dedicadas por Módulo (*Database-per-module* com múltiplas instâncias):** *Rejeitada.* Introduziria complexidade operacional excessiva (múltiplas ligações, múltiplos backups, custos adicionais de servidores e impossibilidade de transações atómicas locais simples) para a escala do sistema e a capacidade da equipa.
