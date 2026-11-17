import { useState, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AlertTriangle } from 'lucide-react';
import { useSessao } from '../state/sessao';
import { useToast } from '../state/toast';
import { Botao, Campo, Marca } from '../components/ui';
import { CamposPagamento } from '../components/CamposPagamento';
import { ApiError } from '../api/client';
import {
  PAGAMENTO_VAZIO,
  emailValido,
  errosPagamento,
  mascaraTelefone,
  nifValido,
  pedidoPagamento,
  soDigitos,
  telefoneValido,
  type DadosPagamento,
} from '../lib/validacao';
import { primeiroNome } from '../lib/format';
import { useTitulo } from '../lib/hooks';

interface Dados {
  nome: string;
  nif: string;
  telefone: string;
  email: string;
  password: string;
}

type ErrosDados = Partial<Record<keyof Dados, string>>;

function validarDados(d: Dados): ErrosDados {
  const e: ErrosDados = {};
  if (!d.nome.trim()) e.nome = 'Indique o nome completo';
  if (!nifValido(d.nif)) e.nif = 'NIF inválido (9 dígitos)';
  if (!telefoneValido(d.telefone)) e.telefone = 'Telemóvel inválido';
  if (!emailValido(d.email)) e.email = 'Email inválido';
  if (d.password.length < 8) e.password = 'A password tem de ter pelo menos 8 caracteres';
  return e;
}

export default function CriarConta() {
  useTitulo('Criar conta');
  const { registar } = useSessao();
  const toast = useToast();
  const nav = useNavigate();
  const [passo, setPasso] = useState<1 | 2>(1);
  const [dados, setDados] = useState<Dados>({ nome: '', nif: '', telefone: '', email: '', password: '' });
  const [pag, setPag] = useState<DadosPagamento>(PAGAMENTO_VAZIO);
  const [termos, setTermos] = useState(false);
  const [tentou, setTentou] = useState(false);
  const [erroApi, setErroApi] = useState<string | null>(null);
  const [aEnviar, setAEnviar] = useState(false);

  const errosD = tentou ? validarDados(dados) : {};
  const errosP = tentou ? errosPagamento(pag) : {};
  const setD = <K extends keyof Dados>(k: K, v: string) => setDados((d) => ({ ...d, [k]: v }));

  async function submeter(e: FormEvent) {
    e.preventDefault();
    setTentou(true);
    setErroApi(null);
    const ed = validarDados(dados);
    const ep = errosPagamento(pag);
    if (Object.keys(ed).length || Object.keys(ep).length) {
      setPasso(Object.keys(ed).length ? 1 : 2);
      setErroApi('Corrija os campos assinalados.');
      return;
    }
    if (!termos) {
      setErroApi('É preciso aceitar os termos e a política de privacidade.');
      return;
    }
    setAEnviar(true);
    try {
      await registar({
        nome: dados.nome.trim(),
        email: dados.email.trim(),
        nif: dados.nif,
        telefone: dados.telefone.replace(/\s/g, ''),
        password: dados.password,
        aceitaTermos: termos,
        pagamento: pedidoPagamento(pag),
      });
      toast(`Conta criada. Bem-vindo, ${primeiroNome(dados.nome)}!`);
      nav('/mapa', { replace: true });
    } catch (e) {
      setErroApi((e as ApiError).message);
    } finally {
      setAEnviar(false);
    }
  }

  return (
    <div className="registo">
      <header className="topo topo--simples">
        <Marca para="/entrar" />
        <p className="topo__aux">
          Já tem conta? <Link to="/entrar">Entrar</Link>
        </p>
      </header>

      <form className="pagina registo__pagina" onSubmit={submeter} noValidate>
        <h1 className="titulo">Criar conta</h1>
        <p className="subtitulo">Dois passos e fica pronto a conduzir. O NIF é usado para emitir as suas faturas.</p>

        <ol className="passos" aria-label="Passos">
          <li className={passo === 1 ? 'passo passo--ativo' : 'passo'}>
            <span className="passo__n">1</span>Dados pessoais
          </li>
          <li className={passo === 2 ? 'passo passo--ativo' : 'passo'}>
            <span className="passo__n">2</span>Meio de pagamento
          </li>
        </ol>

        <div className="registo__grelha">
          <fieldset className="cartao cartao--legenda" onFocus={() => setPasso(1)}>
            <legend>1. Dados pessoais</legend>
            <Campo
              rotulo="Nome completo"
              placeholder="Ana Silva"
              autoComplete="name"
              value={dados.nome}
              onChange={(e) => setD('nome', e.target.value)}
              erro={errosD.nome}
            />
            <div className="linha">
              <Campo
                rotulo="NIF"
                placeholder="123456789"
                inputMode="numeric"
                value={dados.nif}
                onChange={(e) => setD('nif', soDigitos(e.target.value).slice(0, 9))}
                erro={errosD.nif}
              />
              <Campo
                rotulo="Telemóvel"
                placeholder="912 345 678"
                inputMode="tel"
                autoComplete="tel"
                value={dados.telefone}
                onChange={(e) => setD('telefone', mascaraTelefone(e.target.value))}
                erro={errosD.telefone}
              />
            </div>
            <Campo
              rotulo="Email"
              type="email"
              placeholder="nome@exemplo.pt"
              autoComplete="email"
              value={dados.email}
              onChange={(e) => setD('email', e.target.value)}
              erro={errosD.email}
              ajuda="As faturas são enviadas para este email."
            />
            <Campo
              rotulo="Password"
              type="password"
              placeholder="Mínimo 8 caracteres"
              autoComplete="new-password"
              value={dados.password}
              onChange={(e) => setD('password', e.target.value)}
              erro={errosD.password}
            />
          </fieldset>

          <fieldset className="cartao cartao--legenda" onFocus={() => setPasso(2)}>
            <legend>2. Meio de pagamento</legend>
            <CamposPagamento valor={pag} aoMudar={setPag} erros={errosP} />
          </fieldset>
        </div>

        {erroApi ? (
          <div className="alerta alerta--erro registo__erro" role="alert">
            <AlertTriangle size={18} />
            <span>{erroApi}</span>
          </div>
        ) : null}

        <div className="registo__fim">
          <label className="caixa">
            <input type="checkbox" checked={termos} onChange={(e) => setTermos(e.target.checked)} />
            Aceito os termos e a política de privacidade
          </label>
          <Botao type="submit" carregando={aEnviar} className="btn--lg registo__botao">
            Criar conta
          </Botao>
        </div>
      </form>
    </div>
  );
}
