#!/usr/bin/env python3
"""
DriveNow: liga tudo com um só comando, dentro de ~/drivenow (no Ubuntu):

  python3 app.py           -> liga o Docker, o Odoo, o backend e a app, e abre o browser
  python3 app.py parar     -> desliga o backend e o Odoo (os dados ficam guardados)
  python3 app.py estado    -> mostra o que está ligado

Ordem: Docker -> Odoo -> backend -> app. Cada passo espera que o anterior responda.
Para parar a app: Ctrl+C (o Odoo e o backend continuam ligados até "python3 app.py parar").
Só usa o Python que já vem no Ubuntu (não é preciso instalar nada).
"""
import glob
import os
import shutil
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.request
from pathlib import Path

RAIZ = Path(__file__).resolve().parent
ODOO = RAIZ / "infra" / "odoo"
BACKEND = RAIZ / "src" / "backend"
FRONTEND = RAIZ / "src" / "frontend"

URL_ODOO = "http://localhost:8069/web/login"
URL_BACKEND = "http://localhost:8080/api/viaturas"
URL_APP = "http://localhost:5173"

# Docker Desktop no Windows (visto a partir do Ubuntu/WSL e a partir do Windows)
DOCKER_DESKTOP_WSL = "/mnt/c/Program Files/Docker/Docker/Docker Desktop.exe"
DOCKER_DESKTOP_WIN = r"C:\Program Files\Docker\Docker\Docker Desktop.exe"

# Pedidos a localhost sem passar por nenhum proxy
_http = urllib.request.build_opener(urllib.request.ProxyHandler({}))


def responde(url, so_200=False):
    """True se o endereço responde (com so_200, só se responder 200 OK)."""
    try:
        with _http.open(url, timeout=3) as r:
            return r.status == 200 or not so_200
    except urllib.error.HTTPError:
        return not so_200  # respondeu, mesmo que com um erro (ex.: 401)
    except OSError:
        return False


def docker_pronto():
    if not shutil.which("docker"):
        return False  # com o Docker Desktop desligado, o comando docker não existe no Ubuntu
    try:
        r = subprocess.run(["docker", "info"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=20)
        return r.returncode == 0
    except subprocess.TimeoutExpired:
        return False


def esperar(nome, pronto, segundos):
    print(nome, end="", flush=True)
    fim = time.time() + segundos
    while time.time() < fim:
        if pronto():
            print(" pronto.")
            return True
        print(".", end="", flush=True)
        time.sleep(2)
    print(f" não respondeu em {segundos} segundos.")
    return False


def compose(pasta, *args):
    return subprocess.run(["docker", "compose", *args], cwd=pasta).returncode == 0


def ligar_docker_desktop():
    """Abre o Docker Desktop no Windows. Devolve False se não o encontrar."""
    if not (os.path.exists(DOCKER_DESKTOP_WSL) and shutil.which("powershell.exe")):
        return False
    subprocess.run(["powershell.exe", "-NoProfile", "-Command", f"Start-Process '{DOCKER_DESKTOP_WIN}'"],
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    return True


def encontrar_npm():
    """O npm está no PATH num terminal normal; se não estiver, procura-o no nvm."""
    npm = shutil.which("npm")
    if npm:
        return npm
    versoes = sorted(glob.glob(os.path.expanduser("~/.nvm/versions/node/*/bin/npm")))
    if versoes:
        os.environ["PATH"] = str(Path(versoes[-1]).parent) + os.pathsep + os.environ["PATH"]
        return versoes[-1]
    sys.exit("Não encontrei o npm (Node.js). Instale-o com o nvm, como no README do frontend.")


def abrir_browser():
    """Abre a app no browser do Windows (no Ubuntu/WSL) ou no browser do sistema."""
    for comando in ("wslview", "explorer.exe"):
        if shutil.which(comando):
            subprocess.run([comando, URL_APP], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            return
    import webbrowser
    webbrowser.open(URL_APP)


def abrir_quando_pronta():
    for _ in range(60):
        if responde(URL_APP):
            abrir_browser()
            return
        time.sleep(1)


def arrancar():
    print("=== DriveNow ===")

    # 1. Docker
    if not docker_pronto():
        if ligar_docker_desktop():
            print("A abrir o Docker Desktop...")
        else:
            print("Abra o Docker Desktop no Windows (fico à espera).")
        if not esperar("Docker", docker_pronto, 180):
            sys.exit("O Docker não arrancou. Abra o Docker Desktop e corra outra vez: python3 app.py")
    else:
        print("Docker pronto.")

    # 2. Odoo (primeiro, porque o backend precisa dele)
    if not compose(ODOO, "up", "-d"):
        sys.exit("O Odoo não arrancou (veja a mensagem do docker compose acima).")
    if not esperar(f"Odoo ({URL_ODOO})", lambda: responde(URL_ODOO, so_200=True), 180):
        sys.exit("Veja o Odoo com: cd ~/drivenow/infra/odoo && docker compose logs --tail=40 odoo")

    # 3. Backend (--build: se o código mudou, compila outra vez; se não mudou, é rápido)
    if not compose(BACKEND, "up", "-d", "--build"):
        sys.exit("O backend não arrancou (veja a mensagem do docker compose acima; falta o src/backend/.env?).")
    if not esperar("Backend (http://localhost:8080)", lambda: responde(URL_BACKEND), 180):
        sys.exit("Veja o backend com: cd ~/drivenow/src/backend && docker compose logs --tail=40 api")

    # 4. App (frontend)
    if responde(URL_APP):
        print(f"A app já está ligada noutra janela: {URL_APP}")
        abrir_browser()
        return
    npm = encontrar_npm()
    if not (FRONTEND / "node_modules").is_dir():
        print("Primeira vez: a instalar as dependências da app (npm install)...")
        subprocess.run([npm, "install"], cwd=FRONTEND, check=True)
    print(f"\nApp em {URL_APP} (abre sozinha no browser).")
    print("Para parar a app: Ctrl+C. Para desligar também o Odoo e o backend: python3 app.py parar\n")
    threading.Thread(target=abrir_quando_pronta, daemon=True).start()
    try:
        subprocess.run([npm, "run", "dev"], cwd=FRONTEND)
    except KeyboardInterrupt:
        pass
    print("\nApp parada. O Odoo e o backend continuam ligados (python3 app.py parar para os desligar).")


def parar():
    if not docker_pronto():
        print("O Docker está desligado: não há nada a desligar.")
        return
    print("A desligar o backend...")
    compose(BACKEND, "stop")
    print("A desligar o Odoo...")
    compose(ODOO, "stop")
    print("DriveNow desligada. Os dados ficam guardados.")


def estado():
    linhas = [
        ("Docker", docker_pronto()),
        (f"Odoo     {URL_ODOO}", responde(URL_ODOO, so_200=True)),
        ("Backend  http://localhost:8080", responde(URL_BACKEND)),
        (f"App      {URL_APP}", responde(URL_APP)),
    ]
    for nome, ligado in linhas:
        print(f"{'ligado   ' if ligado else 'desligado'}  {nome}")


if __name__ == "__main__":
    acao = sys.argv[1] if len(sys.argv) > 1 else "arrancar"
    try:
        {"arrancar": arrancar, "parar": parar, "estado": estado}.get(acao, lambda: print(__doc__))()
    except KeyboardInterrupt:
        sys.exit("\nCancelado.")
