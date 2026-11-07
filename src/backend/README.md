# DriveNow – Backend (Spring Boot)

API REST da DriveNow: utilizadores, reservas, relatos de início e de fim de viagem, e integração com o Odoo (frota e danos).

- Java 21 · Spring Boot 3.5 · PostgreSQL 16
- Corre em Docker: não é preciso instalar Java nem Maven no PC.

## Estrutura

```
src/main/java/pt/drivenow/
├── odoo/          OdooClient: chamadas à API JSON-2 do Odoo 19
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

# Relato de fim: termina a viagem, guarda onde o carro ficou e cobra os danos novos
# (tipoDanoId = "id" de /api/danos/tipos)
curl -X POST localhost:8080/api/reservas/1/relatos -H "Content-Type: application/json" \
  -d '{"tipo":"FIM","observacoes":"Risquei ao estacionar","latitude":41.4525,"longitude":-8.2898,"danos":[{"tipoDanoId":3,"descricao":"porta traseira esquerda"}]}'

# Ver a reserva (totalPenalizacoes) e os relatos
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
| POST | `/api/reservas/{id}/relatos` | Relato `{tipo: INICIO/FIM, observacoes, danos: [{tipoDanoId, descricao}], latitude, longitude}` (coordenadas obrigatórias no FIM) |
| GET | `/api/reservas/{id}/relatos` | Relatos da reserva |

## Regras implementadas

- Um utilizador só pode ter uma reserva ativa, e uma viatura só pode estar numa reserva ativa.
- Relato de **início**: só com a reserva em `RESERVADA`; passa a `EM_CURSO`.
- Relato de **fim**: só com a reserva em `EM_CURSO`; passa a `CONCLUIDA` e a posição da viatura é atualizada com as coordenadas enviadas.
- Não há hubs: cada viatura tem uma posição GPS. No primeiro arranque, as viaturas sem posição ficam distribuídas por 30 pontos de Guimarães.
- `disponivel` é `false` enquanto a viatura estiver numa reserva ativa.
- Todos os danos assinalados são registados no Odoo, com o custo da tabela.
- Só os danos do relato de **fim** são cobrados ao cliente, e não os de um tipo que ele já tinha assinalado no relato de início.

## Próximos passos

- Cálculo do preço da viagem
- `SapClient` (Business Partner, Sales Order) e `MoloniClient` (faturas-recibo)
- Autenticação (registo e login)
- Marcar a viatura como indisponível no Odoo quando um dano a imobiliza

## Comandos úteis

| Ação | Comando (em `~/drivenow/src/backend`) |
|---|---|
| Arrancar/recompilar | `docker compose up -d --build` |
| Logs | `docker compose logs -f api` |
| Parar | `docker compose down` |
| Apagar a BD do backend (recomeçar do zero) | `docker compose down -v` |
