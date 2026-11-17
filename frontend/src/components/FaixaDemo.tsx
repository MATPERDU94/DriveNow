import { useState } from 'react';
import { RotateCcw, Sparkles } from 'lucide-react';

/** Etiqueta discreta do modo demonstração, com opção de repor os dados de exemplo. */
export function FaixaDemo() {
  const [aberta, setAberta] = useState(false);
  return (
    <div className={`faixa-demo${aberta ? ' faixa-demo--aberta' : ''}`}>
      <button className="faixa-demo__btn" onClick={() => setAberta((a) => !a)} aria-expanded={aberta}>
        <Sparkles size={15} /> Demo
      </button>
      {aberta ? (
        <div className="faixa-demo__painel">
          <p>
            <b>Modo demonstração.</b> A API é simulada no browser, com as mesmas regras do backend. Os dados ficam só
            neste browser.
          </p>
          <button
            className="btn btn--cinza btn--sm"
            onClick={async () => {
              const { reporDemo } = await import('../demo/mockApi');
              reporDemo();
              if (window.location.hash.startsWith('#/')) window.location.hash = '#/entrar';
              window.location.reload();
            }}
          >
            <RotateCcw size={15} /> Repor dados de exemplo
          </button>
        </div>
      ) : null}
    </div>
  );
}
