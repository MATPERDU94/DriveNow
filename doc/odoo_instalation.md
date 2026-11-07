# Instalação do Odoo – DriveNow

Guia para pôr o Odoo do projeto DriveNow a funcionar numa máquina nova, com a frota, a tabela de custos de danos e o módulo `drivenow_danos`.

## O que está no repositório

| Pasta / ficheiro | Conteúdo |
|---|---|
| `infra/odoo/docker-compose.yml` | Define os contentores do Odoo 19 e do PostgreSQL 16 |
| `infra/odoo/addons/drivenow_danos/` | Módulo DriveNow: menu **Frota → Danos** (tabela de custos e registo de danos) |
| `backup/*.zip` | Backup completo da base de dados (frota, danos, utilizadores, imagens) |
| `CSV/` | `FROTA.csv` (base original) e ficheiros de importação para o Odoo |

> O código (docker-compose e módulo) vem pelo Git. Os **dados** (viaturas, danos registados, utilizadores) estão no backup `.zip`.

---

## 1. Pré-requisitos

- **Git**
- **Docker** com Docker Compose
  - Windows: instalar o **Docker Desktop** e ativar *Settings → Resources → WSL Integration → Ubuntu*
  - Linux: `sudo apt install -y docker.io docker-compose-v2` e depois `sudo usermod -aG docker $USER` (terminar sessão e voltar a entrar)

Para confirmar:

```bash
docker --version
docker compose version
```

## 2. Clonar o repositório

```bash
cd ~
git clone https://github.com/MATPERDU94/DriveNow.git drivenow
```

O repositório é privado, por isso pede autenticação. Usa o utilizador do GitHub e um **token** como password.

Se já tiveres o repositório clonado, basta:

```bash
cd ~/drivenow
git pull
```

## 3. Arrancar o Odoo

```bash
cd ~/drivenow/infra/odoo
docker compose up -d
docker compose ps
```

Os dois contentores (`odoo-db-1` e `odoo-odoo-1`) devem estar **Up**. A primeira vez demora uns minutos a descarregar as imagens.

Abrir no browser: **http://localhost:8069**

## 4. Restaurar a base de dados a partir do backup (recomendado)

Com esta opção ficas com tudo igual: frota, tabela de danos, utilizadores e módulo já instalado.

> O passo 2 (`git clone` / `git pull`) tem de ser feito **antes**, porque a base de dados precisa de encontrar o módulo `drivenow_danos` na pasta `addons`.

1. Abrir **http://localhost:8069/web/database/manager**
2. Clicar em **Restore Database**
3. **Master Password**: a master password do Odoo. Numa instalação nova, define-se aqui.
4. **File**: escolher o `.zip` da pasta `backup/` do projeto
   - No Windows com WSL, a pasta abre-se no Explorador em `\\wsl.localhost\Ubuntu\home\<utilizador>\drivenow\backup`
5. **Database Name**: um nome à escolha (por exemplo `drivenow`)
6. Escolher **This database was copied** e clicar em **Continue**
7. Entrar com o login de administrador da base original

## 5. Alternativa: começar com uma base de dados vazia

Usa esta opção se não tiveres o backup.

1. Abrir **http://localhost:8069**, criar uma base de dados nova (nome, email e password de administrador; idioma Português)
2. **Apps** → procurar **Frota** → **Ativar**
3. Ativar o **modo de programador**: *Definições* → fundo da página → **Ativar o modo de programador**
4. **Apps** → **Atualizar lista de aplicações** → **Atualizar**
5. Apagar o filtro "Apps" na pesquisa, procurar **DriveNow** → **Ativar** no **DriveNow - Danos**
   - Os 16 tipos de dano da tabela de custos entram automaticamente
6. Importar a frota pela ordem indicada, em cada lista: vista de lista → ⚙️ → **Importar registos** → **Testar** → **Importar**

| Ordem | Ficheiro | Onde importar |
|---|---|---|
| 1º | `CSV/1_marcas.csv` | Frota → Configuração → **Fabricantes** |
| 2º | `CSV/2_modelos.csv` | Frota → Configuração → **Modelos** |
| 3º | `CSV/3_veiculos.csv` | Frota → Frota → **Veículos** |

A ordem é obrigatória: cada veículo depende de um modelo, e cada modelo de um fabricante.

## 6. Verificar

- **Frota → Veículos**: 30 viaturas
- **Frota → Danos → Tabela de custos**: 16 tipos de dano, com gravidade, custo e se imobilizam o carro
- **Frota → Danos → Registo de danos**: ocorrências de danos por viatura

---

## Fazer um novo backup

1. **http://localhost:8069/web/database/manager** → **Backup** ao lado da base de dados
2. Master Password, formato **zip (includes filestore)** → **Backup**
3. Copiar o `.zip` para `~/drivenow/backup/` e fazer commit e push

> O GitHub recusa ficheiros com mais de **100 MB**. Se o backup for maior, partilha-o por outro meio (OneDrive, pen).

## Atualizar o módulo depois de alterações no código

```bash
cd ~/drivenow
git pull
cd infra/odoo
docker compose restart odoo
```

Depois, no Odoo (em modo de programador): **Apps** → procurar **DriveNow** → ⋮ → **Atualizar**.

## Comandos úteis

| Ação | Comando (em `~/drivenow/infra/odoo`) |
|---|---|
| Arrancar | `docker compose up -d` |
| Parar | `docker compose down` |
| Estado | `docker compose ps` |
| Ver logs do Odoo | `docker compose logs -f odoo` |
| Reiniciar o Odoo | `docker compose restart odoo` |

> ⚠️ **Nunca** usar `docker compose down -v`: o `-v` apaga os volumes, ou seja, a base de dados inteira.

## Problemas comuns

- **O menu "Danos" não aparece**: o módulo não está instalado. Confirma que a pasta `infra/odoo/addons/drivenow_danos` existe e que o `docker-compose.yml` tem a linha `- ./addons:/mnt/extra-addons`. Depois reinicia o Odoo e atualiza a lista de aplicações.
- **A frota desapareceu depois de mexer no `docker-compose.yml`**: confirma que o serviço `db` tem o volume `- odoo-db:/var/lib/postgresql/data`. Os dados continuam no volume; basta repor a linha e fazer `docker compose up -d`.
- **Erro ao apagar um modelo ou fabricante** ("Another model is using the record"): apaga primeiro os veículos, depois os modelos e só depois os fabricantes. Verifica também os registos arquivados (filtro **Arquivado**).
