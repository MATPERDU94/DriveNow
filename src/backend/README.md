# DriveNow – Backend (Spring Boot)

API REST da DriveNow: utilizadores, reservas, relatos de início e de fim de viagem, e integração com o Odoo (frota e danos).

- Java 21 · Spring Boot 3.5 · PostgreSQL 16
- Corre em Docker: não é preciso instalar Java nem Maven no PC.

## Estrutura

```
src/main/java/pt/drivenow/
├── odoo/          OdooClient: chamadas à API JSON-2 do Odoo 19
├── moloni/        MoloniClient: login OAuth e chamadas à API do Moloni
├── faturacao/     fatura-recibo no Moloni no fim de cada viagem
├── frota/         GET /api/viaturas (Odoo + posição GPS + disponibilidade)
├── localizacao/   posição GPS de cada viatura (atualizada no fim de cada viagem)
├── danos/         GET /api/danos/tipos (tabela de custos do Odoo)
├── auth/          criar conta, login e token (JWT)
├── utilizadores/  perfil do utilizador (/api/me)
├── pagamentos/    meio de pagamento e cobranças (simuladas)
├── reservas/      reservas e estado da viagem
├── relatos/       relatos de início/fim + atribuição de danos e penalizações
└── common/        erros da API
```

## 1. Criar a chave de API no Odoo

1. No Odoo, clica no teu avatar (canto superior direito) → **Preferências** (ou **O meu perfil**)
2. Separador **Segurança da conta** → **Nova chave de API**
3. Dá-lhe um nome (ex.: `backend`) e a duração mais longa disponível
4. Copia a chave: só aparece uma vez

## 2. Configurar

```bash
cd ~/drivenow/src/backend
cp .env.example .env
nano .env
```

Preenche:

- `ODOO_DB`: o nome da base de dados do Odoo (vê-o em http://localhost:8069/web/database/manager)
- `ODOO_API_KEY`: a chave do passo 1

O `.env` não vai para o Git.

## 3. Arrancar

O Odoo tem de estar a correr (`cd ~/drivenow/infra/odoo && docker compose up -d`).

```bash
cd ~/drivenow/src/backend
docker compose up -d --build
docker compose logs -f api
```

Quando aparecer `Started DriveNowApplication`, a API está em **http://localhost:8080** (`Ctrl+C` sai dos logs).

## 4. Testar o fluxo completo

O script `scripts/testar_fluxo.sh` corre tudo de uma vez: cria duas contas (uma com um cartão que aprova e outra com um que recusa), faz login, faz viagens, mostra os pagamentos, a dívida que bloqueia reservas e o pagamento da dívida depois de atualizar o cartão.

```bash
cd ~/drivenow/src/backend
bash scripts/testar_fluxo.sh
```

Para testar à mão com o `curl`, primeiro faz-se login e guarda-se o token:

```bash
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"email":"o_teu_email","password":"a_tua_password"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")
curl -s localhost:8080/api/me -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```

## Contas e login

- `POST /api/auth/registar` cria a conta com os dados pessoais **e** o meio de pagamento (ecrã "Criar conta"):
  ```json
  {"nome": "Ana Silva", "email": "ana@exemplo.pt", "nif": "291000010", "telefone": "912345678",
   "password": "segredo123", "aceitaTermos": true,
   "pagamento": {"tipo": "CARTAO", "titular": "ANA SILVA", "numero": "4242 4242 4242 4242", "validade": "08/28", "cvv": "123"}}
  ```
  ou, com conta bancária: `"pagamento": {"tipo": "CONTA_BANCARIA", "titular": "Ana Silva", "iban": "PT50 0002 0123 1234 5678 9015 4"}`.
- `POST /api/auth/login` com `{"email", "password"}` devolve `{"token", "expiraEm", "utilizador"}`.
- Todos os outros pedidos (exceto o mapa e a tabela de danos) levam o cabeçalho `Authorization: Bearer {token}`. Cada cliente só vê e mexe nas suas reservas.
- A password é guardada com BCrypt. O NIF tem de ser válido (dígito de controlo).

## Pagamentos (simulados)

- Guarda-se só o titular, a marca, os **últimos 4 dígitos** e a validade. O número completo, o IBAN completo e o CVV nunca são guardados.
- No fim da viagem (ou ao cancelar), o valor é cobrado logo ao meio de pagamento.
- **Cartões de teste:** `4242 4242 4242 4242` aprova; `4000 0000 0000 0002` recusa sempre. Um cartão expirado também é recusado.
- Uma cobrança recusada fica **em dívida** e **bloqueia novas reservas**. Ao alterar o meio de pagamento (`PUT /api/me/pagamento`), as dívidas são cobradas de novo automaticamente.
- A **fatura só é emitida depois de o pagamento ser aprovado** (diagrama 3).
- Penalizações cobradas ao condutor anterior têm uma cobrança e uma fatura à parte.

## Endpoints

| Método | Caminho | Descrição |
|---|---|---|
| POST | `/api/auth/registar` | Criar conta (dados + meio de pagamento) → token |
| POST | `/api/auth/login` | Login → token |
| GET / PUT | `/api/me` | Ver / alterar os dados pessoais |
| PUT | `/api/me/pagamento` | Alterar o meio de pagamento (paga as dívidas pendentes) |
| GET | `/api/me/cobrancas` | Cobranças do utilizador |
| POST | `/api/me/cobrancas/pagar` | Tentar pagar de novo as dívidas |
| GET | `/api/viaturas[?lat=&lng=&raio=&disponiveis=true]` | Frota com coordenadas; com lat/lng, ordenada por distância (km) |
| GET | `/api/viaturas/{id}` | Uma viatura |
| GET | `/api/danos/tipos` | Tabela de custos de danos (Odoo) |
| POST | `/api/reservas` | Reservar `{viaturaId}` (o cliente vem do token) |
| GET | `/api/reservas` | Histórico de reservas do cliente |
| GET | `/api/reservas/ativa` | Reserva ativa (204 se não houver) |
| GET | `/api/reservas/{id}` | Uma reserva |
| POST | `/api/reservas/{id}/cancelar` | Cancelar (só antes de começar) |
| POST | `/api/reservas/{id}/faturar` | Emitir (ou repetir) a fatura-recibo no Moloni |
| POST | `/api/reservas/{id}/relatos` | Relato `{tipo: INICIO/FIM, observacoes, danos: [{tipoDanoId, descricao}], latitude, longitude, quilometragem}` (coordenadas obrigatórias no FIM; quilometragem opcional) |
| GET | `/api/reservas/{id}/relatos` | Relatos da reserva |

## Preço da viagem

```
total = taxa de entrada + minutos × preço por minuto + penalizações
```

- Os preços estão no Odoo, por modelo, em **Frota → Tarifário** (ex.: Renault Zoe 10 € + 0,18 €/min; Porsche Taycan 200 € + 0,63 €/min).
- A **taxa de entrada** é fixada ao reservar e não é devolvida se a reserva for cancelada.
- O **temporizador começa no momento da reserva** e para no relato de fim (ou no cancelamento); cada minuto começado conta (mínimo 1).
- Enquanto a reserva está ativa, `minutosDecorridos` e `valorAteAgora` dão o tempo e o valor até ao momento (ecrã "Em viagem").
- **Cancelar** (antes do relato de início) paga a taxa de entrada e os minutos decorridos, e é faturado.
- Os preços são copiados para a reserva no momento em que é criada: se o tarifário mudar a meio da viagem, o cliente paga o preço que viu.
- Não é possível reservar uma viatura sem preço por minuto definido no Odoo.

## Faturação (Moloni)

No fim de cada viagem (relato de FIM), o backend emite no Moloni uma **fatura-recibo**:

| Linha | Artigo no Moloni (referência) |
|---|---|
| Taxa de entrada | `DN-TAXA` |
| Minutos de viagem (qtd = minutos) | `DN-MIN` |
| Uma linha por penalização de dano | `DN-DANO` |

- Os três artigos têm de ser criados uma vez no Moloni (**Tabelas → Artigos**), do tipo **Serviço** e com **IVA 23%**.
- Os preços do tarifário **incluem IVA**; no Moloni as linhas levam o preço sem IVA.
- O cliente é criado no Moloni com o NIF do utilizador (ou como "Consumidor final", NIF 999999990, se não tiver NIF).
- `MOLONI_FECHAR_DOCUMENTOS=false` (por omissão): a fatura fica em **rascunho**, sem valor fiscal e sem PDF. Com `true`, é finalizada e o `faturaPdfUrl` da reserva traz o link do PDF.
- Se o Moloni falhar, a viagem termina na mesma; a reserva fica com `estadoFatura: ERRO` e o motivo em `faturaErro`. Para tentar de novo: `curl -X POST localhost:8080/api/reservas/{id}/faturar`.

## Danos (diagrama 4)

Para cada dano assinalado num relato:

1. **Já registado?** Se o Odoo já tem esse tipo de dano nessa viatura por reparar (estado Novo ou Em curso), é ignorado.
2. **Responsável:** no relato de **fim**, o próprio cliente; no relato de **início**, o **condutor anterior** (a última viagem concluída nessa viatura). Sem condutor anterior, o dano é registado mas não é cobrado.
3. **Penalização:** o custo da tabela de danos, até ao limite da **franquia** (`DRIVENOW_FRANQUIA`, 1000 € por omissão) por viagem.
4. As penalizações do fim vão na fatura da viagem; as cobradas ao condutor anterior têm uma **fatura-recibo à parte**.
5. Um dano de um tipo que **imobiliza o carro** deixa a viatura **"Em reparação"** (indisponível no mapa) até o gestor de frota marcar o registo como **Concluído** no Odoo.

## Regras implementadas

- Um utilizador só pode ter uma reserva ativa; só se reservam viaturas disponíveis (não reservadas nem em reparação).
- Relato de **início**: só com a reserva em `RESERVADA`; passa a `EM_CURSO`.
- Relato de **fim**: só com a reserva em `EM_CURSO`; passa a `CONCLUIDA`, calcula o preço, e a posição da viatura é atualizada com as coordenadas enviadas. Se vier a quilometragem, é registada no Odoo (não pode ser inferior à atual).
- Não há hubs: cada viatura tem uma posição GPS. No primeiro arranque, as viaturas sem posição ficam distribuídas por 30 pontos de Guimarães.
- `disponivel` é `false` enquanto a viatura estiver numa reserva ativa.

## Próximos passos

- `SapClient` (Business Partner, Sales Order)
- Documentação dos carros (seguro, inspeção, IUC) e alertas de validade (diagrama 5)
- Envio da fatura por email

## Comandos úteis

| Ação | Comando (em `~/drivenow/src/backend`) |
|---|---|
| Arrancar/recompilar | `docker compose up -d --build` |
| Logs | `docker compose logs -f api` |
| Parar | `docker compose down` |
| Apagar a BD do backend (recomeçar do zero) | `docker compose down -v` |
