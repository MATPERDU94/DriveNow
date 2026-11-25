# Ligar tudo (no Ubuntu/WSL): Docker, Odoo, backend e app, e abre o browser
cd ~/drivenow && python3 app.py

# Ver o que está ligado / desligar o Odoo e o backend
cd ~/drivenow && python3 app.py estado
cd ~/drivenow && python3 app.py parar

# O mesmo à mão, passo a passo (é o que o app.py faz)
cd ~/drivenow/infra/odoo && docker compose up -d
cd ~/drivenow/src/backend && docker compose up -d --build
cd ~/drivenow/src/frontend && npm run dev
