import { useEffect, useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertTriangle, CreditCard, Landmark, LogOut, Plus } from 'lucide-react';
import { api, type ApiError } from '../api/client';
import type { MeioPagamento } from '../api/types';
import { useSessao } from '../state/sessao';
import { useToast } from '../state/toast';
import { Botao, Campo, Carregar, Modal } from '../components/ui';
import { CamposPagamento } from '../components/CamposPagamento';
import { MESES_LONGOS, euro, iniciais } from '../lib/format';
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
import { useTitulo } from '../lib/hooks';

function CartaoVisual({ m }: { m: MeioPagamento }) {
  const conta = m.tipo === 'CONTA_BANCARIA';
  return (
    <div className={`cartao-pag${m.expirado ? ' cartao-pag--expirado' : ''}`}>
      <div className="cartao-pag__topo">
        <span>{conta ? 'Conta bancária' : 'Cartão principal'}</span>
        <b className="cartao-pag__marca">{conta ? <Landmark size={22} /> : m.marca.toUpperCase()}</b>
      </div>
      <div className="cartao-pag__num tabular">
        {conta ? (
          <>PT50 **** **** **** {m.ultimosDigitos}</>
        ) : (
          <>
            <span>****</span> <span>****</span> <span>****</span> {m.ultimosDigitos}
          </>
        )}
      </div>
      <div className="cartao-pag__base">
        <span>{m.titular.toUpperCase()}</span>
        {m.validade ? <span>Validade {m.validade}</span> : <span>Débito direto</span>}
      </div>
    </div>
  );
}

export default function Perfil() {
  useTitulo('Perfil');
  const { utilizador, atualizarUtilizador, sair } = useSessao();
  const toast = useToast();
  const nav = useNavigate();
  const [dados, setDados] = useState({ nome: '', email: '', nif: '', telefone: '' });
  const [tentou, setTentou] = useState(false);
  const [aGuardar, setAGuardar] = useState(false);
  const [erroDados, setErroDados] = useState<string | null>(null);
  const [modalPag, setModalPag] = useState(false);
  const [pag, setPag] = useState<DadosPagamento>(PAGAMENTO_VAZIO);
  const [tentouPag, setTentouPag] = useState(false);
  const [aGuardarPag, setAGuardarPag] = useState(false);
  const [erroPag, setErroPag] = useState<string | null>(null);
  const [aCobrar, setACobrar] = useState(false);

  useEffect(() => {
    void atualizarUtilizador();
  }, [atualizarUtilizador]);

  useEffect(() => {
    if (utilizador)
      setDados({
        nome: utilizador.nome,
        email: utilizador.email,
        nif: utilizador.nif,
        telefone: utilizador.telefone ? mascaraTelefone(utilizador.telefone) : '',
      });
  }, [utilizador?.id, utilizador?.nome, utilizador?.email, utilizador?.nif, utilizador?.telefone]); // eslint-disable-line react-hooks/exhaustive-deps

  if (!utilizador) return <Carregar />;

  const erros = tentou
    ? {
        nome: dados.nome.trim() ? undefined : 'Indique o nome completo',
        email: emailValido(dados.email) ? undefined : 'Email inválido',
        nif: nifValido(dados.nif) ? undefined : 'NIF inválido (9 dígitos)',
        telefone: telefoneValido(dados.telefone) ? undefined : 'Telemóvel inválido',
      }
    : {};
  const alterado =
    dados.nome !== utilizador.nome ||
    dados.email !== utilizador.email ||
    dados.nif !== utilizador.nif ||
    dados.telefone.replace(/\s/g, '') !== (utilizador.telefone ?? '').replace(/\s/g, '');

  const desde = new Date(utilizador.criadoEm);
  const divida = utilizador.valorEmDivida ?? 0;
  const meio = utilizador.meioPagamento;

  async function guardar(e: FormEvent) {
    e.preventDefault();
    setTentou(true);
    setErroDados(null);
    if (!dados.nome.trim() || !emailValido(dados.email) || !nifValido(dados.nif) || !telefoneValido(dados.telefone)) return;
    setAGuardar(true);
    try {
      const u = await api.atualizarDados({
        nome: dados.nome.trim(),
        email: dados.email.trim(),
        nif: dados.nif,
        telefone: dados.telefone.replace(/\s/g, ''),
      });
      await atualizarUtilizador(u);
      setTentou(false);
      toast('Dados guardados.');
    } catch (e) {
      setErroDados((e as ApiError).message);
    } finally {
      setAGuardar(false);
    }
  }

  async function guardarPagamento() {
    setTentouPag(true);
    setErroPag(null);
    if (Object.keys(errosPagamento(pag)).length) return;
    setAGuardarPag(true);
    const tinhaDivida = divida > 0;
    try {
      const u = await api.alterarPagamento(pedidoPagamento(pag));
      await atualizarUtilizador(u);
      setModalPag(false);
      if (tinhaDivida && u.valorEmDivida > 0) toast(`Meio guardado, mas a cobrança da dívida foi recusada (${euro(u.valorEmDivida)}).`, 'erro');
      else if (tinhaDivida) toast('Meio de pagamento atualizado e dívida paga.');
      else toast('Meio de pagamento atualizado.');
    } catch (e) {
      setErroPag((e as ApiError).message);
    } finally {
      setAGuardarPag(false);
    }
  }

  async function cobrarDivida() {
    setACobrar(true);
    try {
      await api.pagarDividas();
      const u = await api.eu();
      await atualizarUtilizador(u);
      toast(u.valorEmDivida > 0 ? 'O pagamento voltou a ser recusado. Altere o meio de pagamento.' : 'Dívida paga. Já pode reservar.', u.valorEmDivida > 0 ? 'erro' : 'sucesso');
    } catch (e) {
      toast((e as ApiError).message, 'erro');
    } finally {
      setACobrar(false);
    }
  }

  function abrirModalPag() {
    setPag({ ...PAGAMENTO_VAZIO, titular: meio?.tipo === 'CARTAO' ? utilizador!.nome.toUpperCase() : utilizador!.nome });
    setTentouPag(false);
    setErroPag(null);
    setModalPag(true);
  }

  return (
    <main className="pagina pagina--media perfil">
      <div className="perfil__cab">
        <span className="avatar avatar--xl">{iniciais(utilizador.nome)}</span>
        <div>
          <h1 className="titulo perfil__nome">{utilizador.nome}</h1>
          <p className="subtitulo perfil__desde">
            Cliente desde {MESES_LONGOS[desde.getMonth()]} de {desde.getFullYear()}
          </p>
        </div>
      </div>

      {divida > 0 ? (
        <div className="alerta alerta--erro perfil__divida">
          <AlertTriangle size={20} />
          <div>
            <strong>Tem {euro(divida)} em dívida.</strong> Enquanto não for pago, não pode fazer novas reservas. Altere o meio
            de pagamento (a dívida é cobrada logo a seguir) ou tente cobrar outra vez.
            <div className="perfil__divida-acoes">
              <Botao variante="navy" className="btn--sm" onClick={abrirModalPag}>
                Alterar meio de pagamento
              </Botao>
              <Botao variante="claro" className="btn--sm" onClick={cobrarDivida} carregando={aCobrar}>
                Tentar cobrar outra vez
              </Botao>
            </div>
          </div>
        </div>
      ) : null}

      <div className="perfil__grelha">
        <form className="cartao cartao--pad" onSubmit={guardar} noValidate>
          <h2 className="cartao__titulo">Dados pessoais</h2>
          <Campo
            rotulo="Nome"
            value={dados.nome}
            autoComplete="name"
            onChange={(e) => setDados({ ...dados, nome: e.target.value })}
            erro={erros.nome}
          />
          <Campo
            rotulo="Email (recebe as faturas)"
            type="email"
            autoComplete="email"
            value={dados.email}
            onChange={(e) => setDados({ ...dados, email: e.target.value })}
            erro={erros.email}
          />
          <div className="linha">
            <Campo
              rotulo="NIF"
              inputMode="numeric"
              value={dados.nif}
              onChange={(e) => setDados({ ...dados, nif: soDigitos(e.target.value).slice(0, 9) })}
              erro={erros.nif}
            />
            <Campo
              rotulo="Telemóvel"
              inputMode="tel"
              value={dados.telefone}
              onChange={(e) => setDados({ ...dados, telefone: mascaraTelefone(e.target.value) })}
              erro={erros.telefone}
            />
          </div>
          {erroDados ? (
            <div className="alerta alerta--erro" style={{ marginTop: 18 }}>
              <AlertTriangle size={18} />
              <span>{erroDados}</span>
            </div>
          ) : null}
          <Botao type="submit" className="btn--md perfil__guardar" carregando={aGuardar} disabled={!alterado}>
            Guardar alterações
          </Botao>
        </form>

        <div className="perfil__coluna">
          <section className="cartao cartao--pad">
            <h2 className="cartao__titulo">Meio de pagamento</h2>
            {meio ? (
              <>
                <CartaoVisual m={meio} />
                {meio.expirado ? (
                  <div className="alerta alerta--aviso" style={{ marginTop: 14 }}>
                    <AlertTriangle size={18} />
                    <span>Este cartão está fora da validade. Atualize-o para continuar a reservar.</span>
                  </div>
                ) : null}
                <Botao variante="outline" className="btn--bloco btn--md" style={{ marginTop: 18 }} onClick={abrirModalPag}>
                  Alterar meio de pagamento
                </Botao>
              </>
            ) : (
              <div className="sem-meio">
                <CreditCard size={28} />
                <p>Sem meio de pagamento. Associe um cartão ou uma conta bancária para poder reservar.</p>
                <Botao variante="navy" className="btn--md" onClick={abrirModalPag}>
                  <Plus size={18} /> Associar
                </Botao>
              </div>
            )}
          </section>

          <section className="cartao cartao--pad sessao">
            <p>Sessão iniciada como {utilizador.email}</p>
            <Botao
              variante="cinza"
              className="btn--md"
              onClick={() => {
                sair();
                nav('/entrar', { replace: true });
              }}
            >
              <LogOut size={18} /> Terminar sessão
            </Botao>
          </section>
        </div>
      </div>

      {modalPag ? (
        <Modal
          titulo="Alterar meio de pagamento"
          subtitulo={divida > 0 ? `A dívida de ${euro(divida)} é cobrada logo a seguir no novo meio.` : 'O novo meio substitui o atual.'}
          aoFechar={() => !aGuardarPag && setModalPag(false)}
          rodape={
            <>
              <Botao variante="claro" className="btn--md" onClick={() => setModalPag(false)} disabled={aGuardarPag}>
                Cancelar
              </Botao>
              <Botao className="btn--md" onClick={guardarPagamento} carregando={aGuardarPag}>
                Guardar
              </Botao>
            </>
          }
        >
          <CamposPagamento valor={pag} aoMudar={setPag} erros={tentouPag ? errosPagamento(pag) : {}} />
          {erroPag ? (
            <div className="alerta alerta--erro" style={{ marginTop: 14 }}>
              <AlertTriangle size={18} />
              <span>{erroPag}</span>
            </div>
          ) : null}
        </Modal>
      ) : null}
    </main>
  );
}
