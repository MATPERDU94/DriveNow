# DriveNow — Diário de bordo

16/11/2026 · Mateus

Registo do trabalho do Grupo 1 (PL5) no projeto DriveNow de IAIE, do mais recente para o mais antigo.

| Data | O que foi feito | Decisões e problemas |
| --- | --- | --- |
| 16/11 | Diagrama 5: documentos dos carros (seguro, inspeção e IUC) no Odoo, 90 registos; bloqueio no backend; alerta diário no n8n; renovação testada | Os documentos ficam como contratos da viatura no Odoo; o alerta é uma nota no carro, sem email |
| 16/11 | n8n instalado em Docker, com os fluxos cliente registado → SAP, viagem terminada → Odoo e dano reportado → Odoo; fluxos exportados para `infra/n8n/workflows` | O backend não recebia resposta do n8n (o Java pedia HTTP/2): forçado HTTP/1.1. Se o n8n falhar, o backend grava km e danos diretamente |
| 16/11 | SAP: cliente criado pela API em rascunho, com papel de cliente (FLCU01) e morada; coleção do Postman exportada para o repositório | A ativação falha com "CHECK\_TABLE\_MISSING" (configuração do s20) e a `API_BUSINESS_PARTNER` não está ativa: pergunta para o docente |
| 11/11 | SAP BTP trial e SAP Build Apps: conta, login próprio, permissões e projeto DriveNow; túnel Cloudflare e CORS no backend; primeiro ecrã com os carros reais | O booster automático falhou no identity provider e a configuração foi feita à mão. A edição gratuita do Build Apps foi anunciada como descontinuada |
| 11/11 | Pagamentos simulados testados (`scripts/testar_fluxo.sh`): aprovado, recusado, dívida que bloqueia reservas e nova cobrança | Frontend adiado; prioridade às integrações |
| 11/11 | Moloni: dados da empresa preenchidos | O registo da série na AT pede um subutilizador das Finanças: pedido ao Prof. Rui Dinis |
| 11/11 | Reunião com o docente; plano atualizado; documento não técnico `doc/Funcionamento_DriveNow.md` | Arquitetura v2 aceite; Odoo para a frota considerado excelente. Ver ata de 11/11 |
| 05/11 a 10/11 | Odoo 19 em Docker com 30 viaturas e módulo `drivenow_danos` (danos e tarifário); backend Spring Boot (mapa GPS, reservas, relatos, danos, preço); Moloni ligado; Postman com o SAP; diagramas 1 a 5 refeitos | React substituído pelo SAP Build Apps; n8n para orquestrar; Odoo como referência das viaturas porque os produtos do s20 são da Global Bike |
| 04/11 | Plano de arranque do projeto: passos 1 a 6 e divisão inicial de tarefas | Arquitetura inicial com React e Spring Boot |

As datas entre 05/11 e 10/11 estão agrupadas; acertar o dia de cada tarefa, se for preciso.
