import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { AlertTriangle, FileText, MapPin, Sparkles, Timer } from 'lucide-react';
import { useSessao } from '../state/sessao';
import { Botao, Campo, Marca, Modal } from '../components/ui';
import { ApiError, MODO_DEMO } from '../api/client';
import { useTitulo } from '../lib/hooks';

const CONTA_DEMO = { email: 'ana.silva@exemplo.pt', password: 'drivenow123' };

export default function Entrar() {
  useTitulo('Iniciar sessão');
  const { entrar } = useSessao();
  const nav = useNavigate();
  const loc = useLocation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [erro, setErro] = useState<string | null>(null);
  const [aEnviar, setAEnviar] = useState(false);
  const [ajuda, setAjuda] = useState(false);

  async function submeter(e: FormEvent) {
    e.preventDefault();
    setErro(null);
    if (!email.trim() || !password) {
      setErro('Indique o email e a password.');
      return;
    }
    setAEnviar(true);
    try {
      await entrar(email, password);
      const destino = (loc.state as { de?: string } | null)?.de;
      nav(destino && destino !== '/entrar' ? destino : '/mapa', { replace: true });
    } catch (e) {
      const err = e as ApiError;
      setErro(err.status === 401 ? 'Email ou password incorretos.' : err.message);
    } finally {
      setAEnviar(false);
    }
  }

  return (
    <div className="auth">
      <section className="auth__lado" aria-hidden="false">
        <Marca escura grande para="/entrar" />
        <div className="auth__mensagem">
          <h1 className="auth__headline">
            O carro mais perto de si, <b>pago ao minuto.</b>
          </h1>
          <p className="auth__texto">
            Encontre um carro no mapa, reserve em segundos e receba a fatura no email assim que terminar a viagem.
          </p>
        </div>
        <ul className="auth__tiles">
          <li>
            <MapPin size={26} />
            <span>Carros próximos</span>
          </li>
          <li>
            <Timer size={26} />
            <span>Pague ao minuto</span>
          </li>
          <li>
            <FileText size={26} />
            <span>Fatura automática</span>
          </li>
        </ul>
      </section>

      <section className="auth__principal">
        <div className="so-movel auth__marca-movel">
          <Marca grande para="/entrar" />
        </div>
        <form className="auth__form" onSubmit={submeter} noValidate>
          <h2 className="auth__titulo">Iniciar sessão</h2>
          <p className="auth__sub">Bem-vindo de volta. Entre para ver os carros perto de si.</p>

          {erro ? (
            <div className="alerta alerta--erro" role="alert" style={{ marginBottom: 18 }}>
              <AlertTriangle size={18} />
              <span>{erro}</span>
            </div>
          ) : null}

          <Campo
            rotulo="Email"
            type="email"
            autoComplete="email"
            placeholder="nome@exemplo.pt"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            autoFocus
          />
          <Campo
            rotulo="Password"
            type="password"
            autoComplete="current-password"
            placeholder="********"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            extra={
              <button type="button" className="link-btn" onClick={() => setAjuda(true)}>
                Esqueceu-se?
              </button>
            }
          />

          <Botao type="submit" className="btn--bloco btn--lg auth__entrar" carregando={aEnviar}>
            Entrar
          </Botao>

          <p className="auth__rodape">
            Ainda não tem conta? <Link to="/criar-conta">Criar conta</Link>
          </p>

          {MODO_DEMO ? (
            <div className="alerta alerta--info auth__demo">
              <Sparkles size={18} />
              <span>
                <strong>Modo demonstração.</strong> Conta de exemplo: {CONTA_DEMO.email} / {CONTA_DEMO.password}.{' '}
                <button
                  type="button"
                  className="link-btn"
                  onClick={() => {
                    setEmail(CONTA_DEMO.email);
                    setPassword(CONTA_DEMO.password);
                  }}
                >
                  Preencher
                </button>
              </span>
            </div>
          ) : null}
        </form>
      </section>

      {ajuda ? (
        <Modal
          titulo="Recuperar a password"
          aoFechar={() => setAjuda(false)}
          rodape={
            <Botao onClick={() => setAjuda(false)} className="btn--md">
              Percebi
            </Botao>
          }
        >
          <p style={{ color: 'var(--muted)' }}>
            A recuperação automática da password ainda não está disponível. Contacte o apoio ao cliente da DriveNow
            com o email da sua conta e enviamos-lhe uma nova password.
          </p>
        </Modal>
      ) : null}
    </div>
  );
}
