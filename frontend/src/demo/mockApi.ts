/**
 * API simulada para o modo demonstração (npm run demo).
 * Segue as mesmas regras e mensagens do backend Spring Boot (ReservaService, RelatoService,
 * PagamentoService, AuthService), para a app poder ser mostrada sem backend, Odoo nem n8n.
 * Os dados ficam no localStorage deste browser.
 */
import type {
  Cobranca,
  DanoRelato,
  MeioPagamento,
  PedidoMeioPagamento,
  PedidoRegisto,
  PedidoRelato,
  Relato,
  Reserva,
  TipoDano,
  Utilizador,
  Viatura,
} from '../api/types';
import { LOCAIS, distanciaKm } from '../lib/geo';
import { euro, data, hora, semIva } from '../lib/format';
import { ibanOk, luhn, marcaCartao, nifValido, validadeOk } from '../lib/validacao';

const CHAVE = 'drivenow.demo.v1';
const CARTAO_RECUSA = '4000000000000002';
const FRANQUIA = 1000;

// Matrícula, modelo (como vem do Odoo), ano, lugares, km, taxa de entrada, preço/min — CSV/Odoo do repositório
const FROTA: [string, string, number, number, number, number, number][] = [
  ['12-AB-34', 'Tesla/Model 3', 2023, 5, 12500, 35, 0.3],
  ['56-CD-78', 'Tesla/Model Y', 2023, 5, 9800, 35, 0.33],
  ['90-EF-12', 'Nissan/Leaf', 2022, 5, 22000, 10, 0.2],
  ['34-GH-56', 'Renault/Zoe', 2022, 5, 18500, 10, 0.18],
  ['78-IJ-90', 'BMW/i3', 2021, 4, 27000, 20, 0.22],
  ['11-KL-22', 'Hyundai/Kona Electric', 2023, 5, 11200, 20, 0.25],
  ['33-MN-44', 'Kia/e-Niro', 2023, 5, 13400, 20, 0.24],
  ['55-OP-66', 'Volkswagen/ID.3', 2023, 5, 8900, 20, 0.26],
  ['77-QR-88', 'Volkswagen/ID.4', 2023, 5, 10500, 35, 0.29],
  ['99-ST-00', 'Peugeot/e-208', 2022, 5, 19800, 10, 0.19],
  ['22-UV-11', 'Opel/Corsa-e', 2022, 5, 21000, 10, 0.19],
  ['44-WX-33', 'MG/ZS EV', 2023, 5, 14200, 20, 0.23],
  ['66-YZ-55', 'Audi/Q4 e-tron', 2023, 5, 7600, 60, 0.35],
  ['88-AA-77', 'Mercedes-Benz/EQA', 2023, 5, 6900, 60, 0.37],
  ['10-BB-99', 'Polestar/2', 2023, 5, 8300, 35, 0.32],
  ['13-CC-21', 'Cupra/Born', 2023, 5, 9100, 35, 0.27],
  ['35-DD-43', 'Fiat/500e', 2022, 4, 17000, 10, 0.17],
  ['57-EE-65', 'Smart/EQ Fortwo', 2021, 2, 24000, 5, 0.15],
  ['79-FF-87', 'Tesla/Model S', 2023, 5, 5400, 100, 0.43],
  ['91-GG-09', 'Tesla/Model X', 2023, 7, 4800, 100, 0.48],
  ['24-HH-13', 'Porsche/Taycan', 2024, 4, 3900, 200, 0.63],
  ['46-II-35', 'Porsche/Taycan', 2023, 4, 8200, 200, 0.63],
  ['68-JJ-57', 'Hyundai/Ioniq 5', 2024, 5, 6100, 35, 0.28],
  ['80-KK-79', 'Kia/EV6', 2024, 5, 5800, 35, 0.29],
  ['02-LL-91', 'Ford/Mustang Mach-E', 2023, 5, 9400, 35, 0.33],
  ['14-MM-23', 'Volvo/EX30', 2024, 5, 4200, 20, 0.26],
  ['36-NN-45', 'Skoda/Enyaq', 2023, 5, 10900, 35, 0.28],
  ['58-OO-67', 'BYD/Atto 3', 2024, 5, 5100, 20, 0.23],
  ['70-PP-89', 'BMW/iX1', 2024, 5, 4600, 35, 0.31],
  ['92-QQ-01', 'Dacia/Spring', 2023, 4, 15300, 5, 0.14],
];

// Tabela de danos do módulo drivenow_danos (Odoo)
const DANOS: Omit<TipoDano, 'id'>[] = [
  { nome: 'Risco ligeiro na pintura', gravidade: 'ligeiro', custo: 50, imobilizaCarro: false, descricao: 'Arranhão superficial, até 10 cm.' },
  { nome: 'Risco profundo na pintura', gravidade: 'moderado', custo: 150, imobilizaCarro: false, descricao: 'Risco que atinge o metal ou com mais de 10 cm.' },
  { nome: 'Amolgadela pequena', gravidade: 'moderado', custo: 120, imobilizaCarro: false, descricao: 'Mossa até 5 cm sem danos na pintura.' },
  { nome: 'Amolgadela grande', gravidade: 'grave', custo: 350, imobilizaCarro: true, descricao: 'Mossa com mais de 5 cm ou pintura danificada.' },
  { nome: 'Para-choques danificado', gravidade: 'moderado', custo: 250, imobilizaCarro: false, descricao: 'Rachado, solto ou deformado.' },
  { nome: 'Espelho retrovisor partido', gravidade: 'moderado', custo: 180, imobilizaCarro: false, descricao: 'Espelho ou caixa do retrovisor partidos.' },
  { nome: 'Para-brisas lascado', gravidade: 'ligeiro', custo: 80, imobilizaCarro: false, descricao: 'Pequena lasca sem fissura.' },
  { nome: 'Vidro partido ou fissurado', gravidade: 'grave', custo: 400, imobilizaCarro: true, descricao: 'Para-brisas fissurado ou vidro partido.' },
  { nome: 'Jante riscada', gravidade: 'ligeiro', custo: 70, imobilizaCarro: false, descricao: 'Jante raspada num passeio.' },
  { nome: 'Pneu furado ou danificado', gravidade: 'moderado', custo: 120, imobilizaCarro: true, descricao: 'Pneu furado, cortado ou rebentado.' },
  { nome: 'Farol ou farolim partido', gravidade: 'moderado', custo: 200, imobilizaCarro: false, descricao: 'Ótica dianteira ou traseira partida.' },
  { nome: 'Interior sujo', gravidade: 'ligeiro', custo: 40, imobilizaCarro: false, descricao: 'Necessita de limpeza além do normal.' },
  { nome: 'Estofos danificados', gravidade: 'moderado', custo: 150, imobilizaCarro: false, descricao: 'Queimaduras, rasgões ou manchas nos bancos.' },
  { nome: 'Cabo de carregamento danificado ou em falta', gravidade: 'moderado', custo: 250, imobilizaCarro: false, descricao: 'Cabo danificado ou não devolvido.' },
  { nome: 'Acidente com outro veículo', gravidade: 'grave', custo: 1000, imobilizaCarro: true, descricao: 'Colisão com outro veículo; valor da franquia.' },
  { nome: 'Avaria mecânica ou elétrica', gravidade: 'grave', custo: 0, imobilizaCarro: true, descricao: 'Avaria não causada pelo cliente; sem custo.' },
];
const TIPOS_DANO: TipoDano[] = DANOS.map((d, i) => ({ id: i + 1, ...d }));

interface MeioInterno extends MeioPagamento {
  recusa: boolean;
}
interface UtilizadorInterno {
  id: number;
  nome: string;
  email: string;
  nif: string;
  telefone: string | null;
  password: string;
  criadoEm: string;
  meio: MeioInterno | null;
}
interface ViaturaInterna {
  id: number;
  matricula: string;
  modelo: string;
  ano: number;
  lugares: number;
  km: number;
  taxa: number;
  preco: number;
  lat: number;
  lng: number;
  estado: 'livre' | 'reparacao' | 'documentos';
  /** tipos de dano registados e ainda não reparados */
  danosAbertos: number[];
}
interface ReservaInterna {
  id: number;
  utilizadorId: number;
  viaturaId: number;
  matricula: string;
  estado: Reserva['estado'];
  criadaEm: string;
  inicioViagem: string | null;
  fimViagem: string | null;
  taxaEntrada: number;
  precoMinuto: number;
  totalPenalizacoes: number;
  estadoPagamento: Reserva['estadoPagamento'];
  pagamentoDescricao: string | null;
  estadoFatura: Reserva['estadoFatura'];
  faturaPrevistaEm: number | null;
  moloniDocumentoId: number | null;
}
interface Estado {
  versao: 1;
  seq: { u: number; r: number; rel: number; c: number; fatura: number };
  utilizadores: UtilizadorInterno[];
  viaturas: ViaturaInterna[];
  reservas: ReservaInterna[];
  relatos: Relato[];
  cobrancas: Cobranca[];
}

const r2 = (n: number) => Math.round(n * 100) / 100;
const agoraIso = () => new Date().toISOString();
const minutosEntre = (a: string, b: string) =>
  Math.max(1, Math.ceil((Date.parse(b) - Date.parse(a)) / 60000 - 1e-9));

function semente(): Estado {
  const viaturas: ViaturaInterna[] = FROTA.map(([matricula, modelo, ano, lugares, km, taxa, preco], i) => {
    const p = LOCAIS[i % LOCAIS.length];
    return {
      id: i + 1,
      matricula,
      modelo,
      ano,
      lugares,
      km,
      taxa,
      preco,
      lat: p.lat,
      lng: p.lng,
      estado: 'livre',
      danosAbertos: [],
    };
  });
  // Dois carros fora do mapa, para mostrar as regras (diagramas 4 e 5)
  viaturas[12].estado = 'reparacao';
  viaturas[19].estado = 'documentos';

  const ana: UtilizadorInterno = {
    id: 1,
    nome: 'Ana Silva',
    email: 'ana.silva@exemplo.pt',
    nif: '123456789',
    telefone: '912345678',
    password: 'drivenow123',
    criadoEm: new Date(Date.now() - 45 * 86400000).toISOString(),
    meio: {
      tipo: 'CARTAO',
      titular: 'ANA SILVA',
      marca: 'Visa',
      ultimosDigitos: '4242',
      validade: '08/28',
      descricao: 'Visa •••• 4242',
      expirado: false,
      recusa: false,
    },
  };

  // Histórico: [dias atrás, hora, minutos, viatura, nº fatura | null = pendente]
  const hist: [number, number, number, number, number | null][] = [
    [2, 10.2, 23, 4, 128],
    [4, 18.5, 35, 10, 121],
    [7, 9.1, 18, 17, null],
    [10, 13.4, 22, 1, 104],
    [13, 8.7, 14, 4, 97],
    [16, 19.2, 22, 10, 90],
    [41, 11.0, 31, 8, 61],
  ];
  const reservas: ReservaInterna[] = [];
  const cobrancas: Cobranca[] = [];
  hist.forEach(([dias, h, min, vid, fat], i) => {
    const v = viaturas[vid - 1];
    const ini = new Date(Date.now() - dias * 86400000);
    ini.setHours(Math.floor(h), Math.round((h % 1) * 60), 0, 0);
    const criadaEm = ini.toISOString();
    const inicioViagem = new Date(ini.getTime() + 3 * 60000).toISOString();
    const fimViagem = new Date(ini.getTime() + (min * 60 - 20) * 1000).toISOString();
    const id = i + 1;
    reservas.push({
      id,
      utilizadorId: 1,
      viaturaId: v.id,
      matricula: v.matricula,
      estado: 'CONCLUIDA',
      criadaEm,
      inicioViagem,
      fimViagem,
      taxaEntrada: v.taxa,
      precoMinuto: v.preco,
      totalPenalizacoes: 0,
      estadoPagamento: 'PAGO',
      pagamentoDescricao: 'Visa •••• 4242',
      estadoFatura: fat ? 'EMITIDA' : 'PENDENTE',
      faturaPrevistaEm: fat ? null : Date.now() + 365 * 86400000,
      moloniDocumentoId: fat,
    });
    cobrancas.push({
      id,
      reservaId: id,
      tipo: 'VIAGEM',
      valor: r2(v.taxa + min * v.preco),
      estado: 'PAGA',
      meio: 'Visa •••• 4242',
      motivoRecusa: null,
      tentativas: 1,
      criadaEm: fimViagem,
      pagaEm: fimViagem,
    });
  });

  return {
    versao: 1,
    seq: { u: 2, r: reservas.length + 1, rel: 1, c: cobrancas.length + 1, fatura: 129 },
    utilizadores: [ana],
    viaturas,
    reservas,
    relatos: [],
    cobrancas,
  };
}

let estado: Estado | null = null;

function carregar(): Estado {
  if (estado) return estado;
  try {
    const raw = localStorage.getItem(CHAVE);
    if (raw) {
      const e = JSON.parse(raw) as Estado;
      if (e.versao === 1) estado = e;
    }
  } catch {
    /* sem storage: começa do zero */
  }
  if (!estado) estado = semente();
  return estado;
}

function gravar() {
  try {
    localStorage.setItem(CHAVE, JSON.stringify(estado));
  } catch {
    /* ignorar */
  }
}

export function reporDemo() {
  estado = null;
  try {
    localStorage.removeItem(CHAVE);
    localStorage.removeItem('drivenow.token');
  } catch {
    /* ignorar */
  }
}

// ---------- Conversões para os DTO ----------

function meioDto(m: MeioInterno | null): MeioPagamento | null {
  if (!m) return null;
  const { recusa: _r, ...dto } = m;
  void _r;
  return { ...dto, expirado: m.validade ? !validadeOk(m.validade) : false };
}

function emDivida(uid: number) {
  return r2(carregar().cobrancas.filter((c) => c.estado === 'EM_DIVIDA' && cobrancaDe(c) === uid).reduce((s, c) => s + c.valor, 0));
}

function cobrancaDe(c: Cobranca) {
  return carregar().reservas.find((r) => r.id === c.reservaId)?.utilizadorId;
}

function utilizadorDto(u: UtilizadorInterno): Utilizador {
  return {
    id: u.id,
    nome: u.nome,
    email: u.email,
    nif: u.nif,
    telefone: u.telefone,
    criadoEm: u.criadoEm,
    meioPagamento: meioDto(u.meio),
    valorEmDivida: emDivida(u.id),
  };
}

function reservaAtivaDaViatura(vid: number) {
  return carregar().reservas.find((r) => r.viaturaId === vid && (r.estado === 'RESERVADA' || r.estado === 'EM_CURSO'));
}

function viaturaDto(v: ViaturaInterna, ref?: { lat: number; lng: number }): Viatura {
  const reservada = !!reservaAtivaDaViatura(v.id);
  const motivo =
    v.estado === 'reparacao' ? 'Em reparação' : v.estado === 'documentos' ? 'Documentos caducados' : reservada ? 'Reservada' : null;
  return {
    id: v.id,
    matricula: v.matricula,
    modelo: v.modelo,
    ano: v.ano,
    lugares: v.lugares,
    quilometragem: v.km,
    estado: v.estado === 'reparacao' ? 'Em reparação' : 'Disponível',
    taxaEntrada: v.taxa,
    precoMinuto: v.preco,
    latitude: v.lat,
    longitude: v.lng,
    disponivel: motivo == null,
    motivoIndisponibilidade: motivo,
    distanciaKm: ref ? Math.round(distanciaKm(ref, { lat: v.lat, lng: v.lng }) * 1000) / 1000 : null,
  };
}

function atualizarFaturas() {
  const e = carregar();
  let mudou = false;
  for (const r of e.reservas) {
    if (r.estadoFatura === 'PENDENTE' && r.estadoPagamento === 'PAGO' && r.faturaPrevistaEm && Date.now() >= r.faturaPrevistaEm) {
      r.estadoFatura = 'EMITIDA';
      r.moloniDocumentoId = e.seq.fatura++;
      r.faturaPrevistaEm = null;
      mudou = true;
    }
  }
  if (mudou) gravar();
}

function reservaDto(r: ReservaInterna): Reserva {
  const u = carregar().utilizadores.find((x) => x.id === r.utilizadorId);
  const fim = r.fimViagem ?? agoraIso();
  const minutos = minutosEntre(r.criadaEm, fim);
  const valorViagem = r2(r.taxaEntrada + minutos * r.precoMinuto);
  const terminada = r.estado === 'CONCLUIDA' || r.estado === 'CANCELADA';
  return {
    id: r.id,
    utilizadorId: r.utilizadorId,
    utilizadorNome: u?.nome ?? '',
    viaturaId: r.viaturaId,
    matricula: r.matricula,
    estado: r.estado,
    criadaEm: r.criadaEm,
    inicioViagem: r.inicioViagem,
    fimViagem: r.fimViagem,
    taxaEntrada: r.taxaEntrada,
    precoMinuto: r.precoMinuto,
    minutos: terminada ? minutos : null,
    valorViagem: terminada ? valorViagem : null,
    totalPenalizacoes: terminada ? r.totalPenalizacoes : null,
    total: terminada ? r2(valorViagem + r.totalPenalizacoes) : null,
    minutosDecorridos: minutos,
    valorAteAgora: valorViagem,
    estadoPagamento: r.estadoPagamento,
    pagamentoDescricao: r.pagamentoDescricao,
    estadoFatura: r.estadoFatura,
    moloniDocumentoId: r.moloniDocumentoId,
    faturaPdfUrl: r.estadoFatura === 'EMITIDA' ? `demo:fatura/${r.id}` : null,
    faturaErro: null,
  };
}

// ---------- Regras ----------

class Erro {
  constructor(
    public status: number,
    public title: string,
    public detail: string,
  ) {}
}
const regra = (msg: string) => new Erro(409, 'Pedido inválido', msg);
const naoEncontrado = (msg: string) => new Erro(404, 'Não encontrado', msg);

function validarDados(nome?: string, email?: string, nif?: string, telefone?: string | null) {
  if (!nome || !nome.trim()) throw regra('Indique o nome completo');
  if (!email || !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email.trim())) throw regra('Email inválido');
  if (!nif || !nifValido(nif)) throw regra('NIF inválido (9 dígitos com dígito de controlo correto)');
  const tel = telefone ? telefone.replace(/\s/g, '') : '';
  if (tel && !/^\+?\d{9,15}$/.test(tel)) throw regra('Telemóvel inválido');
}

function construirMeio(p: PedidoMeioPagamento | undefined): MeioInterno {
  if (!p || !p.tipo) throw regra('Indique o meio de pagamento (CARTAO ou CONTA_BANCARIA)');
  if (!p.titular || !p.titular.trim()) throw regra('Indique o titular do meio de pagamento');
  if (p.tipo === 'CARTAO') {
    const numero = (p.numero ?? '').replace(/[\s-]/g, '');
    if (!luhn(numero)) throw regra('Número de cartão inválido');
    if (!p.validade || !validadeOk(p.validade)) throw regra('Validade do cartão inválida ou expirada (formato MM/AA)');
    if (!p.cvv || !/^\d{3,4}$/.test(p.cvv)) throw regra('CVV inválido');
    const marca = marcaCartao(numero);
    const ult = numero.slice(-4);
    return {
      tipo: 'CARTAO',
      titular: p.titular.trim(),
      marca,
      ultimosDigitos: ult,
      validade: p.validade,
      descricao: `${marca} •••• ${ult}`,
      expirado: false,
      recusa: numero === CARTAO_RECUSA,
    };
  }
  const iban = (p.iban ?? '').replace(/[\s-]/g, '').toUpperCase();
  if (!ibanOk(iban)) throw regra('IBAN inválido (formato PT50 + 21 dígitos)');
  const ult = iban.slice(-4);
  return {
    tipo: 'CONTA_BANCARIA',
    titular: p.titular.trim(),
    marca: 'IBAN',
    ultimosDigitos: ult,
    validade: null,
    descricao: `IBAN •••• ${ult}`,
    expirado: false,
    recusa: false,
  };
}

/** Cobra a reserva no meio de pagamento do cliente (pagamento simulado, como no backend). */
function cobrar(r: ReservaInterna, valor: number) {
  const e = carregar();
  const u = e.utilizadores.find((x) => x.id === r.utilizadorId)!;
  const ok = !!u.meio && !u.meio.recusa && !(u.meio.validade && !validadeOk(u.meio.validade));
  const agora = agoraIso();
  e.cobrancas.push({
    id: e.seq.c++,
    reservaId: r.id,
    tipo: 'VIAGEM',
    valor: r2(valor),
    estado: ok ? 'PAGA' : 'EM_DIVIDA',
    meio: u.meio?.descricao ?? null,
    motivoRecusa: ok ? null : u.meio ? 'Pagamento recusado pelo banco (cartão de teste)' : 'Sem meio de pagamento',
    tentativas: 1,
    criadaEm: agora,
    pagaEm: ok ? agora : null,
  });
  r.estadoPagamento = ok ? 'PAGO' : 'EM_DIVIDA';
  r.pagamentoDescricao = u.meio?.descricao ?? null;
  r.estadoFatura = 'PENDENTE';
  // A fatura-recibo só é pedida ao Moloni depois de pago (simulado: fica pronta passados 6 s)
  r.faturaPrevistaEm = ok ? Date.now() + 6000 : null;
}

function cobrarDividas(uid: number) {
  const e = carregar();
  const u = e.utilizadores.find((x) => x.id === uid)!;
  const ok = !!u.meio && !u.meio.recusa && !(u.meio.validade && !validadeOk(u.meio.validade));
  for (const c of e.cobrancas) {
    if (c.estado !== 'EM_DIVIDA' || cobrancaDe(c) !== uid) continue;
    c.tentativas += 1;
    c.meio = u.meio?.descricao ?? null;
    if (ok) {
      c.estado = 'PAGA';
      c.pagaEm = agoraIso();
      c.motivoRecusa = null;
      const r = e.reservas.find((x) => x.id === c.reservaId);
      if (r) {
        r.estadoPagamento = 'PAGO';
        r.pagamentoDescricao = u.meio?.descricao ?? null;
        r.faturaPrevistaEm = Date.now() + 6000;
      }
    }
  }
}

// ---------- Roteamento ----------

interface Resposta {
  status: number;
  corpo?: unknown;
}

function utilizadorDoToken(token: string | null): UtilizadorInterno {
  const id = token?.startsWith('demo-') ? Number(token.slice(5)) : NaN;
  const u = carregar().utilizadores.find((x) => x.id === id);
  if (!u) throw new Erro(401, 'Unauthorized', 'A sessão terminou. Entre outra vez.');
  return u;
}

function sessao(u: UtilizadorInterno) {
  return {
    token: `demo-${u.id}`,
    expiraEm: new Date(Date.now() + 7 * 86400000).toISOString(),
    utilizador: utilizadorDto(u),
  };
}

function rotear(metodo: string, caminhoCompleto: string, corpo: unknown, token: string | null): Resposta {
  const e = carregar();
  atualizarFaturas();
  const [caminho, qs] = caminhoCompleto.split('?');
  const params = new URLSearchParams(qs ?? '');
  const m = (re: RegExp) => re.exec(caminho);
  const b = (corpo ?? {}) as Record<string, unknown>;

  // ----- Autenticação -----
  if (metodo === 'POST' && caminho === '/auth/login') {
    const email = String(b.email ?? '').trim().toLowerCase();
    const u = e.utilizadores.find((x) => x.email.toLowerCase() === email && x.password === b.password);
    if (!u) throw new Erro(401, 'Login falhou', 'Email ou password incorretos');
    return { status: 200, corpo: sessao(u) };
  }
  if (metodo === 'POST' && caminho === '/auth/registar') {
    const p = corpo as PedidoRegisto;
    validarDados(p.nome, p.email, p.nif, p.telefone);
    if (e.utilizadores.some((x) => x.email.toLowerCase() === p.email.trim().toLowerCase()))
      throw regra(`Já existe uma conta com o email ${p.email.trim()}`);
    if (!p.password || p.password.length < 8) throw regra('A password tem de ter pelo menos 8 caracteres');
    if (!p.aceitaTermos) throw regra('É preciso aceitar os termos e a política de privacidade');
    if (!p.pagamento) throw regra('Associe um cartão ou uma conta bancária');
    const meio = construirMeio(p.pagamento);
    const u: UtilizadorInterno = {
      id: e.seq.u++,
      nome: p.nome.trim(),
      email: p.email.trim(),
      nif: p.nif,
      telefone: p.telefone ? p.telefone.replace(/\s/g, '') : null,
      password: p.password,
      criadoEm: agoraIso(),
      meio,
    };
    e.utilizadores.push(u);
    gravar();
    return { status: 201, corpo: sessao(u) };
  }

  // ----- Públicos -----
  if (metodo === 'GET' && caminho === '/viaturas') {
    const lat = params.get('lat');
    const lng = params.get('lng');
    const ref = lat && lng ? { lat: Number(lat), lng: Number(lng) } : undefined;
    const raio = params.get('raio') ? Number(params.get('raio')) : null;
    const disp = params.get('disponiveis') === 'true';
    let lista = e.viaturas.map((v) => viaturaDto(v, ref));
    if (disp) lista = lista.filter((v) => v.disponivel);
    if (ref && raio) lista = lista.filter((v) => (v.distanciaKm ?? 0) <= raio);
    if (ref) lista.sort((a, b2) => (a.distanciaKm ?? 0) - (b2.distanciaKm ?? 0));
    return { status: 200, corpo: lista };
  }
  let mm = m(/^\/viaturas\/(\d+)$/);
  if (metodo === 'GET' && mm) {
    const v = e.viaturas.find((x) => x.id === Number(mm![1]));
    if (!v) throw naoEncontrado(`Viatura ${mm[1]} não existe no Odoo`);
    return { status: 200, corpo: viaturaDto(v) };
  }
  if (metodo === 'GET' && caminho === '/danos/tipos') return { status: 200, corpo: TIPOS_DANO };

  // ----- Com sessão -----
  const u = utilizadorDoToken(token);

  if (caminho === '/me' && metodo === 'GET') return { status: 200, corpo: utilizadorDto(u) };
  if (caminho === '/me' && metodo === 'PUT') {
    const nome = String(b.nome ?? '');
    const email = String(b.email ?? '');
    const nif = String(b.nif ?? '');
    const tel = b.telefone ? String(b.telefone) : null;
    validarDados(nome, email, nif, tel);
    if (e.utilizadores.some((x) => x.id !== u.id && x.email.toLowerCase() === email.trim().toLowerCase()))
      throw regra(`Já existe uma conta com o email ${email.trim()}`);
    Object.assign(u, { nome: nome.trim(), email: email.trim(), nif, telefone: tel ? tel.replace(/\s/g, '') : null });
    gravar();
    return { status: 200, corpo: utilizadorDto(u) };
  }
  if (caminho === '/me/pagamento' && metodo === 'PUT') {
    u.meio = construirMeio(corpo as PedidoMeioPagamento);
    cobrarDividas(u.id);
    gravar();
    return { status: 200, corpo: utilizadorDto(u) };
  }
  if (caminho === '/me/cobrancas' && metodo === 'GET')
    return { status: 200, corpo: e.cobrancas.filter((c) => cobrancaDe(c) === u.id) };
  if (caminho === '/me/cobrancas/pagar' && metodo === 'POST') {
    cobrarDividas(u.id);
    gravar();
    return { status: 200, corpo: e.cobrancas.filter((c) => cobrancaDe(c) === u.id) };
  }

  // ----- Reservas -----
  const minhas = () => e.reservas.filter((r) => r.utilizadorId === u.id);
  if (caminho === '/reservas' && metodo === 'POST') {
    const v = e.viaturas.find((x) => x.id === Number(b.viaturaId));
    if (!v) throw naoEncontrado(`Viatura ${String(b.viaturaId)} não existe no Odoo`);
    if (minhas().some((r) => r.estado === 'RESERVADA' || r.estado === 'EM_CURSO')) throw regra('Já tem uma reserva ativa');
    if (!u.meio) throw regra('Associe um cartão ou uma conta bancária antes de reservar');
    const divida = emDivida(u.id);
    if (divida > 0)
      throw regra(`Tem pagamentos em dívida (${euro(divida)}). Atualize o meio de pagamento para voltar a reservar`);
    const dto = viaturaDto(v);
    if (!dto.disponivel) throw regra(`A viatura ${v.matricula} não está disponível (${dto.motivoIndisponibilidade})`);
    const r: ReservaInterna = {
      id: e.seq.r++,
      utilizadorId: u.id,
      viaturaId: v.id,
      matricula: v.matricula,
      estado: 'RESERVADA',
      criadaEm: agoraIso(),
      inicioViagem: null,
      fimViagem: null,
      taxaEntrada: v.taxa,
      precoMinuto: v.preco,
      totalPenalizacoes: 0,
      estadoPagamento: null,
      pagamentoDescricao: null,
      estadoFatura: null,
      faturaPrevistaEm: null,
      moloniDocumentoId: null,
    };
    e.reservas.push(r);
    gravar();
    return { status: 201, corpo: reservaDto(r) };
  }
  if (caminho === '/reservas' && metodo === 'GET')
    return { status: 200, corpo: minhas().sort((a, b2) => b2.id - a.id).map(reservaDto) };
  if (caminho === '/reservas/ativa' && metodo === 'GET') {
    const r = minhas().find((x) => x.estado === 'RESERVADA' || x.estado === 'EM_CURSO');
    return r ? { status: 200, corpo: reservaDto(r) } : { status: 204 };
  }

  mm = m(/^\/reservas\/(\d+)(?:\/(cancelar|faturar|relatos))?$/);
  if (mm) {
    const r = minhas().find((x) => x.id === Number(mm![1]));
    if (!r) throw naoEncontrado(`Reserva ${mm[1]} não existe`);
    const acao = mm[2];

    if (!acao && metodo === 'GET') return { status: 200, corpo: reservaDto(r) };

    if (acao === 'cancelar' && metodo === 'POST') {
      if (r.estado !== 'RESERVADA') throw regra('Só é possível cancelar reservas que ainda não começaram');
      r.estado = 'CANCELADA';
      r.fimViagem = agoraIso();
      cobrar(r, reservaDto(r).total ?? 0);
      gravar();
      return { status: 200, corpo: reservaDto(r) };
    }

    if (acao === 'faturar' && metodo === 'POST') {
      if (r.estado !== 'CONCLUIDA' && r.estado !== 'CANCELADA')
        throw regra(`Só se faturam viagens concluídas ou canceladas (estado atual: ${r.estado})`);
      if (r.estadoFatura !== 'EMITIDA' && r.estadoPagamento === 'PAGO') {
        r.estadoFatura = 'EMITIDA';
        r.moloniDocumentoId = e.seq.fatura++;
        r.faturaPrevistaEm = null;
        gravar();
      }
      return { status: 200, corpo: reservaDto(r) };
    }

    if (acao === 'relatos' && metodo === 'GET')
      return { status: 200, corpo: e.relatos.filter((x) => x.reservaId === r.id) };

    if (acao === 'relatos' && metodo === 'POST') {
      const p = corpo as PedidoRelato;
      const tipo = p.tipo;
      const esperado = tipo === 'INICIO' ? 'RESERVADA' : 'EM_CURSO';
      if (r.estado !== esperado)
        throw regra(`O relato de ${tipo} exige a reserva no estado ${esperado} (estado atual: ${r.estado})`);
      const v = e.viaturas.find((x) => x.id === r.viaturaId)!;
      if (tipo === 'FIM') {
        if (p.latitude == null || p.longitude == null)
          throw regra('O relato de FIM tem de indicar a latitude e a longitude onde o carro ficou');
        if (p.quilometragem != null && p.quilometragem < v.km)
          throw regra(`A quilometragem final (${p.quilometragem}) não pode ser inferior à atual (${v.km})`);
      }
      if (e.relatos.some((x) => x.reservaId === r.id && x.tipo === tipo)) throw regra(`Esta reserva já tem um relato de ${tipo}`);

      // Condutor anterior (para os danos encontrados no início)
      const anterior = e.reservas
        .filter((x) => x.viaturaId === v.id && x.id !== r.id && x.estado === 'CONCLUIDA')
        .sort((a, b2) => Date.parse(b2.fimViagem ?? '') - Date.parse(a.fimViagem ?? ''))[0];

      let acumulado = 0;
      const danos: DanoRelato[] = (p.danos ?? []).map((d) => {
        const t = TIPOS_DANO.find((x) => x.id === d.tipoDanoId);
        if (!d.tipoDanoId) throw regra('Cada dano tem de indicar o tipoDanoId');
        if (!t) throw regra(`Tipo de dano ${d.tipoDanoId} não existe`);
        const repetido = v.danosAbertos.includes(t.id);
        let penalizacao = 0;
        let nota: string | null = null;
        let responsavel: number | null = null;
        if (repetido) {
          nota = 'Dano já registado e ainda não reparado: não conta outra vez';
        } else if (tipo === 'FIM') {
          penalizacao = Math.max(0, Math.min(t.custo ?? 0, FRANQUIA - acumulado));
          acumulado += penalizacao;
          responsavel = r.id;
          if ((t.custo ?? 0) > penalizacao) nota = `Limitado à franquia de ${euro(FRANQUIA)} por viagem`;
        } else if (anterior) {
          penalizacao = t.custo ?? 0;
          responsavel = anterior.id;
          nota = 'Cobrado ao condutor anterior';
        } else {
          nota = 'Sem condutor anterior: o dano fica só registado';
        }
        if (!repetido) v.danosAbertos.push(t.id);
        if (t.imobilizaCarro && tipo === 'FIM') v.estado = 'reparacao';
        return {
          tipoDanoId: t.id,
          tipoDano: t.nome,
          descricao: d.descricao ?? null,
          penalizacao,
          reservaResponsavelId: responsavel,
          nota,
          odooRegistoId: 1000 + e.seq.rel,
          estadoFatura: null,
          faturaErro: null,
        };
      });

      const relato: Relato = {
        id: e.seq.rel++,
        reservaId: r.id,
        tipo,
        observacoes: p.observacoes ?? null,
        criadoEm: agoraIso(),
        danos,
        totalPenalizacoes: tipo === 'FIM' ? r2(acumulado) : 0,
      };
      e.relatos.push(relato);

      if (tipo === 'INICIO') {
        r.estado = 'EM_CURSO';
        r.inicioViagem = agoraIso();
      } else {
        r.estado = 'CONCLUIDA';
        r.fimViagem = agoraIso();
        r.totalPenalizacoes = r2(acumulado);
        v.lat = p.latitude!;
        v.lng = p.longitude!;
        if (p.quilometragem != null) v.km = Math.round(p.quilometragem);
        cobrar(r, reservaDto(r).total ?? 0);
      }
      gravar();
      return { status: 201, corpo: relato };
    }
  }

  throw naoEncontrado(`Sem rota para ${metodo} ${caminho}`);
}

export async function tratarPedidoDemo(
  metodo: string,
  caminho: string,
  corpo: unknown,
  token: string | null,
): Promise<Resposta> {
  // Pequena espera, para parecer uma rede real
  await new Promise((res) => setTimeout(res, 180 + Math.random() * 220));
  try {
    // Clonar para a UI não mexer no estado interno
    const r = rotear(metodo, caminho, corpo === undefined ? undefined : JSON.parse(JSON.stringify(corpo)), token);
    return { status: r.status, corpo: r.corpo === undefined ? undefined : JSON.parse(JSON.stringify(r.corpo)) };
  } catch (e) {
    if (e instanceof Erro) return { status: e.status, corpo: { title: e.title, detail: e.detail, status: e.status } };
    console.error(e);
    return { status: 500, corpo: { title: 'Erro', detail: 'Erro inesperado no modo demonstração' } };
  }
}

/** Fatura-recibo de exemplo (no backend real, o PDF vem do Moloni). */
export function abrirFaturaDemo(r: Reserva, modelo: string, email?: string) {
  const total = r.total ?? r.valorAteAgora;
  const base = semIva(total);
  const iva = r2(total - base);
  const u = carregar().utilizadores.find((x) => x.id === r.utilizadorId);
  const linhas = [
    [`Taxa de entrada (${modelo} · ${r.matricula})`, r.taxaEntrada],
    [`Utilização: ${r.minutos ?? r.minutosDecorridos} min × ${euro(r.precoMinuto)}`, r2((r.minutos ?? r.minutosDecorridos) * r.precoMinuto)],
    ...(r.totalPenalizacoes ? [['Penalizações por danos', r.totalPenalizacoes] as [string, number]] : []),
  ] as [string, number][];
  const html = `<!doctype html><html lang="pt-PT"><head><meta charset="utf-8"><title>Fatura-recibo FR ${r.moloniDocumentoId}</title>
<style>body{font-family:system-ui,sans-serif;color:#16233f;max-width:720px;margin:40px auto;padding:0 24px}h1{font-size:26px;margin:0}
.top{display:flex;justify-content:space-between;align-items:flex-start;border-bottom:3px solid #16233f;padding-bottom:16px;margin-bottom:24px}
.muted{color:#5b667a;font-size:14px}table{width:100%;border-collapse:collapse;margin-top:16px}td,th{padding:10px 0;border-bottom:1px solid #e3e8ef;text-align:left}
td:last-child,th:last-child{text-align:right}.tot td{font-weight:800;font-size:18px;border:0}.demo{margin-top:28px;padding:12px 14px;background:#fdf1d2;color:#7a5600;border-radius:10px;font-size:14px}
@media print{.demo{display:none}}</style></head><body>
<div class="top"><div><h1>DriveNow</h1><div class="muted">Carros elétricos partilhados · Guimarães</div></div>
<div style="text-align:right"><b>Fatura-recibo FR 2026/${r.moloniDocumentoId}</b><div class="muted">${data(r.fimViagem)} · ${hora(r.fimViagem)}</div></div></div>
<p><b>${u?.nome ?? r.utilizadorNome}</b><br><span class="muted">NIF ${u?.nif ?? '—'} · ${email ?? u?.email ?? ''}</span></p>
<table><thead><tr><th>Descrição</th><th>Valor (IVA incl.)</th></tr></thead><tbody>
${linhas.map(([d, v]) => `<tr><td>${d}</td><td>${euro(v)}</td></tr>`).join('')}
<tr><td class="muted">Valor sem IVA</td><td class="muted">${euro(base)}</td></tr>
<tr><td class="muted">IVA (23%)</td><td class="muted">${euro(iva)}</td></tr>
<tr class="tot"><td>Total pago</td><td>${euro(total)}</td></tr></tbody></table>
<p class="muted">Pago com ${r.pagamentoDescricao ?? '—'}.</p>
<div class="demo">Documento de demonstração, sem validade fiscal. Na app real, a fatura-recibo é emitida pelo Moloni.</div>
</body></html>`;
  const url = URL.createObjectURL(new Blob([html], { type: 'text/html' }));
  window.open(url, '_blank');
  window.setTimeout(() => URL.revokeObjectURL(url), 60000);
}
