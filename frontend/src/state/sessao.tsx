import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { api, aoExpirarSessao, guardarToken, lerToken } from '../api/client';
import type { PedidoRegisto, Reserva, Utilizador } from '../api/types';

interface ValorSessao {
  /** null = sem sessão; undefined = ainda a carregar */
  utilizador: Utilizador | null | undefined;
  ativa: Reserva | null;
  entrar: (email: string, password: string) => Promise<void>;
  registar: (p: PedidoRegisto) => Promise<void>;
  sair: () => void;
  atualizarUtilizador: (u?: Utilizador) => Promise<void>;
  atualizarAtiva: () => Promise<Reserva | null>;
  definirAtiva: (r: Reserva | null) => void;
}

const Ctx = createContext<ValorSessao | null>(null);

export function SessaoProvider({ children }: { children: ReactNode }) {
  const [utilizador, setUtilizador] = useState<Utilizador | null | undefined>(lerToken() ? undefined : null);
  const [ativa, setAtiva] = useState<Reserva | null>(null);
  const montado = useRef(true);

  const sair = useCallback(() => {
    guardarToken(null);
    setUtilizador(null);
    setAtiva(null);
  }, []);

  const atualizarAtiva = useCallback(async () => {
    if (!lerToken()) return null;
    try {
      const r = await api.reservaAtiva();
      if (montado.current) setAtiva(r ?? null);
      return r ?? null;
    } catch {
      return null;
    }
  }, []);

  const atualizarUtilizador = useCallback(async (u?: Utilizador) => {
    if (u) {
      setUtilizador(u);
      return;
    }
    if (!lerToken()) {
      setUtilizador(null);
      return;
    }
    try {
      const eu = await api.eu();
      if (montado.current) setUtilizador(eu);
    } catch (e) {
      if ((e as { status?: number }).status === 401) sair();
      else if (montado.current) setUtilizador((atual) => atual ?? null);
    }
  }, [sair]);

  useEffect(() => {
    montado.current = true;
    const off = aoExpirarSessao(sair);
    if (lerToken()) {
      void atualizarUtilizador();
      void atualizarAtiva();
    }
    return () => {
      montado.current = false;
      off();
    };
  }, [sair, atualizarUtilizador, atualizarAtiva]);

  // A reserva ativa é verificada periodicamente (por exemplo, se for cancelada noutro dispositivo)
  useEffect(() => {
    if (!utilizador) return;
    const id = window.setInterval(() => void atualizarAtiva(), 20000);
    return () => window.clearInterval(id);
  }, [utilizador, atualizarAtiva]);

  const entrar = useCallback(async (email: string, password: string) => {
    const s = await api.login(email.trim(), password);
    guardarToken(s.token);
    setUtilizador(s.utilizador);
    await atualizarAtiva();
  }, [atualizarAtiva]);

  const registar = useCallback(async (p: PedidoRegisto) => {
    const s = await api.registar(p);
    guardarToken(s.token);
    setUtilizador(s.utilizador);
    setAtiva(null);
  }, []);

  const valor = useMemo<ValorSessao>(
    () => ({
      utilizador,
      ativa,
      entrar,
      registar,
      sair,
      atualizarUtilizador,
      atualizarAtiva,
      definirAtiva: setAtiva,
    }),
    [utilizador, ativa, entrar, registar, sair, atualizarUtilizador, atualizarAtiva],
  );

  return <Ctx.Provider value={valor}>{children}</Ctx.Provider>;
}

export function useSessao() {
  const v = useContext(Ctx);
  if (!v) throw new Error('useSessao fora do SessaoProvider');
  return v;
}
