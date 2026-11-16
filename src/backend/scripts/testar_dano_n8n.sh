#!/usr/bin/env bash
# Testa o evento "dano reportado": backend -> n8n -> Odoo (registo do dano) -> id devolvido ao backend.
# Uso (com Odoo, backend e n8n a correr, e o fluxo "DriveNow - Dano reportado" publicado):
#   bash scripts/testar_dano_n8n.sh
set -u
API="${API:-http://localhost:8080}"
SUF=$(date +%s)
LAT=41.4417
LNG=-8.2955

campo() {
  python3 -c "
import sys, json
txt = sys.stdin.read()
try:
    v = json.loads(txt)
    for k in '$1'.split('.'):
        v = v[int(k)] if isinstance(v, list) else v[k]
    print(v)
except Exception:
    print('ERRO: ' + txt, file=sys.stderr)
    sys.exit(1)
"
}
post() { curl -s -X POST "$API$1" -H "Content-Type: application/json" ${TOKEN:+-H "Authorization: Bearer $TOKEN"} -d "$2"; }
get()  { curl -s "$API$1" ${TOKEN:+-H "Authorization: Bearer $TOKEN"}; }

echo "== 1) Escolher um tipo de dano que NÃO imobiliza o carro"
TIPO=$(get /api/danos/tipos | python3 -c "
import sys, json, random
tipos = [t for t in json.load(sys.stdin) if not t.get('imobilizaCarro')]
t = random.choice(tipos)
print(t['id'], '|', t['nome'], '|', t['custo'], '€')")
TIPO_ID=${TIPO%% *}
echo "   $TIPO"

echo "== 2) Criar conta e reservar a viatura livre mais próxima"
unset TOKEN
REG=$(post /api/auth/registar "{\"nome\":\"Rita Teste\",\"email\":\"rita$SUF@exemplo.pt\",\"nif\":\"291000010\",\"password\":\"segredo123\",\"aceitaTermos\":true,\"pagamento\":{\"tipo\":\"CARTAO\",\"titular\":\"RITA TESTE\",\"numero\":\"4242 4242 4242 4242\",\"validade\":\"12/29\",\"cvv\":\"123\"}}")
TOKEN=$(echo "$REG" | campo token) || exit 1
VIATURA=$(get "/api/viaturas?lat=$LAT&lng=$LNG&disponiveis=true" | campo 0.id) || exit 1
RESERVA=$(post /api/reservas "{\"viaturaId\":$VIATURA}" | campo id) || exit 1
echo "   viatura $VIATURA | reserva $RESERVA"

echo "== 3) Viagem com dano no fim (tipo $TIPO_ID)"
post /api/reservas/$RESERVA/relatos '{"tipo":"INICIO","danos":[]}' > /dev/null
post /api/reservas/$RESERVA/relatos "{\"tipo\":\"FIM\",\"latitude\":$LAT,\"longitude\":$LNG,\"danos\":[{\"tipoDanoId\":$TIPO_ID,\"descricao\":\"Teste n8n $SUF\"}]}" | python3 -c "
import sys, json
d = json.load(sys.stdin)
for x in d.get('danos', []):
    print(f\"   dano: {x['tipoDano']} | penalização {x['penalizacao']} € | registo Odoo #{x['odooRegistoId']} | {x['nota']}\")
if 'danos' not in d: print('   resposta:', d)"

echo "== 4) Confirmar"
echo "   - logs:  docker compose logs api | grep -i 'dano-reportado' | tail -1"
echo "   - n8n:   separador Executions do fluxo 'DriveNow - Dano reportado'"
echo "   - Odoo:  Danos -> Registo de danos (descrição 'Teste n8n $SUF')"
