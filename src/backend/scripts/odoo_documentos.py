#!/usr/bin/env python3
"""
Prepara no Odoo os documentos das viaturas (diagrama 5): seguro, inspeção periódica e IUC.

- cria os 3 tipos de contrato (fleet.service.type, categoria "contract"), se não existirem;
- cria, para cada viatura, um contrato de cada tipo com data de validade, se ainda não tiver.

Para a demonstração:
- a 1.ª viatura (por matrícula) fica com o SEGURO CADUCADO há 5 dias -> sai do mapa;
- a 2.ª viatura fica com a INSPEÇÃO a caducar daqui a 10 dias -> alerta, mas continua disponível;
- as outras ficam com validades entre 2 e 12 meses.

Uso (com o Odoo a correr), dentro de src/backend:  python3 scripts/odoo_documentos.py
Lê ODOO_DB e ODOO_API_KEY do ficheiro .env; fala com o Odoo em http://localhost:8069.
"""
import json
import pathlib
import sys
import urllib.error
import urllib.request
from datetime import date, timedelta

ODOO = "http://localhost:8069"
TIPOS = ["Seguro", "Inspeção periódica", "IUC"]


def ler_env():
    env = {}
    ficheiro = pathlib.Path(__file__).resolve().parent.parent / ".env"
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
        sys.exit(f"Erro do Odoo em {modelo}.{metodo}: {e.code} {e.read().decode('utf-8', 'replace')[:500]}")


hoje = date.today()

# 1) Tipos de documento
tipos = {}
for nome in TIPOS:
    existentes = odoo("fleet.service.type", "search_read",
                      {"domain": [["name", "=", nome]], "fields": ["id", "category"]})
    if existentes:
        tipos[nome] = existentes[0]["id"]
        print(f"Tipo '{nome}' já existe (id {tipos[nome]})")
    else:
        tipos[nome] = odoo("fleet.service.type", "create",
                           {"vals_list": [{"name": nome, "category": "contract"}]})[0]
        print(f"Tipo '{nome}' criado (id {tipos[nome]})")

# 2) Contratos por viatura
viaturas = odoo("fleet.vehicle", "search_read",
                {"domain": [], "fields": ["id", "license_plate"], "order": "license_plate"})
criados = 0
for i, v in enumerate(viaturas):
    for j, nome in enumerate(TIPOS):
        ja_tem = odoo("fleet.vehicle.log.contract", "search_read",
                      {"domain": [["vehicle_id", "=", v["id"]], ["cost_subtype_id", "=", tipos[nome]],
                                  ["state", "!=", "closed"]], "fields": ["id"]})
        if ja_tem:
            continue
        if i == 0 and nome == "Seguro":
            validade = hoje - timedelta(days=5)
        elif i == 1 and nome == "Inspeção periódica":
            validade = hoje + timedelta(days=10)
        else:
            validade = hoje + timedelta(days=60 + ((i * 37 + j * 53) % 300))
        odoo("fleet.vehicle.log.contract", "create", {"vals_list": [{
            "vehicle_id": v["id"],
            "cost_subtype_id": tipos[nome],
            "start_date": (validade - timedelta(days=365)).isoformat(),
            "expiration_date": validade.isoformat(),
        }]})
        criados += 1
print(f"{criados} documentos criados para {len(viaturas)} viaturas")
if len(viaturas) > 1:
    print(f"Demonstração: {viaturas[0]['license_plate']} com o seguro caducado; "
          f"{viaturas[1]['license_plate']} com a inspeção a caducar em 10 dias")
