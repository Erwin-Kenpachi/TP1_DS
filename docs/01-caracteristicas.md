# Caracteristicas escolhidas

| Característica | Implícita / Explícita | Frase do Texto |
| :--- | :--- | :--- |
| **Disponibilidade** | Implícita | "O contrato de concessão aplica uma multa por cada hora em que mais de 10% das estações não conseguem desbloquear bicicletas." |
| **Robustez** | Implícita | "As docas comunicam por rede móvel e há zonas onde a rede falha com frequência. Cada doca guarda até 24 h de eventos localmente." |
| **Elasticidade a pedido** | Implícita | "Cerca de 70% dos desbloqueios acontecem entre as 8h00–9h30 e as 17h30–19h00. Em eventos (Santos Populares, maratona) a procura em algumas estações multiplica por 4." |
| **Arquivabilidade** | Explícita | "As faturas são comunicadas à Autoridade Tributária e guardadas durante 10 anos" |
| **Desempenho** | Implícita | "As elétricas enviam telemetria a cada 30 s em andamento." |
| **Privacidade** | Explícita | "O percurso de cada viagem é dado pessoal" |
| **Manutenibilidade** | Implícita | "Equipa de desenvolvimento de 6 pessoas e orçamento fixo." |

# Top 3
| Característica | Cenário Concreto |
| :--- | :--- |
| **Robustez** | • **Estímulo:** Um utilizador pretende devolver uma bicicleta numa doca cuja rede móvel se encontra temporariamente inoperacional.<br>• **Ambiente:** Operação em zonas com falhas frequentes de rede móvel nas estações.<br>• **Resposta:** O sistema garante que a doca valida e aceita o encaixe localmente em modo offline. O evento de finalização da viagem fica guardado de forma segura na memória local da doca (até 24 horas). Assim que a ligação for restabelecida, a doca sincroniza os dados com o servidor central, assegurando que não ocorre perda de informação. (Nota: Em caso de falha de rede ao desbloquear, o desbloqueamento torna-se impossível).<br>• **Medida de Resposta:** 100% de integridade dos dados sincronizados após o restabelecimento da rede, sem perda de registos de viagem. |
| **Elasticidade a pedido** | • **Estímulo:** Aumento expressivo do volume de viagens simultâneas e tentativas de acesso à aplicação e bicicletas (consoante horas de maior afluência — cerca de 70% dos desbloqueios concentrados entre as 8h00–9h30 e as 17h30–19h00 — ou eventos especiais como os Santos Populares e maratonas, onde a procura por estação multiplica por 4).<br>• **Ambiente:** Sistema a suportar a base instalada de ~60 000 assinantes e ~15 000 utilizadores ocasionais por mês, gerindo uma frota de 1 500 bicicletas (70% elétricas) distribuídas por 180 estações.<br>• **Resposta:** O sistema escala dinamicamente os recursos para absorver as variações sazonais e picos operacionais de tráfego.<br>• **Medida de Resposta:** Manter o tempo de resposta da aplicação estável e dentro dos limites aceitáveis durante os picos de afluência e momentos específicos de alta utilização. |
| **Disponibilidade** | • **Estímulo:** Ocorrência de falhas técnicas ou de comunicação prolongadas em múltiplas estações da rede.<br>• **Ambiente:** Sistema central de gestão de estações e desbloqueios a operar sob obrigações contratuais rigorosas de concessão municipal.<br>• **Resposta:** O subsistema central assegura resiliência e redundância operacional contínua para evitar interrupções sistémicas.<br>• **Medida de Resposta:** Garantir que mais de 90% das estações mantêm a capacidade operativa de desbloquear bicicletas sem interrupções prolongadas, prevenindo a aplicação de sanções financeiras severas previstas no contrato de concessão. |
