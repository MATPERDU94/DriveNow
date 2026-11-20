#!/usr/bin/env python3
"""
Revisões por km (diagrama 6): ver o estado e preparar testes.

Uso (com o Odoo a correr), dentro de src/backend:
  python3 scripts/revisoes.py estado                  -> km que faltam até à revisão de cada carro
  python3 scripts/revisoes.py preparar 12-AB-34 10    -> a revisão desse carro fica a 10 km (para testar)
  python3 scripts/revisoes.py repor                   -> próxima revisão de todos = múltiplo de 15 000 km seguinte

Lê ODOO_DB e ODOO_API_KEY do ficheiro .env; fala com o Odoo em http://localhost:8069 (ou ODOO_URL).
"""
import json
import os
import pathlib
import sys
import urllib.error
import urllib.request

INTERVALO = 15000
PASTA = pathlib.Path(__file__).resolve().parent
ODOO = os.environ.get("ODOO_URL", "http://localhost:8069")


def ler_env():
    env = {}
    ficheiro = PASTA.parent / ".env"
    if not ficheiro.exists():
        sys.exit(f"Não encontrei o {ficheiro}")
    for linha in ficheiro.read_text(encoding="utf-8").splitlines():
        if "=" in linha and not linha.strip().startswith("#"):
            k, v = linha.split("=", 1)
            env[k.strip()] = v.strip()
    return env


ENV = ler_env()
DB = ENV.get("ODOO_DB", "drivenow")
CHAVE = ENV.get("ODOO_API_KEY", "")
if not CHAVE:
    sys.exit("Falta ODOO_API_KEY no .env")


def odoo(modelo, metodo, corpo):
    dados = json.dumps(corpo).encode("utf-8")
    pedido = urllib.request.Request(
        f"{ODOO}/json/2/{modelo}/{metodo}", data=dados, method="POST",
        headers={"Content-Type": "application/json", "Authorization": f"Bearer {CHAVE}",
                 "X-Odoo-Database": DB, "Content-Length": str(len(dados))})
    try:
        with urllib.request.urlopen(pedido, timeout=30) as r:
            return json.loads(r.read() or "null")
    except urllib.error.HTTPError as e:
        texto = e.read().decode("utf-8", "replace")
        if "next_service_km" in texto or "is_revision" in texto:
            sys.exit("O Odoo ainda não tem as revisões: atualize o módulo drivenow_danos (passo 5).")
        sys.exit(f"Erro do Odoo em {modelo}.{metodo}: {e.code} {texto[:400]}")
    except urllib.error.URLError as e:
        sys.exit(f"Não consegui falar com o Odoo em {ODOO}: {e.reason}. O Odoo está a correr?")


def carros():
    return odoo("fleet.vehicle", "search_read", {
        "domain": [], "order": "license_plate",
        "fields": ["license_plate", "odometer", "next_service_km", "km_to_service", "service_status"]})


ESTADOS = {"ok": "em dia", "soon": "REVISÃO PRÓXIMA", "due": "REVISÃO POR FAZER"}


def estado():
    lista = sorted(carros(), key=lambda c: c["km_to_service"])
    print(f"{'Matrícula':<10} {'Km atuais':>10} {'Próxima':>10} {'Faltam':>8}  Estado")
    for c in lista:
        print(f"{c['license_plate'] or '-':<10} {c['odometer']:>10.0f} {c['next_service_km']:>10.0f} "
              f"{c['km_to_service']:>8.0f}  {ESTADOS.get(c['service_status'], c['service_status'])}")
    ordens = odoo("fleet.vehicle.log.services", "search_read", {
        "domain": [["service_type_id.is_revision", "=", True], ["state", "in", ["new", "running"]]],
        "fields": ["vehicle_id", "state", "description"]})
    print(f"\n{sum(c['service_status'] == 'due' for c in lista)} carros com revisão por fazer (saem do mapa), "
          f"{sum(c['service_status'] == 'soon' for c in lista)} com a revisão a menos de 1000 km.")
    print(f"{len(ordens)} ordens de revisão abertas no Odoo (Frota → Revisões → Ordens de revisão):")
    for o in ordens:
        print(f"  - {o['vehicle_id'][1]}: {o['description'] or ''} ({o['state']})")


def preparar(matricula, km):
    c = next((c for c in carros() if (c["license_plate"] or "").upper() == matricula.upper()), None)
    if not c:
        sys.exit(f"Não encontrei o carro {matricula}")
    proxima = round(c["odometer"] + km)
    odoo("fleet.vehicle", "write", {"ids": [c["id"]], "vals": {"next_service_km": proxima}})
    print(f"{c['license_plate']}: {c['odometer']:.0f} km agora; revisão aos {proxima} km (faltam {km:.0f} km).")
    print(f"Faça uma viagem com mais de {km:.0f} km: no fim, o carro sai do mapa com 'Revisão por fazer'.")


def repor():
    lista = carros()
    for c in lista:
        proxima = (int(c["odometer"] // INTERVALO) + 1) * INTERVALO
        odoo("fleet.vehicle", "write", {"ids": [c["id"]], "vals": {"next_service_km": proxima}})
    print(f"Próxima revisão reposta em {len(lista)} carros (múltiplo de {INTERVALO} km seguinte).")


if __name__ == "__main__":
    acao = sys.argv[1] if len(sys.argv) > 1 else ""
    if acao == "estado":
        estado()
    elif acao == "preparar" and len(sys.argv) >= 3:
        preparar(sys.argv[2], float(sys.argv[3]) if len(sys.argv) > 3 else 10)
        estado()
    elif acao == "repor":
        repor()
        estado()
    else:
        print(__doc__)
