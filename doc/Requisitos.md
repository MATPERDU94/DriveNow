# DriveNow — Requisitos

20/11/2026 · Mateus

Requisitos do DriveNow, serviço de partilha de carros elétricos em Guimarães, com o estado de cada um a 20/11. A explicação sem linguagem técnica está em `doc/Funcionamento_DriveNow.md`, e os fluxos nos diagramas 1 a 7 (`doc/Diagramas`).

Mudanças desde 16/11, decididas na PL de 18/11: a aplicação é feita em React (em vez do SAP Build Apps); os pagamentos usam só cartões fictícios; o Moloni fica em pausa até a série estar registada na AT; juntam-se o preço por km, o trajeto da viagem, as revisões por km e a oficina com stock de peças.

## Requisitos funcionais

O estado refere-se ao backend, às integrações e à aplicação React, testados por scripts e na própria aplicação.

| ID | Requisito | Estado |
| --- | --- | --- |
| RF01 | Criar conta com nome, email, NIF válido, telemóvel, password (mínimo 8 caracteres) e aceitação dos termos, associando um cartão ou uma conta bancária | Implementado |
| RF02 | Iniciar sessão com email e password; cada cliente só acede aos seus dados | Implementado |
| RF03 | Ver os carros livres no mapa, do mais perto para o mais longe (GPS), com distância, taxa de entrada, preço por minuto e preço por km | Implementado |
| RF04 | Esconder os carros reservados, em reparação, com a revisão por fazer ou com documentos caducados | Implementado |
| RF05 | Reservar um carro, depois de confirmar o preço (taxa de entrada, preço por minuto, preço por km e um exemplo): uma reserva ativa de cada vez, com meio de pagamento, sem dívidas e com o carro disponível; preços e km iniciais fixados na reserva; tempo a contar desde a reserva | Implementado |
| RF06 | Cancelar a reserva antes da viagem, pagando a taxa de entrada e os minutos (0 km) | Implementado |
| RF07 | Indicar o estado do carro no início e no fim da viagem: "sem danos" ou tipo de dano e descrição | Implementado |
| RF08 | Ver o tempo e o valor da viagem em curso (os km somam-se no fim) | Implementado |
| RF09 | Terminar a viagem, indicando onde deixou o carro e os km do painel (obrigatórios; não podem ser inferiores aos do início nem dar mais de 1000 km) | Implementado |
| RF10 | Calcular o preço: taxa de entrada + minutos × preço por minuto + km da viagem × preço por km + penalizações, com IVA de 23% incluído; cada minuto começado conta inteiro; km da viagem = km do painel no fim − km do carro na reserva | Implementado |
| RF11 | Cobrar automaticamente no meio de pagamento (simulado, só com cartões fictícios); um pagamento recusado gera uma dívida que bloqueia reservas e é cobrada quando o cliente atualiza o meio de pagamento | Implementado |
| RF12 | Emitir a fatura-recibo no Moloni (com linhas de taxa, minutos, km e danos) e enviá-la por email; se o Moloni falhar, a fatura fica pendente e é emitida depois | Em pausa |
| RF13 | Danos: custo da tabela do Odoo; paga o próprio cliente (relato de fim) ou o condutor anterior (relato de início); dano já registado e por reparar não conta outra vez; máximo de 1000 € (franquia) por viagem; danos que imobilizam tiram o carro do mapa até à reparação | Implementado |
| RF14 | Documentos dos carros (seguro, inspeção, IUC): aviso ao gestor 30 dias antes de caducar; carro bloqueado se caducado; volta ao mapa quando é renovado | Implementado |
| RF15 | Ver o histórico de viagens (duração, km e valor) e as faturas | Parcial |
| RF16 | Editar os dados pessoais e o meio de pagamento no perfil | Implementado |
| RF17 | Ecrãs da aplicação em React, conforme os mockups: entrar, criar conta, mapa, confirmação, viagem, viagem terminada, viagens e faturas, perfil | Implementado |
| RF18 | Trajeto da viagem: no resumo, o percurso pelas ruas entre onde o carro estava na reserva e onde o cliente o deixou (simulado, sem seguir o carro por GPS); linha reta se o serviço de rotas não responder | Implementado |
| RF19 | Revisões por km: todos os carros vão à oficina a cada 15 000 km; ao chegar aos km da revisão, o carro sai do mapa ("Revisão por fazer") e é criada uma ordem de revisão no Odoo; ao concluí-la, a próxima revisão passa para os km atuais + 15 000; o gestor vê no Odoo os carros a menos de 1 000 km da revisão | Implementado |
| RF20 | Oficina DriveNow e stock de peças no Odoo (Inventário e Compras): 11 peças com stock mínimo e máximo; entradas por compras a fornecedores; saídas nas revisões e reparações; proposta de compra quando o stock desce abaixo do mínimo | Por fazer |

RF12: a fatura fica "Pendente" na aplicação até o Moloni voltar. RF15: as faturas dependem do RF12.

## Requisitos de integração

Cada tipo de dado tem um sistema de referência, e o n8n leva os eventos do backend aos outros sistemas.

| ID | Requisito | Estado |
| --- | --- | --- |
| RI01 | Cliente registado → criar o cliente (Business Partner) no SAP, pelo n8n (`MD_BUSINESSPARTNER_SRV`) | Parcial |
| RI02 | Registar as vendas no SAP | Bloqueado |
| RI03 | Moloni: criar o cliente, emitir a fatura-recibo e enviar o PDF por email | Em pausa |
| RI04 | Odoo: ler a frota, o tarifário (taxa, preço por minuto e preço por km), os danos, os documentos e as revisões (API JSON-2) | Implementado |
| RI05 | Viagem terminada → atualizar a quilometragem no Odoo, pelo n8n | Implementado |
| RI06 | Dano reportado → registar o dano no Odoo, pelo n8n, devolvendo o número do registo | Implementado |
| RI07 | Todos os dias → procurar documentos a caducar e alertar o gestor no Odoo, pelo n8n | Implementado |
| RI08 | Sistema de referência: clientes no SAP; faturas no Moloni; viaturas, tarifário, danos, documentos, revisões, peças e stock no Odoo; reservas, viagens, km, trajetos, pagamentos e posições no backend | Implementado |
| RI09 | A aplicação React fala com o backend pela API REST (JSON), com o token no cabeçalho | Implementado |
| RI10 | Fim da viagem → pedir o percurso ao serviço de rotas (OSRM) e guardá-lo | Implementado |
| RI11 | Viagem terminada → verificar a revisão por km, avisar a 1 000 km e criar a ordem de revisão no Odoo, pelo n8n (diagrama 6) | Parcial |
| RI12 | Stock abaixo do mínimo → proposta de compra no Odoo e alerta ao gestor, pelo n8n (diagrama 7) | Por fazer |

RI01: o cliente fica em rascunho, porque a ativação falha por uma configuração em falta no s20. RI02: não há serviço de vendas ativo no s20. RI03: falta registar a série na AT. RI05 e RI06: sem o n8n, o backend grava os km e os danos diretamente no Odoo. RI11: sem o n8n, é o backend que cria a ordem de revisão; falta o fluxo do n8n e o aviso a 1 000 km.

## Requisitos não funcionais

| ID | Requisito | Estado |
| --- | --- | --- |
| RNF01 | Segurança: login com token (JWT, válido 24 h), passwords cifradas (BCrypt) e pedidos sem token recusados (HTTP 401) | Implementado |
| RNF02 | Privacidade: do cartão só se guardam os últimos 4 dígitos, e cada cliente só vê os seus dados | Implementado |
| RNF03 | Chaves e palavras-passe (Odoo, Moloni, SAP) fora do Git: ficheiro `.env` e cofres de credenciais do n8n e do Postman | Implementado |
| RNF04 | Resiliência: uma falha do Moloni, do n8n ou do serviço de rotas não impede reservar nem terminar a viagem (faturas pendentes; km, danos e revisões gravados diretamente no Odoo; trajeto em linha reta) | Implementado |
| RNF05 | Tempos limite em todas as chamadas externas: Odoo e Moloni 10 s para ligar e 30 s para responder; n8n 5 s e 10 s; serviço de rotas 3 s e 6 s | Implementado |
| RNF06 | CORS: a API só aceita pedidos de origens autorizadas | Implementado |
| RNF07 | Portabilidade: Odoo, backend, PostgreSQL e n8n correm em Docker | Implementado |
| RNF08 | Rastreabilidade: código, diagramas, coleção do Postman e fluxos do n8n no GitHub | Implementado |
| RNF09 | Testabilidade: scripts de teste de ponta a ponta (`testar_fluxo.sh`, `testar_n8n.sh`, `testar_dano_n8n.sh`) e de preparação de testes (`bloquear_carros.py`, `revisoes.py`) | Implementado |
| RNF10 | Usabilidade: aplicação em português, pensada primeiro para telemóvel, com as regras e os preços visíveis antes de reservar | Implementado |
| RNF11 | Demonstração sem servidores: modo demonstração da aplicação, com as mesmas regras e mensagens do backend, também num só ficheiro HTML | Implementado |
| RNF12 | Serviços públicos usados com as regras deles: o pedido ao serviço de rotas identifica a aplicação, e a aplicação mostra "OSRM · © OpenStreetMap" | Implementado |
