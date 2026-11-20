# DriveNow — app web do cliente (React)

Frontend da DriveNow, feito a partir dos mockups (`doc/Mockups`) e ligado à API do backend Spring Boot (`src/backend`).
Tem os ecrãs todos do percurso do cliente: entrar, criar conta, mapa, reserva, viagem, viagem terminada, viagens e faturas e perfil.

Tecnologia: Vite + React 18 + TypeScript, React Router, Leaflet (mapa OpenStreetMap) e lucide-react (ícones).

## Arrancar

Precisa de Node.js 18 ou mais recente (`node -v`). No WSL, dentro do repositório:

```bash
cd ~/drivenow/src/frontend
npm install          # só da primeira vez
```

### Com o backend (modo normal)

1. Arrancar o backend como de costume (porta 8080), com o Odoo e o n8n ligados.
2. Arrancar o frontend:

```bash
npm run dev
```

3. Abrir http://localhost:5173

O Vite reencaminha `/api` para `http://localhost:8080`, por isso não há problemas de CORS. Para usar outro endereço:
`API_URL=http://outro-endereco:8080 npm run dev`.

### Sem backend (modo demonstração)

```bash
npm run demo
```

A API é simulada no próprio browser, com as mesmas regras e mensagens do backend (uma reserva de cada vez, dívidas,
cancelamento, danos no início e no fim, franquia de 1000 €, km da viagem e preço por km, pagamento e fatura). Serve para testar o
frontend e como plano B numa apresentação. O botão **Demo** (canto do ecrã) repõe os dados de exemplo.

| Conta de exemplo | Password |
|---|---|
| ana.silva@exemplo.pt | drivenow123 |

### Num só ficheiro (plano B para a apresentação)

```bash
npm run build:ficheiro
```

Gera `dist-ficheiro/index.html`: a versão demonstração completa num único ficheiro HTML. Abre-se com duplo clique em
qualquer computador, sem Node nem servidor (pode ir numa pen). Sem internet, o mapa passa a um fundo ilustrativo e o
resto funciona igual.

## Dados de teste

| O quê | Valor | Resultado |
|---|---|---|
| Cartão | 4242 4242 4242 4242 | pagamento aprovado |
| Cartão | 4000 0000 0000 0002 | pagamento recusado (fica em dívida) |
| IBAN | PT50 0002 0123 1234 5678 9015 4 | válido |
| NIF | 123456789 | válido |

## Ecrãs e API usada

| Ecrã | Rota | Pedidos ao backend |
|---|---|---|
| Iniciar sessão | `/entrar` | `POST /api/auth/login` |
| Criar conta (dados + cartão ou IBAN) | `/criar-conta` | `POST /api/auth/registar` |
| Mapa com os carros livres | `/mapa` | `GET /api/viaturas?lat&lng&disponiveis=true`, `POST /api/reservas` |
| Reserva ativa / em viagem | `/viagem` | `GET /api/reservas/ativa`, `GET /api/viaturas/{id}`, `GET /api/danos/tipos`, `POST /api/reservas/{id}/relatos` (INICIO e FIM), `POST /api/reservas/{id}/cancelar` |
| Viagem terminada | `/viagens/{id}` | `GET /api/reservas/{id}`, `GET /api/reservas/{id}/relatos`, `GET /api/reservas/{id}/trajeto`, `POST /api/reservas/{id}/faturar` |
| Viagens e faturas | `/viagens` | `GET /api/reservas`, `GET /api/viaturas` |
| Perfil | `/perfil` | `GET/PUT /api/me`, `PUT /api/me/pagamento`, `POST /api/me/cobrancas/pagar` |

O token JWT do login fica no `localStorage` e vai em `Authorization: Bearer ...`. Se o backend responder 401, a app volta ao ecrã de entrada.
Os erros do backend (ProblemDetail) aparecem tal como vêm, por exemplo "Já tem uma reserva ativa".

## Regras visíveis na app

- O temporizador começa na reserva; cada minuto começado conta inteiro.
- Valor = taxa de entrada + minutos × preço/min + km × preço/km + penalizações (IVA incluído). O preço/km vem do Tarifário do Odoo e
  aparece no mapa e na confirmação da reserva (com um exemplo de 30 min e 10 km).
- Durante a viagem, o "valor até agora" ainda não tem os km: somam-se no fim.
- Antes de começar a viagem, o cliente indica o estado do carro ("sem danos" ou os danos encontrados). Esses danos são cobrados ao condutor anterior.
- Para terminar: estado do carro, quilómetros do painel (obrigatórios; não podem ser inferiores aos do início da viagem nem dar mais de
  1000 km) e onde deixou o carro (GPS ou toque no mapa). Ao escrever os km, a app mostra os km da viagem e quanto custam.
- O resumo da viagem mostra a distância (km do painel no início e no fim), o tempo, os km e o total; a lista de viagens mostra os km.
- Cancelar a reserva não conta km (o carro não saiu do sítio).
- O resumo de uma viagem concluída mostra o **trajeto**: o percurso pelas ruas entre onde o carro estava na reserva e onde o
  cliente o deixou, calculado pelo backend com o serviço de rotas OSRM. É simulado (o carro não é seguido por GPS); se o serviço
  não responder, aparece a linha reta a tracejado. No modo demonstração o percurso é desenhado "em escada" entre os dois pontos.
- Pagamento recusado → dívida: não pode reservar até mudar o meio de pagamento no perfil (a dívida é cobrada logo a seguir).
- A fatura aparece como "Pendente" até o sistema de faturação responder; o ecrã vai-se atualizando sozinho.

## Configuração (opcional)

Copiar `.env.example` para `.env`:

| Variável | Para quê |
|---|---|
| `API_URL` | Backend usado pelo proxy do `npm run dev` (por omissão `http://localhost:8080`) |
| `VITE_API_URL` | Endereço completo da API quando o frontend é servido noutro sítio (ex.: túnel do Cloudflare) |
| `VITE_TILES_URL` | Outro servidor de mapas (por omissão OpenStreetMap) |

## Build

```bash
npm run build        # versão normal em dist/
npm run build:demo   # versão demonstração em dist-demo/ (estática, não precisa de backend)
npm run build:ficheiro  # versão demonstração num só ficheiro: dist-ficheiro/index.html
npx vite preview --mode demo --outDir dist-demo   # servir a versão demonstração
```

## Telemóvel

O `npm run dev` mostra também um endereço "Network" (ex.: `http://192.168.1.20:5173`). Com o telemóvel na mesma rede Wi-Fi,
abre-se a app nesse endereço. Num endereço sem https o browser não dá a localização, por isso o mapa mostra o centro de Guimarães.

## Estrutura

```
src/
  api/         cliente HTTP (client.ts) e tipos iguais aos DTO do backend (types.ts)
  state/       sessão (utilizador, reserva ativa) e avisos (toasts)
  components/  cabeçalho, mapa, formulário de pagamento, modal do estado do carro, botões e campos
  pages/       um ficheiro por ecrã
  lib/         formatação (€, datas, distâncias), validações (NIF, cartão, IBAN) e localização
  demo/        API simulada do modo demonstração
  styles.css   cores, tipografia e componentes dos mockups
  pages.css    layout de cada ecrã (computador e telemóvel)
```
