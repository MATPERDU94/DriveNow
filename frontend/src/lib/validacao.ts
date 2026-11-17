// Mesmas regras do backend (AuthService.validarDados, Nif, ValidacaoPagamento), para avisar antes de enviar.

export const soDigitos = (s: string) => s.replace(/\D/g, '');

export function nifValido(nif: string): boolean {
  if (!/^\d{9}$/.test(nif)) return false;
  let soma = 0;
  for (let i = 0; i < 8; i++) soma += Number(nif[i]) * (9 - i);
  const resto = soma % 11;
  const controlo = resto < 2 ? 0 : 11 - resto;
  return controlo === Number(nif[8]);
}

export const emailValido = (e: string) => /^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(e.trim());

export function telefoneValido(t: string): boolean {
  const limpo = t.replace(/\s/g, '');
  return limpo === '' || /^\+?\d{9,15}$/.test(limpo);
}

export function luhn(numero: string): boolean {
  if (!/^\d{13,19}$/.test(numero)) return false;
  let soma = 0;
  let duplicar = false;
  for (let i = numero.length - 1; i >= 0; i--) {
    let d = Number(numero[i]);
    if (duplicar) {
      d *= 2;
      if (d > 9) d -= 9;
    }
    soma += d;
    duplicar = !duplicar;
  }
  return soma % 10 === 0;
}

export function validadeOk(v: string): boolean {
  const m = /^(0[1-9]|1[0-2])\/(\d{2})$/.exec(v);
  if (!m) return false;
  const fim = new Date(2000 + Number(m[2]), Number(m[1]), 0, 23, 59, 59);
  return fim.getTime() >= Date.now();
}

export function ibanOk(ibanBruto: string): boolean {
  const iban = ibanBruto.replace(/[\s-]/g, '').toUpperCase();
  if (!/^PT\d{23}$/.test(iban)) return false;
  const r = iban.slice(4) + iban.slice(0, 4);
  let resto = 0;
  for (const c of r) {
    const n = /[A-Z]/.test(c) ? String(c.charCodeAt(0) - 55) : c;
    for (const d of n) resto = (resto * 10 + Number(d)) % 97;
  }
  return resto === 1;
}

export function marcaCartao(numero: string): string {
  const n = soDigitos(numero);
  if (n.startsWith('4')) return 'Visa';
  if (/^5[1-5]/.test(n) || /^2[2-7]/.test(n)) return 'Mastercard';
  if (/^3[47]/.test(n)) return 'American Express';
  return 'Cartão';
}

// ----- Máscaras de escrita -----

/** 4242 4242 4242 4242 */
export const mascaraCartao = (v: string) =>
  soDigitos(v)
    .slice(0, 19)
    .replace(/(\d{4})(?=\d)/g, '$1 ');

/** 08/28 */
export function mascaraValidade(v: string, anterior: string): string {
  const d = soDigitos(v).slice(0, 4);
  if (d.length === 0) return '';
  // a apagar a barra: não a voltar a pôr
  if (anterior.endsWith('/') && v.length < anterior.length) return d.slice(0, 1);
  if (d.length === 1 && Number(d) > 1) return `0${d}/`;
  if (d.length < 2) return d;
  return d.length === 2 ? `${d}/` : `${d.slice(0, 2)}/${d.slice(2)}`;
}

/** PT50 0002 0123 1234 5678 9015 4 */
export function mascaraIban(v: string): string {
  const limpo = v.replace(/[^0-9a-zA-Z]/g, '').toUpperCase().slice(0, 25);
  return limpo.replace(/(.{4})(?=.)/g, '$1 ');
}

/** 912 345 678 */
export function mascaraTelefone(v: string): string {
  const mais = v.trim().startsWith('+');
  const d = soDigitos(v).slice(0, 15);
  if (mais) return `+${d}`;
  return d.replace(/^(\d{3})(\d{1,3})?(\d{1,3})?(\d*)$/, (_, a, b, c, r) => [a, b, c, r].filter(Boolean).join(' '));
}

export interface DadosPagamento {
  tipo: 'CARTAO' | 'CONTA_BANCARIA';
  titular: string;
  numero: string;
  validade: string;
  cvv: string;
  iban: string;
}

export const PAGAMENTO_VAZIO: DadosPagamento = {
  tipo: 'CARTAO',
  titular: '',
  numero: '',
  validade: '',
  cvv: '',
  iban: '',
};

export function errosPagamento(p: DadosPagamento): Partial<Record<keyof DadosPagamento, string>> {
  const e: Partial<Record<keyof DadosPagamento, string>> = {};
  if (!p.titular.trim()) e.titular = p.tipo === 'CARTAO' ? 'Indique o titular do cartão' : 'Indique o titular da conta';
  if (p.tipo === 'CARTAO') {
    if (!luhn(soDigitos(p.numero))) e.numero = 'Número de cartão inválido';
    if (!validadeOk(p.validade)) e.validade = 'Validade inválida ou expirada (MM/AA)';
    if (!/^\d{3,4}$/.test(p.cvv)) e.cvv = 'CVV inválido';
  } else if (!ibanOk(p.iban)) {
    e.iban = 'IBAN inválido (PT50 + 21 dígitos)';
  }
  return e;
}

export function pedidoPagamento(p: DadosPagamento) {
  return p.tipo === 'CARTAO'
    ? {
        tipo: 'CARTAO' as const,
        titular: p.titular.trim(),
        numero: soDigitos(p.numero),
        validade: p.validade,
        cvv: p.cvv,
      }
    : { tipo: 'CONTA_BANCARIA' as const, titular: p.titular.trim(), iban: p.iban.replace(/\s/g, '') };
}
