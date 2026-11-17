import { createContext, useCallback, useContext, useState, type ReactNode } from 'react';
import { AlertTriangle, CheckCircle2, Info } from 'lucide-react';

type Tipo = 'sucesso' | 'erro' | 'info';
interface Toast {
  id: number;
  tipo: Tipo;
  texto: string;
}

const Ctx = createContext<(texto: string, tipo?: Tipo) => void>(() => {});

let seq = 1;

export function ToastProvider({ children }: { children: ReactNode }) {
  const [lista, setLista] = useState<Toast[]>([]);

  const mostrar = useCallback((texto: string, tipo: Tipo = 'sucesso') => {
    const id = seq++;
    setLista((l) => [...l.slice(-2), { id, tipo, texto }]);
    window.setTimeout(() => setLista((l) => l.filter((t) => t.id !== id)), tipo === 'erro' ? 6000 : 3500);
  }, []);

  return (
    <Ctx.Provider value={mostrar}>
      {children}
      <div className="toasts" role="status" aria-live="polite">
        {lista.map((t) => (
          <div key={t.id} className={`toast toast--${t.tipo}`}>
            {t.tipo === 'sucesso' ? <CheckCircle2 size={18} /> : t.tipo === 'erro' ? <AlertTriangle size={18} /> : <Info size={18} />}
            <span>{t.texto}</span>
          </div>
        ))}
      </div>
    </Ctx.Provider>
  );
}

export const useToast = () => useContext(Ctx);
