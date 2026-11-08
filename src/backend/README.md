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
├── utilizadores/  registo de utilizadores
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

```bash
# Estado da API
curl localhost:8080/actuator/health

# Frota com coordenadas, e tabela de danos (vem do Odoo)
curl localhost:8080/api/viaturas
# Viaturas livres a menos de 1 km do Largo do Toural, da mais próxima para a mais afastada
curl "localhost:8080/api/viaturas?lat=41.4417&lng=-8.2955&raio=1&disponiveis=true"
curl localhost:8080/api/danos/tipos

# Criar um utilizador
curl -X POST localhost:8080/api/utilizadores -H "Content-Type: application/json" \
  -d '{"nome":"Ana Silva","email":"ana@exemplo.pt","nif":"123456789"}'

# Reservar uma viatura (viaturaId = "id" de /api/viaturas)
curl -X POST localhost:8080/api/reservas -H "Content-Type: application/json" \
  -d '{"utilizadorId":1,"viaturaId":1}'

# Relato de início: começa a viagem (danos que já existiam não são cobrados)
curl -X POST localhost:8080/api/reservas/1/relatos -H "Content-Type: application/json" \
  -d '{"tipo":"INICIO","observacoes":"Carro limpo","danos":[]}'

# Relato de fim: termina a viagem, guarda onde o carro ficou e a quilometragem,
# calcula o preço e cobra os danos novos (tipoDanoId = "id" de /api/danos/tipos)
curl -X POST localhost:8080/api/reservas/1/relatos -H "Content-Type: application/json" \
  -d '{"tipo":"FIM","observacoes":"Risquei ao estacionar","latitude":41.4525,"longitude":-8.2898,"quilometragem":3912,"danos":[{"tipoDanoId":3,"descricao":"porta traseira esquerda"}]}'

# Ver a reserva (minutos, valorViagem, totalPenalizacoes, total) e os relatos
curl localhost:8080/api/reservas/1
curl localhost:8080/api/reservas/1/relatos
```

Depois do relato de fim, o dano aparece no Odoo em **Frota → Danos → Registo de danos**.

## Endpoints

| Método | Caminho | Descrição |
|---|---|---|
| GET | `/api/viaturas[?lat=&lng=&raio=&disponiveis=true]` | Frota com coordenadas; com lat/lng, ordenada por distância (km) |
| GET | `/api/viaturas/{id}` | Uma viatura |
| GET | `/api/danos/tipos` | Tabela de custos de danos (Odoo) |
| POST | `/api/utilizadores` | Criar utilizador |
| GET | `/api/utilizadores`, `/api/utilizadores/{id}` | Consultar utilizadores |
| POST | `/api/reservas` | Criar reserva `{utilizadorId, viaturaId}` |
| GET | `/api/reservas?utilizadorId=1` | Histórico de reservas |
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
- Autenticação (registo e login)
- Documentação dos carros (seguro, inspeção, IUC) e alertas de validade (diagrama 5)
- Cobrança (meios de pagamento e dívidas pendentes)

## Comandos úteis

| Ação | Comando (em `~/drivenow/src/backend`) |
|---|---|
| Arrancar/recompilar | `docker compose up -d --build` |
| Logs | `docker compose logs -f api` |
| Parar | `docker compose down` |
| Apagar a BD do backend (recomeçar do zero) | `docker compose down -v` |
