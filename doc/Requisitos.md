# DriveNow — Requisitos

16/11/2026 · Mateus

Requisitos do DriveNow, serviço de partilha de carros elétricos em Guimarães, com o estado de cada um a 16/11. A explicação sem linguagem técnica está em `doc/Funcionamento_DriveNow.md`, e os fluxos nos diagramas 1 a 5.

## Requisitos funcionais

O estado refere-se ao backend e às integrações, testados por scripts. Os ecrãs da aplicação estão por fazer (RF17).

| ID | Requisito | Estado |
| --- | --- | --- |
| RF01 | Criar conta com nome, email, NIF válido, telemóvel, password (mínimo 8 caracteres) e aceitação dos termos, associando um cartão ou uma conta bancária | Implementado |
| RF02 | Iniciar sessão com email e password; cada cliente só acede aos seus dados | Implementado |
| RF03 | Ver os carros livres do mais perto para o mais longe (GPS), com distância, taxa de entrada e preço por minuto | Implementado |
| RF04 | Esconder os carros reservados, em reparação ou com documentos caducados | Implementado |
| RF05 | Reservar um carro: uma reserva ativa de cada vez, com meio de pagamento, sem dívidas e com o carro disponível; preços fixados e tempo a contar desde a reserva | Implementado |
| RF06 | Cancelar a reserva antes da viagem, pagando a taxa de entrada e os minutos | Implementado |
| RF07 | Indicar o estado do carro no início e no fim da viagem: "sem danos" ou tipo de dano e descrição | Implementado |
| RF08 | Ver o tempo e o valor da viagem em curso | Implementado |
| RF09 | Terminar a viagem, registando a localização e a quilometragem | Implementado |
| RF10 | Calcular o preço: taxa de entrada + minutos × preço por minuto + penalizações, com IVA de 23% incluído; cada minuto começado conta inteiro | Implementado |
| RF11 | Cobrar automaticamente no meio de pagamento (simulado); um pagamento recusado gera uma dívida que bloqueia reservas e é cobrada quando o cliente atualiza o meio de pagamento | Implementado |
| RF12 | Emitir a fatura-recibo no Moloni e enviá-la por email; se o Moloni falhar, a fatura fica pendente e é emitida depois | Bloqueado |
| RF13 | Danos: custo da tabela do Odoo; paga o próprio cliente (relato de fim) ou o condutor anterior (relato de início); dano já registado e por reparar não conta outra vez; máximo de 1000 € (franquia) por viagem; danos que imobilizam tiram o carro do mapa até à reparação | Implementado |
| RF14 | Documentos dos carros (seguro, inspeção, IUC): aviso ao gestor 30 dias antes de caducar; carro bloqueado se caducado; volta ao mapa quando é renovado | Implementado |
| RF15 | Ver o histórico de viagens e faturas | Parcial |
| RF16 | Editar os dados pessoais e o meio de pagamento no perfil | Implementado |
| RF17 | Ecrãs da aplicação no SAP Build Apps, conforme os mockups | Por fazer |

## Requisitos de integração

Cada tipo de dado tem um sistema de referência, e o n8n leva os eventos do backend aos outros sistemas.

| ID | Requisito | Estado |
| --- | --- | --- |
| RI01 | Cliente registado → criar o cliente (Business Partner) no SAP, pelo n8n (`MD_BUSINESSPARTNER_SRV`) | Parcial |
| RI02 | Registar as vendas no SAP | Bloqueado |
| RI03 | Moloni: criar o cliente, emitir a fatura-recibo e enviar o PDF por email | Bloqueado |
| RI04 | Odoo: ler a frota, o tarifário, os danos e os documentos (API JSON-2) | Implementado |
| RI05 | Viagem terminada → atualizar a quilometragem no Odoo, pelo n8n | Implementado |
| RI06 | Dano reportado → registar o dano no Odoo, pelo n8n, devolvendo o número do registo | Implementado |
| RI07 | Todos os dias → procurar documentos a caducar e alertar o gestor no Odoo, pelo n8n | Implementado |
| RI08 | Sistema de referência: clientes no SAP; faturas no Moloni; viaturas, tarifário, danos e documentos no Odoo; reservas, viagens, pagamentos e posições no backend | Implementado |
| RI09 | A aplicação (SAP Build Apps) chega ao backend por um túnel HTTPS | Parcial |

RI01: o cliente fica em rascunho, porque a ativação falha por uma configuração em falta no s20. RI02: não há serviço de vendas ativo no s20. RI03: falta registar a série na AT. RI09: o túnel atual muda de endereço a cada arranque.

## Requisitos não funcionais

| ID | Requisito | Estado |
| --- | --- | --- |
| RNF01 | Segurança: login com token (JWT, válido 24 h), passwords cifradas (BCrypt) e pedidos sem token recusados (HTTP 401) | Implementado |
| RNF02 | Privacidade: do cartão só se guardam os últimos 4 dígitos, e cada cliente só vê os seus dados | Implementado |
| RNF03 | Chaves e palavras-passe (Odoo, Moloni, SAP) fora do Git: ficheiro `.env` e cofres de credenciais do n8n e do Postman | Implementado |
| RNF04 | Resiliência: uma falha do Moloni ou do n8n não impede reservar nem terminar a viagem (faturas pendentes; km e danos gravados diretamente no Odoo) | Implementado |
| RNF05 | Tempos limite em todas as chamadas externas: Odoo e Moloni 10 s para ligar e 30 s para responder; n8n 5 s e 10 s | Implementado |
| RNF06 | CORS: a API só aceita pedidos de origens autorizadas (SAP Build e localhost) | Implementado |
| RNF07 | Portabilidade: Odoo, backend, PostgreSQL e n8n correm em Docker | Implementado |
| RNF08 | Rastreabilidade: código, diagramas, coleção do Postman e fluxos do n8n no GitHub | Implementado |
| RNF09 | Testabilidade: scripts de teste de ponta a ponta (`testar_fluxo.sh`, `testar_n8n.sh`, `testar_dano_n8n.sh`) | Implementado |
| RNF10 | Usabilidade: aplicação em português, pensada primeiro para telemóvel | Por fazer |
