import { useEffect, useRef, useState } from 'react';

/** Segundos desde `inicio` (ISO), atualizado a cada segundo. Para em `fim`, se existir. */
export function useSegundosDesde(inicio: string | null | undefined, fim?: string | null): number {
  const calc = () => {
    if (!inicio) return 0;
    const ate = fim ? Date.parse(fim) : Date.now();
    return Math.max(0, (ate - Date.parse(inicio)) / 1000);
  };
  const [s, setS] = useState(calc);
  useEffect(() => {
    setS(calc());
    if (!inicio || fim) return;
    const id = window.setInterval(() => setS(calc()), 1000);
    return () => window.clearInterval(id);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [inicio, fim]);
  return s;
}

/** Repete `fn` a cada `ms` enquanto `ativo` for true (e a página estiver visível). */
export function useIntervalo(fn: () => void, ms: number, ativo = true) {
  const ref = useRef(fn);
  ref.current = fn;
  useEffect(() => {
    if (!ativo) return;
    const id = window.setInterval(() => {
      if (document.visibilityState === 'visible') ref.current();
    }, ms);
    return () => window.clearInterval(id);
  }, [ms, ativo]);
}

/** Título do separador do browser. */
export function useTitulo(titulo: string) {
  useEffect(() => {
    document.title = titulo ? `${titulo} | DriveNow` : 'DriveNow';
  }, [titulo]);
}
