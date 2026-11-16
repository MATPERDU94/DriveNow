#!/usr/bin/env bash
# Testa o evento "viagem terminada": backend -> n8n -> Odoo (quilometragem).
# Uso (com Odoo, backend e n8n a correr, e o fluxo publicado no n8n):  bash scripts/testar_n8n.sh
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

echo "== 1) Criar conta de teste"
unset TOKEN
REG=$(post /api/auth/registar "{\"nome\":\"Nuno Teste\",\"email\":\"nuno$SUF@exemplo.pt\",\"nif\":\"291000010\",\"password\":\"segredo123\",\"aceitaTermos\":true,\"pagamento\":{\"tipo\":\"CARTAO\",\"titular\":\"NUNO TESTE\",\"numero\":\"4242 4242 4242 4242\",\"validade\":\"12/29\",\"cvv\":\"123\"}}")
TOKEN=$(echo "$REG" | campo token) || exit 1

echo "== 2) Reservar a viatura livre mais próxima"
VIATURA=$(get "/api/viaturas?lat=$LAT&lng=$LNG&disponiveis=true" | campo 0.id) || exit 1
KM_ANTES=$(get "/api/viaturas/$VIATURA" | campo quilometragem) || exit 1
KM_DEPOIS=$(python3 -c "print(round(float('$KM_ANTES' if '$KM_ANTES' != 'None' else 0) + 12))")
RESERVA=$(post /api/reservas "{\"viaturaId\":$VIATURA}" | campo id) || exit 1
echo "   viatura $VIATURA | km antes: $KM_ANTES | reserva $RESERVA"

echo "== 3) Viagem: relato de início e de fim (com $KM_DEPOIS km)"
post /api/reservas/$RESERVA/relatos '{"tipo":"INICIO","danos":[]}' > /dev/null
post /api/reservas/$RESERVA/relatos "{\"tipo\":\"FIM\",\"latitude\":$LAT,\"longitude\":$LNG,\"quilometragem\":$KM_DEPOIS,\"danos\":[]}" | python3 -c "import sys,json; d=json.load(sys.stdin); print('   relato de fim:', d.get('id', d))"

echo "== 4) Quilometragem no Odoo (via n8n)"
sleep 2
KM_AGORA=$(get "/api/viaturas/$VIATURA" | campo quilometragem)
echo "   km agora: $KM_AGORA (esperado: $KM_DEPOIS)"
echo "   Confirma também no n8n, separador Executions, uma execução nova do fluxo 'DriveNow - Viagem terminada'."
