# DriveNow — Plano do projeto (atualizado)

16/11/2026 · Mateus

## Ponto de situação

O núcleo está feito: Odoo, backend, n8n com 4 fluxos e a ligação SAP Build Apps → túnel → backend. Faltam a fatura no Moloni (à espera da AT), a ativação do cliente no SAP (configuração do servidor) e os ecrãs da aplicação.

**Resultado da reunião com o docente**

- Aceitou a arquitetura proposta: SAP Build Apps → backend Spring Boot → n8n → SAP, Moloni e Odoo. Disse que está muito bem e que é um desafio bastante interessante.
- Considerou excelente a ideia de usar o Odoo para gerir a frota.
- Quer saber como as pessoas vão reservar as viaturas. A resposta é o diagrama 2 (Reserva e viagem), explicado em linguagem simples no documento novo.
- Pediu um ficheiro `.md` que explique, sem linguagem técnica, como funcionam as funcionalidades básicas: `doc/Funcionamento_DriveNow.md` (feito, ver Passo 2).

## Passo 1 — Acessos aos sistemas

Todos os sistemas estão acessíveis, exceto a emissão de faturas no Moloni, que espera o registo da série na AT.

| Sistema | Estado | O que temos |
| --- | --- | --- |
| Odoo 19 Community (Docker) | Feito | Frota de 30 viaturas, tarifário, danos, documentos (seguro, inspeção, IUC) e chave API |
| SAP s20 (cliente 305) | Feito | Utilizador LEARN-091; OData testado no Postman e usado pelo n8n |
| SAP BTP trial e SAP Build Apps | Feito | Conta trial, login próprio (Cloud Identity Services), permissões BuildApps e projeto DriveNow |
| Túnel (Cloudflare) | Feito | Túnel rápido; o endereço muda a cada arranque, falta um endereço fixo |
| n8n (Docker) | Feito | `infra/n8n`, com 4 fluxos publicados |
| Moloni (conta de desenvolvimento) | Bloqueado | Falta o subutilizador AT para registar a série; pedido ao Prof. Rui Dinis |

Em vez da chave do api.sap.com, usamos o sistema SAP s20 da universidade. O React foi abandonado; o Spring Boot mantém-se como backend. A edição gratuita do SAP Build Apps foi anunciada como descontinuada a 30/09/2026: ainda funciona, mas convém exportar o projeto com frequência.

## Passo 2 — Documentação

Os diagramas foram refeitos para a arquitetura v2 e já existe o documento não técnico pedido pelo docente.

| Documento | Estado | Onde está |
| --- | --- | --- |
| Funcionamento da DriveNow (não técnico, pedido na reunião) | Feito | `doc/Funcionamento_DriveNow.md` |
| Diagrama de arquitetura v2 | Feito | `doc/Arquitetura - DriveNow.puml` e `.png` |
| Diagramas de atividade v2: 1 Registo, 2 Reserva e viagem, 3 Pagamento e faturação, 4 Danos e penalizações, 5 Documentação dos carros | Feito | `doc/Diagramas/` (`.puml` e `.png`) |
| Mockups (versão inicial) | Feito | `doc/Mockups/`; rever quando os ecrãs forem feitos no SAP Build Apps |
| Perguntas para a reunião | Feito | `doc/Reuniao_perguntas_docente.md` |
| Guia de instalação do Odoo e README do backend | Feito | `info_odoo_instalation.md` e `src/backend/README.md` |

Falta confirmar o estado da caracterização do negócio (Fase 1) para o relatório.

## Passo 3 — Arquitetura e cenários de integração (v2)

A arquitetura v2, aceite pelo docente, segue o cenário hub: a app fala só com o backend, e o n8n liga o backend ao SAP, ao Moloni e ao Odoo.

![Arquitetura v2](Arquiteture%20-%20DriveNow.png)

A reserva de uma viatura (a pergunta do docente) só passa pela app, pelo backend e pelo Odoo; está no diagrama 2 e no documento não técnico.

Em relação ao plano inicial, o SAP deixou de guardar a frota, porque os produtos do s20 são da Global Bike. Passou a ser a referência dos clientes, e a frota foi para o Odoo. O React deu lugar ao SAP Build Apps.

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
| Moloni: fatura-recibo | Bloqueada: série sem registo na AT |

## Passo 5 — Implementação

Odoo, backend e n8n estão feitos e testados de ponta a ponta, com scripts de teste no repositório. Faltam a fatura no Moloni, a ativação do cliente no SAP e os ecrãs da aplicação.

| Parte | Estado | Detalhe |
| --- | --- | --- |
| Odoo: frota e tarifário | Feito | 30 viaturas em Guimarães; taxa de entrada e preço por minuto por modelo |
| Odoo: módulo de danos | Feito | 16 tipos de dano com custo e gravidade; alguns imobilizam o carro |
| Odoo: documentos dos carros (diagrama 5) | Feito | Seguro, inspeção e IUC dos 30 carros (`scripts/odoo_documentos.py`) |
| Backend: contas e login | Feito | Cada cliente só vê as suas reservas |
| Backend: mapa de carros próximos | Feito | Esconde carros reservados, em reparação ou com documentos caducados |
| Backend: reserva, viagem e preço | Feito | Temporizador, cancelamento, preços guardados na reserva |
| Backend: relatos e danos | Feito | Responsável, franquia de 1000 €, carro "em reparação" |
| Backend: pagamentos simulados | Feito | Testado com `scripts/testar_fluxo.sh`: aprovado, recusado, dívida e nova cobrança |
| Backend: faturação no Moloni | Bloqueado | Código feito; série sem registo na AT |
| n8n: cliente registado → SAP | Feito | Cliente em rascunho, com papel e morada |
| n8n: viagem terminada → Odoo (km) | Feito | Se o n8n falhar, o backend grava os km diretamente |
| n8n: dano reportado → Odoo | Feito | Devolve ao backend o id do registo do dano |
| n8n: documentos dos carros → alerta diário | Feito | Todos os dias às 8h, nota no carro para o gestor de frota |
| n8n: viagem paga → Moloni e email | Bloqueado | Depende do registo da série na AT |
| SAP: ativar cliente e registar vendas | Bloqueado | Configuração em falta e serviços de vendas inativos no s20 |
| SAP Build Apps: prova de conceito | Feito | Lista com os carros reais, via túnel e backend |
| SAP Build Apps: ecrãs | Por fazer | 7 ecrãs dos mockups e 2 novos (estado do carro no início e no fim) |

## Pendentes e decisões em aberto

O Moloni continua a ser o bloqueio mais urgente, e o SAP tem duas perguntas para o docente. Levar à PL de 18/11/2026 (perguntas também em `doc/info.md`).

- [ ] **Moloni e AT:** o passo "Comunicação automática à AT de documentos e séries" pede um subutilizador das Finanças da empresa 278993281\_UM, que não temos. Pedido enviado ao Prof. Rui Dinis.
- [ ] **SAP, ativar o cliente:** a ativação devolve "Error\_Time Dependency\_Addresses CHECK\_TABLE\_MISSING" e a `API_BUSINESS_PARTNER` não está ativa. Chega o cliente em rascunho?
- [ ] **SAP, vendas:** que serviço OData usar, já que os de vendas dão 404?
- [ ] **SAP Build Apps:** a edição gratuita foi anunciada como descontinuada a 30/09/2026. Há uma conta da universidade?
- [ ] **Pagamentos:** a simulação (cartão de teste que aprova ou recusa) é aceite?
- [ ] **Entrega:** estrutura do relatório técnico; a demonstração pode ser no preview web do SAP Build Apps?

## Próximos passos e divisão de tarefas

Com as integrações feitas, o foco passa para a aplicação no SAP Build Apps e para a documentação. Os responsáveis são uma proposta; ajustar à vontade.

| # | Tarefa | Responsável (proposta) | Estado |
| --- | --- | --- | --- |
| 1 | Ecrãs no SAP Build Apps a partir dos mockups: login, criar conta, mapa, viagem, viagem terminada, viagens e faturas, perfil | Membro 3 e Mateus | Por fazer |
| 2 | Ecrãs novos: estado do carro no início e no fim da viagem | Membro 3 | Por fazer |
| 3 | Endereço fixo para o túnel (ou alternativa) | Mateus | Por fazer |
| 4 | Fluxo viagem paga → Moloni e email, quando a série estiver registada na AT | Membro 1 | Bloqueado |
| 5 | Ativar o cliente no SAP, se o docente indicar como | Membro 1 | Bloqueado |
| 6 | Relatório técnico: arquitetura, diagramas, integrações e testes | Membro 2 | Por fazer |
| 7 | Pasta partilhada: gestão, diário de bordo, atas e requisitos | Mateus | Em curso |

## Passo 6 — Entrega

A entrega mantém-se: relatório técnico, vídeo de demonstração e apresentação pública. Falta saber as datas de cada entrega.

- **Relatório técnico:** caracterização, arquitetura v2, diagramas 1 a 5, funcionamento não técnico, testes no Postman e decisões (Odoo para a frota, vendas no SAP).
- **Vídeo:** um percurso completo na aplicação: registo, reserva, viagem com dano, pagamento e fatura no Moloni, com o Odoo e o SAP a mostrar o resultado.
- **Apresentação pública:** o problema, a arquitetura e a demonstração.
- **Repositório:** código, `doc/`, `postman/`, CSV e cópia de segurança do Odoo, sem o ficheiro `.env`.
