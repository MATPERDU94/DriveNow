import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AlertTriangle, ChevronRight, ReceiptText } from 'lucide-react';
import { abrirFatura, api, type ApiError } from '../api/client';
import type { Reserva } from '../api/types';
import { useSessao } from '../state/sessao';
import { Carregar } from '../components/ui';
import { data, duracao, euro, nomeModelo, totalDe } from '../lib/format';
import { useIntervalo, useTitulo } from '../lib/hooks';

const TRINTA_DIAS = 30 * 24 * 3600 * 1000;

function ChipFatura({ r }: { r: Reserva }) {
  if (r.estadoFatura === 'EMITIDA') return <span className="chip">{r.moloniDocumentoId ? `FR nº ${r.moloniDocumentoId}` : 'Emitida'}</span>;
  if (r.estadoFatura === 'RASCUNHO') return <span className="chip chip--cinza">Rascunho</span>;
  return <span className="chip chip--aviso">Pendente</span>;
}

export default function Viagens() {
  useTitulo('Viagens e faturas');
  const nav = useNavigate();
  const { utilizador, ativa } = useSessao();
  const [lista, setLista] = useState<Reserva[] | null>(null);
  const [modelos, setModelos] = useState<Record<number, string>>({});
  const [erro, setErro] = useState<string | null>(null);
  const [periodo, setPeriodo] = useState<'30' | 'tudo'>('30');

  async function carregar() {
    try {
      const rs = await api.reservas();
      setLista(rs);
      setErro(null);
    } catch (e) {
      setErro((e as ApiError).message);
      setLista((l) => l ?? []);
    }
  }

  useEffect(() => {
    void carregar();
    // Nomes dos modelos (as reservas só têm a matrícula e o id da viatura)
    api
      .viaturas({})
      .then((vs) => setModelos(Object.fromEntries(vs.map((v) => [v.id, nomeModelo(v.modelo)]))))
      .catch(() => {});
  }, []);

  const terminadas = useMemo(
    () =>
      (lista ?? [])
        .filter((r) => r.estado === 'CONCLUIDA' || r.estado === 'CANCELADA')
        .sort((a, b) => Date.parse(b.criadaEm) - Date.parse(a.criadaEm)),
    [lista],
  );
  const visiveis = periodo === '30' ? terminadas.filter((r) => Date.now() - Date.parse(r.criadaEm) <= TRINTA_DIAS) : terminadas;
  const minutosTotal = visiveis.reduce((s, r) => s + (r.minutos ?? r.minutosDecorridos ?? 0), 0);
  const gastoTotal = visiveis.reduce((s, r) => s + totalDe(r), 0);
  const pendentes = visiveis.some((r) => r.estadoFatura !== 'EMITIDA');

  // Enquanto houver faturas por emitir, a lista atualiza-se
  useIntervalo(() => void carregar(), 10000, pendentes);

  if (lista === null && !erro) return <Carregar />;

  return (
    <main className="pagina">
      <div className="cabecalho-pagina">
        <div>
          <h1 className="titulo">Viagens e faturas</h1>
          <p className="subtitulo">{periodo === '30' ? 'Últimos 30 dias' : 'Todas as viagens'}</p>
        </div>
        <div className="segmentado" role="tablist" aria-label="Período">
          <button role="tab" aria-selected={periodo === '30'} onClick={() => setPeriodo('30')}>
            30 dias
          </button>
          <button role="tab" aria-selected={periodo === 'tudo'} onClick={() => setPeriodo('tudo')}>
            Tudo
          </button>
        </div>
      </div>

      {ativa ? (
        <Link to="/viagem" className="faixa-ativa">
          <span className="ponto-vivo" />
          <span>
            {ativa.estado === 'EM_CURSO' ? 'Tem uma viagem a decorrer' : 'Tem uma reserva ativa'} com o carro <b>{ativa.matricula}</b>.
          </span>
          <span className="faixa-ativa__ir">
            Ver <ChevronRight size={18} />
          </span>
        </Link>
      ) : null}

      <div className="kpis">
        <div className="kpi kpi--navy">
          <span>Viagens</span>
          <b className="tabular">{visiveis.length}</b>
        </div>
        <div className="kpi">
          <span>Tempo total</span>
          <b className="tabular">{duracao(minutosTotal)}</b>
        </div>
        <div className="kpi">
          <span>Total gasto</span>
          <b className="tabular kpi__azul">{euro(gastoTotal)}</b>
        </div>
      </div>

      {erro ? (
        <div className="alerta alerta--erro" style={{ marginBottom: 16 }}>
          <AlertTriangle size={18} />
          <span>{erro}</span>
        </div>
      ) : null}

      <div className="cartao tabela-wrap">
        {visiveis.length === 0 ? (
          <div className="vazio">
            <ReceiptText size={36} />
            <h3>{periodo === '30' ? 'Sem viagens nos últimos 30 dias' : 'Ainda não fez nenhuma viagem'}</h3>
            <p>
              As viagens e as faturas aparecem aqui. <Link to="/mapa">Encontrar um carro</Link>
            </p>
          </div>
        ) : (
          <table className="tabela">
            <thead>
              <tr>
                <th>Data</th>
                <th>Carro</th>
                <th>Duração</th>
                <th className="dir">Valor</th>
                <th>Fatura</th>
                <th aria-label="Ações" />
              </tr>
            </thead>
            <tbody>
              {visiveis.map((r) => {
                const modelo = modelos[r.viaturaId] ?? r.matricula;
                return (
                  <tr key={r.id} onClick={() => nav(`/viagens/${r.id}`)} className="tabela__linha">
                    <td className="forte" data-rot="Data">
                      {data(r.criadaEm)}
                    </td>
                    <td data-rot="Carro">
                      {modelo}
                      {r.estado === 'CANCELADA' ? <span className="chip chip--cinza chip--mini">Cancelada</span> : null}
                      {r.estadoPagamento === 'EM_DIVIDA' ? <span className="chip chip--perigo chip--mini">Em dívida</span> : null}
                    </td>
                    <td data-rot="Duração">{duracao(r.minutos ?? r.minutosDecorridos)}</td>
                    <td className="dir forte tabular" data-rot="Valor">
                      {euro(totalDe(r))}
                    </td>
                    <td data-rot="Fatura">
                      <ChipFatura r={r} />
                    </td>
                    <td className="dir" onClick={(e) => e.stopPropagation()}>
                      {r.faturaPdfUrl && (r.estadoFatura === 'EMITIDA' || r.estadoFatura === 'RASCUNHO') ? (
                        <button className="link-btn" onClick={() => abrirFatura(r, modelo, utilizador?.email)}>
                          Descarregar PDF
                        </button>
                      ) : r.estadoFatura === 'EMITIDA' ? (
                        <span className="muted">Enviada por email</span>
                      ) : (
                        <span className="muted">A emitir…</span>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        )}
      </div>

      <p className="nota-rodape">Faturas pendentes são emitidas automaticamente assim que o sistema de faturação responder.</p>
    </main>
  );
}
