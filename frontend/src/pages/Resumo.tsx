import { useCallback, useEffect, useState } from 'react';
import { Link, Navigate, useLocation, useParams } from 'react-router-dom';
import { AlertTriangle, Check, FileClock, FileText, FileWarning, ShieldAlert, X } from 'lucide-react';
import { abrirFatura, api, type ApiError } from '../api/client';
import type { Relato, Reserva, Viatura } from '../api/types';
import { useSessao } from '../state/sessao';
import { useToast } from '../state/toast';
import { Botao, Carregar, Spinner } from '../components/ui';
import { data, duracao, euro, hora, nomeModelo, semIva, totalDe } from '../lib/format';
import { useIntervalo, useTitulo } from '../lib/hooks';

export default function Resumo() {
  const { id } = useParams();
  const loc = useLocation();
  const acabou = (loc.state as { acabou?: boolean } | null)?.acabou;
  const { utilizador } = useSessao();
  const toast = useToast();
  const [r, setR] = useState<Reserva | null>(null);
  const [viatura, setViatura] = useState<Viatura | null>(null);
  const [relatos, setRelatos] = useState<Relato[]>([]);
  const [erro, setErro] = useState<string | null>(null);
  const [aFaturar, setAFaturar] = useState(false);
  const [inicioPoll] = useState(() => Date.now());

  const carregar = useCallback(async () => {
    try {
      const res = await api.reserva(Number(id));
      setR(res);
    } catch (e) {
      setErro((e as ApiError).message);
    }
  }, [id]);

  useEffect(() => {
    void carregar();
    api.relatos(Number(id)).then(setRelatos).catch(() => setRelatos([]));
  }, [carregar, id]);

  useEffect(() => {
    if (!r) return;
    api.viatura(r.viaturaId).then(setViatura).catch(() => setViatura(null));
  }, [r?.viaturaId]); // eslint-disable-line react-hooks/exhaustive-deps

  // A fatura é emitida em segundo plano: vai-se perguntando durante 2 minutos
  const aEmitir = !!r && (r.estadoFatura == null || r.estadoFatura === 'PENDENTE');
  useIntervalo(() => void carregar(), 3000, aEmitir && Date.now() - inicioPoll < 120000);

  const cancelada = r?.estado === 'CANCELADA';
  useTitulo(cancelada ? 'Reserva cancelada' : 'Viagem terminada');

  if (erro && !r) {
    return (
      <main className="pagina pagina--estreita">
        <div className="alerta alerta--erro">
          <AlertTriangle size={18} />
          <span>{erro}</span>
        </div>
      </main>
    );
  }
  if (!r) return <Carregar />;
  if (r.estado === 'RESERVADA' || r.estado === 'EM_CURSO') return <Navigate to="/viagem" replace />;

  const total = totalDe(r);
  const base = semIva(total);
  const iva = Math.round((total - base) * 100) / 100;
  const modelo = viatura ? nomeModelo(viatura.modelo) : 'Carro';
  const danos = relatos.flatMap((rel) => rel.danos.map((d) => ({ ...d, momento: rel.tipo })));

  async function tentarFaturar() {
    setAFaturar(true);
    try {
      const res = await api.faturar(r!.id);
      setR(res);
      toast(res.estadoFatura === 'EMITIDA' ? 'Fatura emitida.' : 'Pedido enviado para a faturação.');
    } catch (e) {
      toast((e as ApiError).message, 'erro');
      void carregar();
    } finally {
      setAFaturar(false);
    }
  }

  return (
    <main className="pagina resumo">
      <div className="resumo__cab">
        <span className={`resumo__icone${cancelada ? ' resumo__icone--cinza' : ''}`}>
          {cancelada ? <X size={28} strokeWidth={2.6} /> : <Check size={28} strokeWidth={2.8} />}
        </span>
        <div>
          <h1 className="titulo resumo__titulo">{cancelada ? 'Reserva cancelada' : acabou ? 'Viagem terminada' : 'Resumo da viagem'}</h1>
          <p className="subtitulo resumo__sub">
            {cancelada ? 'O carro voltou a ficar livre para outros clientes.' : 'Obrigado por conduzir com a DriveNow.'}
          </p>
        </div>
      </div>

      <section className="cartao recibo">
        <div className="recibo__topo">
          <div>
            <p className="recibo__carro">
              {modelo} · {r.matricula}
            </p>
            <p className="recibo__quando">
              {data(r.criadaEm)} · {hora(r.criadaEm)} → {hora(r.fimViagem)}
            </p>
          </div>
          <p className="recibo__total tabular">{euro(total)}</p>
        </div>
        <dl className="recibo__linhas">
          <div>
            <dt>Duração</dt>
            <dd>{duracao(r.minutos ?? r.minutosDecorridos)}</dd>
          </div>
          <div>
            <dt>Taxa de entrada</dt>
            <dd>{euro(r.taxaEntrada)}</dd>
          </div>
          <div>
            <dt>Preço por minuto</dt>
            <dd>{euro(r.precoMinuto)}</dd>
          </div>
          {r.totalPenalizacoes ? (
            <div>
              <dt>Penalizações por danos</dt>
              <dd>{euro(r.totalPenalizacoes)}</dd>
            </div>
          ) : null}
          <div>
            <dt>Valor sem IVA</dt>
            <dd>{euro(base)}</dd>
          </div>
          <div>
            <dt>IVA (23%)</dt>
            <dd>{euro(iva)}</dd>
          </div>
          <div>
            <dt>Pagamento</dt>
            <dd>
              {r.estadoPagamento === 'EM_DIVIDA' ? (
                <span className="chip chip--perigo">Recusado · em dívida</span>
              ) : r.estadoPagamento === 'PAGO' ? (
                r.pagamentoDescricao ?? 'Pago'
              ) : (
                <span className="chip chip--cinza">A processar</span>
              )}
            </dd>
          </div>
        </dl>
      </section>

      {r.estadoPagamento === 'EM_DIVIDA' ? (
        <div className="alerta alerta--erro resumo__bloco">
          <AlertTriangle size={18} />
          <span>
            <strong>O pagamento foi recusado.</strong> Fica em dívida e não pode fazer novas reservas até atualizar o meio de
            pagamento. Quando o fizer, a dívida é cobrada automaticamente. <Link to="/perfil">Atualizar agora</Link>
          </span>
        </div>
      ) : null}

      {danos.length > 0 ? (
        <section className="cartao cartao--pad resumo__bloco">
          <h2 className="cartao__titulo" style={{ marginBottom: 12 }}>
            Danos reportados
          </h2>
          <ul className="lista-danos">
            {danos.map((d, i) => (
              <li key={i}>
                <ShieldAlert size={18} />
                <div>
                  <b>{d.tipoDano}</b>
                  <small>
                    {d.momento === 'INICIO' ? 'No início da viagem' : 'No fim da viagem'}
                    {d.descricao ? ` · ${d.descricao}` : ''}
                    {d.nota ? ` · ${d.nota}` : ''}
                  </small>
                </div>
                <span className="tabular">{d.momento === 'FIM' && d.penalizacao ? euro(d.penalizacao) : 'Sem custo para si'}</span>
              </li>
            ))}
          </ul>
        </section>
      ) : null}

      <section className={`fatura resumo__bloco${r.estadoFatura === 'ERRO' ? ' fatura--aviso' : ''}`}>
        {r.estadoFatura === 'EMITIDA' || r.estadoFatura === 'RASCUNHO' ? (
          <>
            <FileText size={30} />
            <div className="fatura__txt">
              <b>
                Fatura-recibo{r.moloniDocumentoId ? ` nº ${r.moloniDocumentoId}` : ''}
                {r.estadoFatura === 'RASCUNHO' ? ' (rascunho)' : ''}
              </b>
              <span>
                {r.estadoFatura === 'RASCUNHO'
                  ? 'Criada no sistema de faturação; fica emitida assim que for confirmada.'
                  : `Enviada para ${utilizador?.email ?? 'o seu email'}`}
              </span>
            </div>
            {r.faturaPdfUrl ? (
              <Botao variante="outline" className="btn--md" onClick={() => abrirFatura(r, modelo, utilizador?.email)}>
                Descarregar PDF
              </Botao>
            ) : null}
          </>
        ) : r.estadoFatura === 'ERRO' ? (
          <>
            <FileWarning size={30} />
            <div className="fatura__txt">
              <b>Fatura por emitir</b>
              <span>O sistema de faturação não respondeu. A fatura é emitida automaticamente assim que responder.</span>
            </div>
            <Botao variante="outline" className="btn--md" onClick={tentarFaturar} carregando={aFaturar}>
              Tentar agora
            </Botao>
          </>
        ) : (
          <>
            <FileClock size={30} />
            <div className="fatura__txt">
              <b>A emitir a fatura-recibo…</b>
              <span>Aparece aqui e no email assim que estiver pronta. Não precisa de esperar.</span>
            </div>
            {aEmitir && Date.now() - inicioPoll < 120000 ? <Spinner /> : null}
          </>
        )}
      </section>

      <div className="resumo__botoes">
        <Link to="/mapa" className="btn btn--navy btn--lg">
          Voltar ao mapa
        </Link>
        <Link to="/viagens" className="btn btn--claro btn--lg">
          Ver todas as viagens
        </Link>
      </div>
    </main>
  );
}
