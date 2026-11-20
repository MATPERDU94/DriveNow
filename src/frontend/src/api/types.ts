// Tipos iguais aos DTO do backend (pt.drivenow.*). Datas chegam como texto ISO-8601 (Instant).

export type TipoMeioPagamento = 'CARTAO' | 'CONTA_BANCARIA';

export interface MeioPagamento {
  tipo: TipoMeioPagamento;
  titular: string;
  marca: string;
  ultimosDigitos: string;
  validade: string | null;
  descricao: string;
  expirado: boolean;
}

export interface Utilizador {
  id: number;
  nome: string;
  email: string;
  nif: string;
  telefone: string | null;
  criadoEm: string;
  meioPagamento: MeioPagamento | null;
  valorEmDivida: number;
}

export interface Sessao {
  token: string;
  expiraEm: string;
  utilizador: Utilizador;
}

export interface PedidoMeioPagamento {
  tipo: TipoMeioPagamento;
  titular: string;
  numero?: string;
  validade?: string;
  cvv?: string;
  iban?: string;
}

export interface PedidoRegisto {
  nome: string;
  email: string;
  nif: string;
  telefone: string;
  password: string;
  aceitaTermos: boolean;
  pagamento: PedidoMeioPagamento;
}

export interface Viatura {
  id: number;
  matricula: string;
  modelo: string;
  ano: number | null;
  lugares: number | null;
  quilometragem: number | null;
  estado: string | null;
  taxaEntrada: number;
  precoMinuto: number;
  precoKm: number | null;
  latitude: number | null;
  longitude: number | null;
  disponivel: boolean;
  motivoIndisponibilidade: string | null;
  distanciaKm: number | null;
}

export type EstadoReserva = 'RESERVADA' | 'EM_CURSO' | 'CONCLUIDA' | 'CANCELADA';
export type EstadoPagamento = 'PENDENTE' | 'PAGO' | 'EM_DIVIDA';
export type EstadoFatura = 'PENDENTE' | 'RASCUNHO' | 'EMITIDA' | 'ERRO';

export interface Reserva {
  id: number;
  utilizadorId: number;
  utilizadorNome: string;
  viaturaId: number;
  matricula: string;
  estado: EstadoReserva;
  criadaEm: string;
  inicioViagem: string | null;
  fimViagem: string | null;
  taxaEntrada: number;
  precoMinuto: number;
  precoKm: number | null;
  minutos: number | null;
  /** minutos × preço por minuto */
  valorViagem: number | null;
  /** km do carro no momento da reserva */
  kmInicial: number | null;
  /** km do painel indicados no fim da viagem */
  kmFinal: number | null;
  /** km finais - km iniciais (0 se a reserva foi cancelada) */
  kmViagem: number | null;
  /** km da viagem × preço por km */
  valorKm: number | null;
  totalPenalizacoes: number | null;
  total: number | null;
  minutosDecorridos: number;
  valorAteAgora: number;
  estadoPagamento: EstadoPagamento | null;
  pagamentoDescricao: string | null;
  estadoFatura: EstadoFatura | null;
  moloniDocumentoId: number | null;
  faturaPdfUrl: string | null;
  faturaErro: string | null;
}

export interface TipoDano {
  id: number;
  nome: string;
  gravidade: string | null;
  custo: number | null;
  imobilizaCarro: boolean;
  descricao: string | null;
}

export type TipoRelato = 'INICIO' | 'FIM';

export interface PedidoRelato {
  tipo: TipoRelato;
  observacoes?: string;
  danos: { tipoDanoId: number; descricao?: string }[];
  latitude?: number;
  longitude?: number;
  quilometragem?: number;
}

export interface DanoRelato {
  tipoDanoId: number;
  tipoDano: string;
  descricao: string | null;
  penalizacao: number | null;
  reservaResponsavelId: number | null;
  nota: string | null;
  odooRegistoId: number | null;
  estadoFatura: EstadoFatura | null;
  faturaErro: string | null;
}

export interface Relato {
  id: number;
  reservaId: number;
  tipo: TipoRelato;
  observacoes: string | null;
  criadoEm: string;
  danos: DanoRelato[];
  totalPenalizacoes: number | null;
}

export interface Cobranca {
  id: number;
  reservaId: number | null;
  tipo: 'VIAGEM' | 'PENALIZACAO';
  valor: number;
  estado: 'PAGA' | 'EM_DIVIDA';
  meio: string | null;
  motivoRecusa: string | null;
  tentativas: number;
  criadaEm: string;
  pagaEm: string | null;
}
