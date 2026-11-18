#!/usr/bin/env python3
"""
Teste do diagrama 5: bloquear carros com um documento caducado e ver que saem do mapa.

Não mexe nos documentos que já existem. Para bloquear, cria em cada carro um IUC extra,
caducado ontem; para desbloquear, apaga só esses IUC de teste (os ids ficam guardados
em scripts/.bloqueio_teste.json).

Uso (com o Odoo e o backend a correr), dentro de src/backend:
  python3 scripts/bloquear_carros.py bloquear              -> bloqueia todos os carros
  python3 scripts/bloquear_carros.py bloquear 12-AB-34     -> bloqueia só esses carros (matrículas)
  python3 scripts/bloquear_carros.py estado                -> quantos carros estão bloqueados
  python3 scripts/bloquear_carros.py desbloquear           -> apaga os documentos de teste

Lê ODOO_DB e ODOO_API_KEY do ficheiro .env; fala com o Odoo em http://localhost:8069.
"""
import json
import pathlib
import sys
import urllib.error
import urllib.request
from datetime import date, timedelta

ODOO = "http://localhost:8069"
TIPOS = ["Seguro", "Inspeção periódica", "IUC"]  # os mesmos do DocumentoService do backend
PASTA = pathlib.Path(__file__).resolve().parent
REGISTO = PASTA / ".bloqueio_teste.json"


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
        sys.exit(f"Erro do Odoo em {modelo}.{metodo}: {e.code} {e.read().decode('utf-8', 'replace')[:400]}")
    except urllib.error.URLError as e:
        sys.exit(f"Não consegui falar com o Odoo em {ODOO}: {e.reason}. O Odoo está a correr?")


def ler_registo():
    if REGISTO.exists():
        return json.loads(REGISTO.read_text(encoding="utf-8"))
    return []


def gravar_registo(ids):
    if ids:
        REGISTO.write_text(json.dumps(sorted(set(ids))), encoding="utf-8")
    elif REGISTO.exists():
        REGISTO.unlink()


def bloqueados():
    """Carros com pelo menos um documento caducado (a mesma regra do backend)."""
    contratos = odoo("fleet.vehicle.log.contract", "search_read", {
        "domain": [["cost_subtype_id.name", "in", TIPOS], ["state", "!=", "closed"],
                   ["expiration_date", "<", date.today().isoformat()]],
        "fields": ["vehicle_id"]})
    return {c["vehicle_id"][0] for c in contratos if c.get("vehicle_id")}


def estado():
    total = odoo("fleet.vehicle", "search_count", {"domain": []})
    n = len(bloqueados())
    print(f"{n} de {total} carros com documentos caducados (não aparecem no mapa).")
    teste = ler_registo()
    if teste:
        print(f"{len(teste)} documentos de teste criados por este script; para os apagar: desbloquear")


def bloquear(matriculas):
    tipo = odoo("fleet.service.type", "search_read", {"domain": [["name", "=", "IUC"]], "fields": ["id"]})
    if not tipo:
        sys.exit("Não existe o tipo de documento 'IUC' no Odoo. Corre primeiro scripts/odoo_documentos.py")
    dominio = [["license_plate", "in", matriculas]] if matriculas else []
    viaturas = odoo("fleet.vehicle", "search_read",
                    {"domain": dominio, "fields": ["id", "license_plate"], "order": "license_plate"})
    if not viaturas:
        sys.exit("Nenhum carro encontrado" + (f" com as matrículas {', '.join(matriculas)}" if matriculas else ""))
    ontem = date.today() - timedelta(days=1)
    novos = odoo("fleet.vehicle.log.contract", "create", {"vals_list": [{
        "vehicle_id": v["id"],
        "cost_subtype_id": tipo[0]["id"],
        "start_date": (ontem - timedelta(days=365)).isoformat(),
        "expiration_date": ontem.isoformat(),
    } for v in viaturas]})
    gravar_registo(ler_registo() + list(novos))
    print(f"Bloqueados {len(viaturas)} carros (IUC de teste caducado a {ontem.isoformat()}):")
    print("  " + ", ".join(v["license_plate"] or f"id {v['id']}" for v in viaturas))
    print("Atualiza o mapa (F5): estes carros já não devem aparecer.")


def desbloquear():
    ids = ler_registo()
    if not ids:
        print("Não há documentos de teste para apagar.")
    else:
        existentes = odoo("fleet.vehicle.log.contract", "search_read",
                          {"domain": [["id", "in", ids]], "fields": ["id"]})
        ids_ok = [c["id"] for c in existentes]
        if ids_ok:
            odoo("fleet.vehicle.log.contract", "unlink", {"ids": ids_ok})
        gravar_registo([])
        print(f"Apagados {len(ids_ok)} documentos de teste. Atualiza o mapa (F5): os carros voltam a aparecer.")
    estado()


if __name__ == "__main__":
    acao = sys.argv[1] if len(sys.argv) > 1 else ""
    if acao == "bloquear":
        bloquear(sys.argv[2:])
        estado()
    elif acao == "desbloquear":
        desbloquear()
    elif acao == "estado":
        estado()
    else:
        print(__doc__)
