#!/usr/bin/env bash
# Testa o fluxo completo da API DriveNow: criar conta, login, reservar, viagem, pagamento e dívida.
# Uso (com o Odoo e o backend a correr):  bash scripts/testar_fluxo.sh
set -u
API="${API:-http://localhost:8080}"
SUF=$(date +%s)
LAT=41.4417
LNG=-8.2955

# Lê um campo de um JSON; se a resposta não tiver o campo, mostra a resposta inteira (erro da API).
campo() {
  python3 -c "
import sys, json
txt = sys.stdin.read()
try:
    d = json.loads(txt)
    v = d
    for k in '$1'.split('.'):
        v = v[int(k)] if isinstance(v, list) else v[k]
    print(v)
except Exception:
    print('ERRO: ' + txt, file=sys.stderr)
    sys.exit(1)
"
}

resumo() {
  python3 -c "
import sys, json
d = json.loads(sys.stdin.read())
campos = '$1'.split(',')
print('   ' + ' | '.join(f'{c}={d.get(c)}' for c in campos))
"
}

post() { curl -s -X POST "$API$1" -H "Content-Type: application/json" ${TOKEN:+-H "Authorization: Bearer $TOKEN"} -d "$2"; }
put()  { curl -s -X PUT  "$API$1" -H "Content-Type: application/json" ${TOKEN:+-H "Authorization: Bearer $TOKEN"} -d "$2"; }
get()  { curl -s "$API$1" ${TOKEN:+-H "Authorization: Bearer $TOKEN"}; }

viagem() {  # $1 = descrição; reserva a viatura livre mais próxima, começa e termina a viagem
  VIATURA=$(get "/api/viaturas?lat=$LAT&lng=$LNG&disponiveis=true" | campo 0.id) || exit 1
  echo "   viatura escolhida: id $VIATURA"
  RES=$(post /api/reservas "{\"viaturaId\":$VIATURA}")
  RESERVA=$(echo "$RES" | campo id) || exit 1
  echo "$RES" | resumo id,matricula,estado,taxaEntrada,precoMinuto,valorAteAgora
  post /api/reservas/$RESERVA/relatos '{"tipo":"INICIO","danos":[]}' > /dev/null
  sleep 2
  post /api/reservas/$RESERVA/relatos "{\"tipo\":\"FIM\",\"latitude\":$LAT,\"longitude\":$LNG,\"danos\":[]}" > /dev/null
  get /api/reservas/$RESERVA | resumo estado,minutos,total,estadoPagamento,pagamentoDescricao
}

echo "== 1) Criar conta com cartão que APROVA (4242 4242 4242 4242)"
unset TOKEN
REG=$(post /api/auth/registar "{\"nome\":\"Carla Teste\",\"email\":\"carla$SUF@exemplo.pt\",\"nif\":\"291000010\",\"telefone\":\"912345678\",\"password\":\"segredo123\",\"aceitaTermos\":true,\"pagamento\":{\"tipo\":\"CARTAO\",\"titular\":\"CARLA TESTE\",\"numero\":\"4242 4242 4242 4242\",\"validade\":\"12/29\",\"cvv\":\"123\"}}")
TOKEN=$(echo "$REG" | campo token) || exit 1
echo "   conta criada: carla$SUF@exemplo.pt | meio: $(echo "$REG" | campo utilizador.meioPagamento.descricao)"

echo "== 2) Login com email e password"
unset TOKEN
TOKEN=$(post /api/auth/login "{\"email\":\"carla$SUF@exemplo.pt\",\"password\":\"segredo123\"}" | campo token) || exit 1
echo "   login OK (token recebido)"

echo "== 3) Viagem paga com sucesso"
viagem

echo "== 4) Criar conta com cartão que RECUSA (4000 0000 0000 0002)"
unset TOKEN
REG=$(post /api/auth/registar "{\"nome\":\"Diogo Teste\",\"email\":\"diogo$SUF@exemplo.pt\",\"nif\":\"291000029\",\"password\":\"segredo123\",\"aceitaTermos\":true,\"pagamento\":{\"tipo\":\"CARTAO\",\"titular\":\"DIOGO TESTE\",\"numero\":\"4000 0000 0000 0002\",\"validade\":\"12/29\",\"cvv\":\"123\"}}")
TOKEN=$(echo "$REG" | campo token) || exit 1
echo "   conta criada: diogo$SUF@exemplo.pt"

echo "== 5) Viagem com pagamento recusado (fica EM_DIVIDA)"
viagem

echo "== 6) Tentar reservar outra vez (deve ser recusado por dívida)"
VIATURA=$(get "/api/viaturas?lat=$LAT&lng=$LNG&disponiveis=true" | campo 0.id)
post /api/reservas "{\"viaturaId\":$VIATURA}" | resumo title,detail

echo "== 7) Atualizar o cartão para um que aprova (a dívida é paga logo)"
put /api/me/pagamento '{"tipo":"CARTAO","titular":"DIOGO TESTE","numero":"4242 4242 4242 4242","validade":"12/29","cvv":"123"}' | resumo valorEmDivida
get /api/me/cobrancas | python3 -c "
import sys, json
for c in json.load(sys.stdin):
    print(f\"   cobrança {c['id']}: {c['tipo']} {c['valor']} € -> {c['estado']} ({c['meio']}, tentativas: {c['tentativas']})\")"

echo "== 8) Já pode reservar outra vez (e cancela logo a seguir)"
RES=$(post /api/reservas "{\"viaturaId\":$VIATURA}")
RESERVA=$(echo "$RES" | campo id) || exit 1
post /api/reservas/$RESERVA/cancelar '' | resumo id,estado,minutos,total,estadoPagamento

echo "== 9) Sem token, a API recusa (HTTP 401)"
unset TOKEN
echo "   GET /api/reservas -> HTTP $(curl -s -o /dev/null -w '%{http_code}' "$API/api/reservas")"

echo "== Fim do teste"
