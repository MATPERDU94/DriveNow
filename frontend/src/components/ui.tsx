import { useEffect, useId, type ButtonHTMLAttributes, type InputHTMLAttributes, type ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { X } from 'lucide-react';
import logo from '../assets/logo.png';

export function Marca({ escura, grande, para = '/' }: { escura?: boolean; grande?: boolean; para?: string }) {
  return (
    <Link to={para} className={`marca${escura ? ' marca--escura' : ''}${grande ? ' marca--grande' : ''}`} aria-label="DriveNow">
      <img className="marca__tile" src={logo} alt="" width={46} height={46} />
      <span className="marca__nome">
        Drive<b>Now</b>
      </span>
    </Link>
  );
}

export function Spinner({ grande }: { grande?: boolean }) {
  return <span className={`spinner${grande ? ' spinner--lg' : ''}`} aria-hidden />;
}

export function Carregar() {
  return (
    <div className="carregar" aria-busy>
      <Spinner grande />
    </div>
  );
}

type CampoProps = InputHTMLAttributes<HTMLInputElement> & {
  rotulo: string;
  erro?: string | null;
  ajuda?: ReactNode;
  extra?: ReactNode;
  icone?: ReactNode;
};

export function Campo({ rotulo, erro, ajuda, extra, icone, id, className, ...resto }: CampoProps) {
  const auto = useId();
  const cid = id ?? auto;
  const input = (
    <input
      id={cid}
      className={`input${className ? ` ${className}` : ''}`}
      aria-invalid={erro ? true : undefined}
      aria-describedby={erro ? `${cid}-erro` : undefined}
      {...resto}
    />
  );
  return (
    <div className="campo">
      <label className="campo__rot" htmlFor={cid}>
        <span>{rotulo}</span>
        {extra}
      </label>
      {icone ? (
        <div className="input-icone">
          {icone}
          {input}
        </div>
      ) : (
        input
      )}
      {erro ? (
        <span className="campo__erro" id={`${cid}-erro`}>
          {erro}
        </span>
      ) : ajuda ? (
        <span className="campo__ajuda">{ajuda}</span>
      ) : null}
    </div>
  );
}

export function Botao({
  carregando,
  children,
  variante = 'navy',
  className,
  ...resto
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  carregando?: boolean;
  variante?: 'navy' | 'sky' | 'outline' | 'claro' | 'cinza' | 'perigo' | 'navy-claro';
}) {
  return (
    <button
      className={`btn btn--${variante}${className ? ` ${className}` : ''}`}
      disabled={carregando || resto.disabled}
      aria-busy={carregando || undefined}
      {...resto}
    >
      {carregando ? <Spinner /> : null}
      {children}
    </button>
  );
}

export function Modal({
  titulo,
  subtitulo,
  aoFechar,
  children,
  rodape,
  largo,
}: {
  titulo: ReactNode;
  subtitulo?: ReactNode;
  aoFechar: () => void;
  children: ReactNode;
  rodape?: ReactNode;
  largo?: boolean;
}) {
  const tid = useId();
  useEffect(() => {
    const tecla = (e: KeyboardEvent) => e.key === 'Escape' && aoFechar();
    document.addEventListener('keydown', tecla);
    const overflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.removeEventListener('keydown', tecla);
      document.body.style.overflow = overflow;
    };
  }, [aoFechar]);

  return (
    <div className="modal-fundo" onMouseDown={(e) => e.target === e.currentTarget && aoFechar()}>
      <div className={`modal${largo ? ' modal--largo' : ''}`} role="dialog" aria-modal="true" aria-labelledby={tid}>
        <div className="modal__cab">
          <div>
            <h2 className="modal__titulo" id={tid}>
              {titulo}
            </h2>
            {subtitulo ? <p className="modal__sub">{subtitulo}</p> : null}
          </div>
          <button className="modal__fechar" onClick={aoFechar} aria-label="Fechar">
            <X size={20} />
          </button>
        </div>
        <div className="modal__corpo">{children}</div>
        {rodape ? <div className="modal__rodape">{rodape}</div> : null}
      </div>
    </div>
  );
}
