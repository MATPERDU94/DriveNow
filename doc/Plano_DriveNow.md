# DriveNow — Plano do projeto (atualizado)

20/11/2026 · Mateus

## Ponto de situação

A aplicação está feita em React e ligada ao backend. Odoo, backend e n8n estão testados de ponta a ponta. Depois da PL de 18/11 juntaram-se o preço por km, o trajeto da viagem e as revisões por km, já feitos. Faltam a oficina com stock de peças no Odoo, os fluxos novos do n8n, a fatura no Moloni (à espera da AT) e a ativação do cliente no SAP.

**Reunião de 11/11 com o docente**

- Aceitou a arquitetura proposta e considerou excelente a ideia de usar o Odoo para gerir a frota.
- Pediu um ficheiro `.md` que explique, sem linguagem técnica, como funcionam as funcionalidades básicas: `doc/Funcionamento_DriveNow.md`.

**PL de 18/11**

- A aplicação pode ser em React (em vez do SAP Build Apps).
- O Moloni fica em pausa: o registo na AT é tratado pelo regente.
- Os pagamentos usam cartões fictícios, porque não é para fazer transações reais.
- O tempo conta desde a reserva, com uma confirmação antes.
- A acrescentar: preço por km (km final − km inicial), trajeto simulado, revisões por km e oficina própria com entradas e saídas de peças em armazém.
- Para a próxima aula: definir o que cada sistema faz.

## Passo 1 — Acessos aos sistemas

Todos os sistemas estão acessíveis, exceto a emissão de faturas no Moloni, que espera o registo da série na AT.

| Sistema | Estado | O que temos |
| --- | --- | --- |
| Odoo 19 Community (Docker) | Feito | Frota de 30 viaturas, tarifário (taxa, preço por minuto e por km), danos, documentos, revisões por km e chave API |
| SAP s20 (cliente 305) | Feito | Utilizador LEARN-091; OData testado no Postman e usado pelo n8n |
| n8n (Docker) | Feito | `infra/n8n`, com 4 fluxos publicados; falta configurá-lo no portátil |
| Serviço de rotas OSRM (público) | Feito | Percurso pelas ruas para o trajeto das viagens |
| Moloni (conta de desenvolvimento) | Em pausa | Falta o subutilizador AT para registar a série; tratado pelo regente |
| SAP BTP trial e SAP Build Apps | Abandonado | Prova de conceito feita a 11/11; a app passou a React (PL de 18/11) |

Em vez da chave do api.sap.com, usamos o sistema SAP s20 da universidade. Com a app em React deixou de ser preciso o túnel Cloudflare.

## Passo 2 — Documentação

A documentação foi revista para a arquitetura v3, com as alterações da PL de 18/11.

| Documento | Estado | Onde está |
| --- | --- | --- |
| Funcionamento da DriveNow (não técnico) | Feito (atualizado a 20/11) | `doc/Funcionamento_DriveNow.md` |
| Requisitos | Feito (atualizado a 20/11) | `doc/Requisitos.md` |
| Diagrama de arquitetura v3 | Feito | `doc/Arquitetura/Arquitetura DriveNow V3.png` (e código) |
| Diagramas de atividade v3: 1 Registo, 2a Procurar e reservar, 2b Viagem e fim, 3 Pagamento e faturação, 4a Danos (registo e cobrança), 4b Danos (reparação), 5 Documentação, 6 Revisões por km, 7 Armazém e compras | Feito | `doc/Diagramas/` (`.png` e `src/`); versões antigas em `old version/` |
| Oficina, revisões e peças | Feito | `doc/Oficina, revisões e peças.md` |
| Mockups | Por rever | `doc/Mockups/`; falta juntar o preço por km, os km e o trajeto |
| Diário de bordo e atas | Em curso | `doc/Diario_de_bordo.md` (falta 17 a 20/11) e `doc/Atas/` (falta a de 18/11) |
| Guia de instalação do Odoo e README do backend e da app | Feito | `info_odoo_instalation.md`, `src/backend/README.md` e `src/frontend/README.md` |

Falta confirmar o estado da caracterização do negócio (Fase 1) para o relatório.

## Passo 3 — Arquitetura e cenários de integração (v3)

A arquitetura segue o cenário hub: a app fala só com o backend, e o n8n liga o backend ao SAP, ao Moloni e ao Odoo.

![Arquitetura v3](Arquitetura/Arquitetura%20DriveNow%20V3.png)

Mudanças em relação à v2:

- A app passou a React.
- O Moloni fica em pausa.
- O backend calcula os km de cada viagem e pede o trajeto a um serviço de rotas externo (OSRM).
- O Odoo passa a ter duas partes:
  - **Frota**: viaturas, tarifário, km, revisões, danos e documentos;
  - **Inventário e Compras**: armazém da oficina, peças, compras a fornecedores e saídas nas revisões e reparações.

O SAP continua a ser a referência dos clientes, porque os produtos do s20 são da Global Bike.

## Passo 4 — Provar que as APIs funcionam (Postman)

O SAP aceita criar clientes pela API em rascunho, com papel e morada, mas a ativação falha por uma configuração em falta no s20. A coleção está no repositório, em `postman/`, sem palavras-passe.

| Pedido | Resultado |
| --- | --- |
| SAP: ler Business Partners (`MD_BUSINESSPARTNER_SRV`) | 200 OK |
| SAP: produtos (`UI_PRODUCTLIST`) e catálogo de serviços | 200 OK |
| SAP: obter token CSRF | OK |
| SAP: criar cliente em rascunho (`C_BusinessPartner`) | 201 Created |
| SAP: papel de cliente `FLCU01` (e `ForEdit` por PATCH) | OK; o papel geral `000000` não está configurado no s20 |
| SAP: morada do cliente | OK |
| SAP: ativar o cliente (`C_BusinessPartnerActivation`) | Erro do servidor: "Error\_Time Dependency\_Addresses CHECK\_TABLE\_MISSING" |
| SAP: `API_BUSINESS_PARTNER` | 403, serviço não ativo |
| SAP: vendas (`API_SALES_QUOTATION_SRV`, `SD_SALES_ORDER_IMPORT`) | 404, serviço não ativo |
| Moloni: login, empresas, séries e clientes | OK |
| Moloni: fatura-recibo | Em pausa: série sem registo na AT |

## Passo 5 — Implementação

A app, o backend, o Odoo e o n8n estão feitos e testados, com scripts de teste no repositório. Faltam a oficina com stock, os fluxos novos do n8n, a fatura no Moloni e a ativação do cliente no SAP.

| Parte | Estado | Detalhe |
| --- | --- | --- |
| Odoo: frota e tarifário | Feito | 30 viaturas em Guimarães; taxa de entrada, preço por minuto e preço por km por modelo (0,10 a 0,40 €/km) |
| Odoo: módulo de danos | Feito | 16 tipos de dano com custo e gravidade; alguns imobilizam o carro |
| Odoo: documentos dos carros (diagrama 5) | Feito | Seguro, inspeção e IUC dos 30 carros (`scripts/odoo_documentos.py`) |
| Odoo: revisões por km (diagrama 6) | Feito | Km da próxima revisão em cada carro (a cada 15 000 km); menu Frota → Revisões; ao concluir uma ordem de revisão, a próxima passa para +15 000 km |
| Odoo: Inventário e Compras (diagrama 7) | Por fazer | Armazém "Oficina DriveNow", 11 peças com mínimo e máximo, 2 fornecedores, saídas nas revisões e reparações |
| Backend: contas e login | Feito | Cada cliente só vê as suas reservas |
| Backend: mapa de carros próximos | Feito | Esconde carros reservados, em reparação, com revisão por fazer ou com documentos caducados |
| Backend: reserva, viagem e preço | Feito | Temporizador, cancelamento, preços e km iniciais guardados na reserva; km do painel obrigatórios no fim; preço com km |
| Backend: trajeto da viagem | Feito | Percurso pelas ruas pedido ao OSRM no fim da viagem; linha reta se não responder |
| Backend: revisões por km | Feito | Sem o n8n, cria a ordem de revisão no Odoo quando o carro chega aos km da revisão (`scripts/revisoes.py` para testar) |
| Backend: relatos e danos | Feito | Responsável, franquia de 1000 €, carro "em reparação" |
| Backend: pagamentos simulados | Feito | Cartões fictícios; testado com `scripts/testar_fluxo.sh`: aprovado, recusado, dívida e nova cobrança |
| Backend: faturação no Moloni | Em pausa | Código feito (com a linha dos km, artigo `DN-KM`); série sem registo na AT |
| App React | Feito | Entrar, criar conta, mapa, confirmação da reserva, viagem, estado do carro no início e no fim, viagem terminada com trajeto, viagens e faturas, perfil; modo demonstração e versão num só ficheiro |
| n8n: cliente registado → SAP | Feito | Cliente em rascunho, com papel e morada |
| n8n: viagem terminada → Odoo (km) | Feito | Se o n8n falhar, o backend grava os km diretamente |
| n8n: dano reportado → Odoo | Feito | Devolve ao backend o id do registo do dano |
| n8n: documentos dos carros → alerta diário | Feito | Todos os dias às 8h, nota no carro para o gestor de frota |
| n8n: revisões por km (diagrama 6) | Por fazer | Verificar a revisão depois de cada viagem e avisar a 1 000 km |
| n8n: stock mínimo de peças (diagrama 7) | Por fazer | Alerta ao gestor quando há proposta de compra |
| n8n: viagem paga → Moloni e email | Em pausa | Depende do registo da série na AT |
| SAP: ativar cliente e registar vendas | Bloqueado | Configuração em falta e serviços de vendas inativos no s20 |

## Pendentes e decisões em aberto

Levar à próxima PL (perguntas também em `doc/info.md`).

- [ ] **Moloni e AT:** o registo da série pede um subutilizador das Finanças da empresa 278993281\_UM; tratado pelo regente.
- [ ] **SAP, ativar o cliente:** a ativação devolve "Error\_Time Dependency\_Addresses CHECK\_TABLE\_MISSING" e a `API_BUSINESS_PARTNER` não está ativa. Chega o cliente em rascunho?
- [ ] **SAP, vendas:** que serviço OData usar, já que os de vendas dão 404?
- [ ] **O que cada sistema faz:** documento pedido para a próxima aula.
- [ ] **Entrega:** estrutura do relatório técnico e datas.
- [x] **App:** React aceite (PL de 18/11).
- [x] **Pagamentos:** cartões fictícios, sem transações reais (PL de 18/11).

## Próximos passos e divisão de tarefas

Os responsáveis são uma proposta; ajustar à vontade.

| # | Tarefa | Responsável (proposta) | Estado |
| --- | --- | --- | --- |
| 1 | Oficina e stock no Odoo: instalar Inventário e Compras; armazém, 11 peças, 2 fornecedores, mínimo e máximo (por script) | Mateus | Por fazer |
| 2 | n8n no portátil; fluxo viagem terminada com a verificação das revisões e o aviso a 1 000 km; alerta de stock mínimo | Membro 1 | Por fazer |
| 3 | Documento "o que cada sistema faz" para a PL | Membro 2 | Por fazer |
| 4 | Mockups com o preço por km, os km e o trajeto | Membro 3 | Por fazer |
| 5 | Diário de bordo (17 a 20/11) e ata de 18/11 | Mateus | Por fazer |
| 6 | Fluxo viagem paga → Moloni e email, quando a série estiver registada na AT | Membro 1 | Em pausa |
| 7 | Ativar o cliente no SAP, se o docente indicar como | Membro 1 | Bloqueado |
| 8 | Relatório técnico: arquitetura, diagramas, integrações e testes | Membro 2 | Por fazer |

## Passo 6 — Entrega

A entrega mantém-se: relatório técnico, vídeo de demonstração e apresentação pública. Falta saber as datas de cada entrega.

- **Relatório técnico:**
  - caracterização e arquitetura v3;
  - diagramas 1 a 7 e o funcionamento não técnico;
  - requisitos e testes (Postman e scripts);
  - decisões: Odoo para a frota e a oficina, React, Moloni em pausa, cartões fictícios, revisões a cada 15 000 km.
- **Vídeo:** um percurso completo na aplicação, com o Odoo e o SAP a mostrar o resultado:
  - registo, reserva com confirmação;
  - viagem com dano, km e trajeto;
  - pagamento;
  - um carro que chega à revisão e volta ao mapa depois de a oficina a concluir.
- **Apresentação pública:** o problema, a arquitetura e a demonstração. Como plano B, a app em modo demonstração (num só ficheiro, sem servidores).
- **Repositório:** código, `doc/`, `postman/`, CSV e cópia de segurança do Odoo, sem o ficheiro `.env`.
