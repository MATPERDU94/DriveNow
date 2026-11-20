import type {
  Cobranca,
  PedidoMeioPagamento,
  PedidoRegisto,
  PedidoRelato,
  Relato,
  Reserva,
  Sessao,
  TipoDano,
  Trajeto,
  Utilizador,
  Viatura,
} from './types';

/** true no modo demonstração (`npm run demo` ou `npm run build:ficheiro`): API simulada no browser, sem backend. */
export const MODO_DEMO = import.meta.env.MODE === 'demo' || import.meta.env.MODE === 'ficheiro';

/** Versão num só ficheiro HTML (abre com duplo clique, sem Node nem servidor). */
export const MODO_FICHEIRO = import.meta.env.MODE === 'ficheiro';

const BASE = (import.meta.env.VITE_API_URL as string | undefined)?.replace(/\/$/, '') ?? '';
const CHAVE_TOKEN = 'drivenow.token';

/** Erro devolvido pela API (formato ProblemDetail do Spring: title, detail, status). */
export class ApiError extends Error {
  constructor(
    public status: number,
    public titulo: string,
    public detalhe: string,
  ) {
    super(detalhe || titulo);
  }
}

let tokenEmMemoria: string | null = null;

export function lerToken(): string | null {
  if (tokenEmMemoria) return tokenEmMemoria;
  try {
    tokenEmMemoria = localStorage.getItem(CHAVE_TOKEN);
  } catch {
    /* storage indisponível: fica só em memória */
  }
  return tokenEmMemoria;
}

export function guardarToken(token: string | null) {
  tokenEmMemoria = token;
  try {
    if (token) localStorage.setItem(CHAVE_TOKEN, token);
    else localStorage.removeItem(CHAVE_TOKEN);
  } catch {
    /* ignorar */
  }
}

type OuvinteSessao = () => void;
const ouvintesSessaoExpirada = new Set<OuvinteSessao>();
export function aoExpirarSessao(fn: OuvinteSessao) {
  ouvintesSessaoExpirada.add(fn);
  return () => ouvintesSessaoExpirada.delete(fn);
}

function mensagemPorEstado(status: number): string {
  if (status === 0) return 'Sem ligação ao servidor. Confirme que o backend está a correr.';
  if (status === 401) return 'A sessão terminou. Entre outra vez.';
  if (status === 403) return 'Não tem permissão para esta ação.';
  if (status === 404) return 'Não encontrado.';
  if (status === 502) return 'Um dos sistemas da DriveNow não respondeu. Tente outra vez daqui a pouco.';
  if (status >= 500) return 'Erro no servidor. Tente outra vez daqui a pouco.';
  return 'Não foi possível concluir o pedido.';
}

async function pedido<T>(metodo: string, caminho: string, corpo?: unknown): Promise<T> {
  const token = lerToken();

  if (MODO_DEMO) {
    const { tratarPedidoDemo } = await import('../demo/mockApi');
    const r = await tratarPedidoDemo(metodo, caminho, corpo, token);
    if (r.status >= 400) {
      const p = (r.corpo ?? {}) as { title?: string; detail?: string };
      if (r.status === 401 && token) ouvintesSessaoExpirada.forEach((f) => f());
      throw new ApiError(r.status, p.title ?? 'Erro', p.detail ?? mensagemPorEstado(r.status));
    }
    return (r.status === 204 ? null : r.corpo) as T;
  }

  let res: Response;
  try {
    res = await fetch(`${BASE}/api${caminho}`, {
      method: metodo,
      headers: {
        Accept: 'application/json',
        ...(corpo !== undefined ? { 'Content-Type': 'application/json' } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: corpo !== undefined ? JSON.stringify(corpo) : undefined,
    });
  } catch {
    throw new ApiError(0, 'Sem ligação', mensagemPorEstado(0));
  }

  if (res.status === 204) return null as T;

  const texto = await res.text();
  let dados: unknown = null;
  if (texto) {
    try {
      dados = JSON.parse(texto);
    } catch {
      dados = null;
    }
  }

  if (!res.ok) {
    if (res.status === 401 && token) ouvintesSessaoExpirada.forEach((f) => f());
    const p = (dados ?? {}) as { title?: string; detail?: string };
    // O Spring devolve "Unauthorized" sem detalhe quando falta o token
    const detalhe = p.detail && p.detail !== 'Unauthorized' ? p.detail : mensagemPorEstado(res.status);
    throw new ApiError(res.status, p.title ?? 'Erro', detalhe);
  }
  return dados as T;
}

const q = (params: Record<string, string | number | boolean | undefined>) => {
  const s = Object.entries(params)
    .filter(([, v]) => v !== undefined)
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
    .join('&');
  return s ? `?${s}` : '';
};

export const api = {
  // Autenticação
  login: (email: string, password: string) => pedido<Sessao>('POST', '/auth/login', { email, password }),
  registar: (p: PedidoRegisto) => pedido<Sessao>('POST', '/auth/registar', p),

  // Perfil
  eu: () => pedido<Utilizador>('GET', '/me'),
  atualizarDados: (d: { nome: string; email: string; nif: string; telefone: string }) =>
    pedido<Utilizador>('PUT', '/me', d),
  alterarPagamento: (p: PedidoMeioPagamento) => pedido<Utilizador>('PUT', '/me/pagamento', p),
  cobrancas: () => pedido<Cobranca[]>('GET', '/me/cobrancas'),
  pagarDividas: () => pedido<Cobranca[]>('POST', '/me/cobrancas/pagar'),

  // Frota
  viaturas: (p: { lat?: number; lng?: number; raio?: number; disponiveis?: boolean }) =>
    pedido<Viatura[]>('GET', `/viaturas${q(p)}`),
  viatura: (id: number) => pedido<Viatura>('GET', `/viaturas/${id}`),
  tiposDano: () => pedido<TipoDano[]>('GET', '/danos/tipos'),

  // Reservas e viagens
  reservar: (viaturaId: number) => pedido<Reserva>('POST', '/reservas', { viaturaId }),
  reservas: () => pedido<Reserva[]>('GET', '/reservas'),
  reservaAtiva: () => pedido<Reserva | null>('GET', '/reservas/ativa'),
  reserva: (id: number) => pedido<Reserva>('GET', `/reservas/${id}`),
  cancelar: (id: number) => pedido<Reserva>('POST', `/reservas/${id}/cancelar`),
  faturar: (id: number) => pedido<Reserva>('POST', `/reservas/${id}/faturar`),
  relatar: (reservaId: number, r: PedidoRelato) => pedido<Relato>('POST', `/reservas/${reservaId}/relatos`, r),
  relatos: (reservaId: number) => pedido<Relato[]>('GET', `/reservas/${reservaId}/relatos`),
  /** null se a reserva não tiver trajeto (cancelada) */
  trajeto: (reservaId: number) => pedido<Trajeto | null>('GET', `/reservas/${reservaId}/trajeto`),
};

/** Abre a fatura (PDF do Moloni). No modo demonstração gera um recibo de exemplo. */
export async function abrirFatura(r: Reserva, modelo: string, email?: string) {
  if (MODO_DEMO) {
    const { abrirFaturaDemo } = await import('../demo/mockApi');
    abrirFaturaDemo(r, modelo, email);
    return;
  }
  if (r.faturaPdfUrl) window.open(r.faturaPdfUrl, '_blank', 'noopener');
}
