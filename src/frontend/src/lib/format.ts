import type { Reserva } from '../api/types';

const euroFmt = new Intl.NumberFormat('pt-PT', { style: 'currency', currency: 'EUR' });
const num2 = new Intl.NumberFormat('pt-PT', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const num1 = new Intl.NumberFormat('pt-PT', { minimumFractionDigits: 1, maximumFractionDigits: 1 });
const inteiro = new Intl.NumberFormat('pt-PT', { maximumFractionDigits: 0 });

export const MESES = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez'];
export const MESES_LONGOS = [
  'janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho',
  'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro',
];

/** 6,67 € */
export const euro = (v: number | null | undefined) => euroFmt.format(v ?? 0);

/** 0,32 €/min */
export const porMinuto = (v: number | null | undefined) => `${num2.format(v ?? 0)} €/min`;

/** 320 m · 1,2 km */
export function distancia(km: number | null | undefined): string {
  if (km == null) return '—';
  if (km < 1) return `${Math.max(10, Math.round((km * 1000) / 10) * 10)} m`;
  return `${num1.format(km)} km`;
}

/** Minutos a pé (≈ 80 m por minuto). */
export const minutosAPe = (km: number) => Math.max(1, Math.round((km * 1000) / 80));

export const km = (v: number | null | undefined) => (v == null ? '—' : `${inteiro.format(v)} km`);

/** 4 nov 2026 */
export function data(iso: string | null | undefined): string {
  if (!iso) return '—';
  const d = new Date(iso);
  return `${d.getDate()} ${MESES[d.getMonth()]} ${d.getFullYear()}`;
}

/** 10:12 */
export function hora(iso: string | null | undefined): string {
  if (!iso) return '—';
  const d = new Date(iso);
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

/** 23 min · 1 h 05 min */
export function duracao(minutos: number | null | undefined): string {
  const m = Math.max(0, Math.round(minutos ?? 0));
  if (m < 60) return `${m} min`;
  const h = Math.floor(m / 60);
  const r = m % 60;
  return r ? `${h} h ${String(r).padStart(2, '0')} min` : `${h} h`;
}

/** 25:51 · 1:02:07 */
export function cronometro(segundos: number): string {
  const s = Math.max(0, Math.floor(segundos));
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const ss = String(s % 60).padStart(2, '0');
  return h > 0 ? `${h}:${String(m).padStart(2, '0')}:${ss}` : `${String(m).padStart(2, '0')}:${ss}`;
}

/** "Tesla/Model 3" (nome do Odoo) -> "Tesla Model 3" */
export const nomeModelo = (m: string | null | undefined) => (m ? m.replace(/\s*\/\s*/g, ' ').trim() : 'Carro');

/** Ana Silva -> AS */
export function iniciais(nome: string | null | undefined): string {
  if (!nome) return '?';
  const p = nome.trim().split(/\s+/);
  return ((p[0]?.[0] ?? '') + (p.length > 1 ? p[p.length - 1][0] : '')).toUpperCase();
}

export const primeiroNome = (nome: string | null | undefined) => (nome ?? '').trim().split(/\s+/)[0] ?? '';

/** Cada minuto começado conta como inteiro (igual ao backend), no mínimo 1. */
export const minutosCobrados = (segundos: number) => Math.max(1, Math.ceil(segundos / 60));

/** Valor sem IVA a partir de um preço com IVA a 23%. */
export const semIva = (total: number) => Math.round((total / 1.23) * 100) / 100;

/** Total da viagem (taxa + minutos + penalizações); enquanto decorre, o valor até agora. */
export const totalDe = (r: Reserva) => r.total ?? r.valorAteAgora ?? 0;
