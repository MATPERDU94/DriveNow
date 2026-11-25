import { useEffect, useMemo, useState } from 'react';
import { MapContainer, Marker } from 'react-leaflet';
import type { Marker as MarcadorLeaflet } from 'leaflet';
import { useSessao } from '../state/sessao';
import { AlertTriangle, Check, CheckCircle2, Info, LocateFixed, ShieldAlert } from 'lucide-react';
import { api, type ApiError } from '../api/client';
import type { Relato, Reserva, TipoDano, Viatura } from '../api/types';
import { AjustarTamanho, AoClicar, Centrar, Mosaico, iconeCarro } from './mapa';
import { Botao, Campo, Modal, Spinner } from './ui';
import { MAX_KM_VIAGEM, euro, km as fmtKm, kmViagem as fmtKmViagem, porKm } from '../lib/format';
import { GUIMARAES, moradaAproximada, type Ponto } from '../lib/geo';

let cacheTipos: TipoDano[] | null = null;

/** Franquia: por viagem, o cliente nunca paga mais do que isto em danos. */
const FRANQUIA = 1000;

const ORDEM_GRAVIDADE: Record<string, number> = { ligeiro: 0, moderado: 1, grave: 2 };

export function EstadoCarroModal({
  tipo,
  reserva,
  viatura,
  posicaoGps,
  valorAteAgora,
  aoFechar,
  aoConcluir,
}: {
  tipo: 'INICIO' | 'FIM';
  reserva: Reserva;
  viatura: Viatura | null;
  posicaoGps: Ponto | null;
  valorAteAgora: number;
  aoFechar: () => void;
  aoConcluir: (r: Relato) => void;
}) {
  const fim = tipo === 'FIM';
  const { utilizador } = useSessao();
  const [tipos, setTipos] = useState<TipoDano[] | null>(cacheTipos);
  const [erroTipos, setErroTipos] = useState<string | null>(null);
  const [comDanos, setComDanos] = useState(false);
  const [danos, setDanos] = useState<Record<number, string>>({});
  const [obs, setObs] = useState('');
  // Km no momento da reserva (guardados pelo backend); em reservas antigas, os km atuais do carro.
  const kmInicio = reserva.kmInicial ?? viatura?.quilometragem ?? null;
  const [kmFinal, setKmFinal] = useState('');
  const posCarro: Ponto | null =
    viatura?.latitude != null && viatura?.longitude != null ? { lat: viatura.latitude, lng: viatura.longitude } : null;
  const [local, setLocal] = useState<Ponto | null>(posicaoGps ?? posCarro ?? GUIMARAES);
  const [focoMapa, setFocoMapa] = useState<Ponto | null>(local);
  const [aEnviar, setAEnviar] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    if (cacheTipos) return;
    api
      .tiposDano()
      .then((t) => {
        cacheTipos = t;
        setTipos(t);
      })
      .catch((e: ApiError) => setErroTipos(e.message));
  }, []);

  const ordenados = useMemo(
    () =>
      [...(tipos ?? [])].sort(
        (a, b) =>
          (ORDEM_GRAVIDADE[a.gravidade ?? ''] ?? 9) - (ORDEM_GRAVIDADE[b.gravidade ?? ''] ?? 9) ||
          a.nome.localeCompare(b.nome, 'pt'),
      ),
    [tipos],
  );

  const escolhidos = comDanos ? ordenados.filter((t) => t.id in danos) : [];
  const penalizacao = Math.min(
    FRANQUIA,
    escolhidos.reduce((s, t) => s + (t.custo ?? 0), 0),
  );
  const imobiliza = escolhidos.some((t) => t.imobilizaCarro);
  const kmNum = kmFinal.trim() === '' ? null : Number(kmFinal.replace(',', '.'));
  const erroKm = !fim || kmNum == null
    ? null
    : Number.isNaN(kmNum)
      ? 'Escreva só o número de km'
      : kmInicio != null && kmNum < kmInicio
        ? `Não pode ser inferior aos km do início (${fmtKm(kmInicio)})`
        : kmInicio != null && kmNum - kmInicio > MAX_KM_VIAGEM
          ? `Seriam mais de ${fmtKm(MAX_KM_VIAGEM)} numa viagem. Confirme os km do painel`
          : null;
  // Km da viagem e quanto custam, enquanto o cliente escreve
  const kmDaViagem =
    fim && kmNum != null && !erroKm && kmInicio != null ? Math.round((kmNum - kmInicio) * 10) / 10 : null;
  const valorKm = kmDaViagem != null ? Math.round(kmDaViagem * (reserva.precoKm ?? 0) * 100) / 100 : 0;

  function alternar(t: TipoDano) {
    setDanos((d) => {
      const n = { ...d };
      if (t.id in n) delete n[t.id];
      else n[t.id] = '';
      return n;
    });
  }

  async function enviar() {
    setErro(null);
    if (comDanos && escolhidos.length === 0) {
      setErro('Escolha pelo menos um tipo de dano, ou indique que o carro está sem danos.');
      return;
    }
    if (fim && kmNum == null) {
      setErro('Indique os km que aparecem no painel do carro.');
      return;
    }
    if (fim && !local) {
      setErro('Indique no mapa onde deixou o carro.');
      return;
    }
    if (erroKm) {
      setErro(erroKm);
      return;
    }
    setAEnviar(true);
    try {
      const r = await api.relatar(reserva.id, {
        tipo,
        observacoes: obs.trim() || undefined,
        danos: escolhidos.map((t) => ({ tipoDanoId: t.id, descricao: danos[t.id]?.trim() || undefined })),
        ...(fim
          ? {
              latitude: Number(local!.lat.toFixed(6)),
              longitude: Number(local!.lng.toFixed(6)),
              quilometragem: kmNum!,
            }
          : {}),
      });
      aoConcluir(r);
    } catch (e) {
      setErro((e as ApiError).message);
      setAEnviar(false);
    }
  }

  return (
    <Modal
      largo
      titulo={fim ? 'Terminar viagem' : 'Como está o carro?'}
      subtitulo={
        fim
          ? 'Indique o estado do carro, os quilómetros e onde o deixou.'
          : 'Antes de começar, veja o carro à volta. Danos que já lá estavam não são cobrados a si.'
      }
      aoFechar={aEnviar ? () => {} : aoFechar}
      rodape={
        <>
          <Botao variante="claro" className="btn--md" onClick={aoFechar} disabled={aEnviar}>
            Voltar
          </Botao>
          <Botao variante={fim ? 'navy' : 'sky'} className="btn--md" onClick={enviar} carregando={aEnviar}>
            {fim ? 'Terminar e pagar' : 'Começar viagem'}
          </Botao>
        </>
      }
    >
      <div className="estado-opcoes" role="radiogroup" aria-label="Estado do carro">
        <button
          type="button"
          role="radio"
          aria-checked={!comDanos}
          className={`estado-opcao${!comDanos ? ' estado-opcao--ativa' : ''}`}
          onClick={() => setComDanos(false)}
        >
          <CheckCircle2 size={24} />
          <span>
            <b>Sem danos</b>
            <small>{fim ? 'Deixo o carro como o encontrei' : 'O carro está em bom estado'}</small>
          </span>
        </button>
        <button
          type="button"
          role="radio"
          aria-checked={comDanos}
          className={`estado-opcao${comDanos ? ' estado-opcao--ativa estado-opcao--danos' : ''}`}
          onClick={() => setComDanos(true)}
        >
          <ShieldAlert size={24} />
          <span>
            <b>Reportar danos</b>
            <small>{fim ? 'Aconteceu algo durante a viagem' : 'Encontrei o carro com danos'}</small>
          </span>
        </button>
      </div>

      {comDanos ? (
        <div className="danos">
          {erroTipos ? (
            <div className="alerta alerta--erro">
              <AlertTriangle size={18} />
              <span>Não foi possível carregar a tabela de danos: {erroTipos}</span>
            </div>
          ) : !tipos ? (
            <div className="vazio">
              <Spinner grande />
            </div>
          ) : (
            <div className="danos__grelha">
              {ordenados.map((t) => {
                const ativo = t.id in danos;
                return (
                  <div key={t.id} className={`dano${ativo ? ' dano--ativo' : ''}`}>
                    <button type="button" className="dano__cab" onClick={() => alternar(t)} aria-pressed={ativo}>
                      <span className="dano__check">{ativo ? <Check size={14} strokeWidth={3} /> : null}</span>
                      <span className="dano__txt">
                        <b>{t.nome}</b>
                        {t.descricao ? <small>{t.descricao}</small> : null}
                      </span>
                      <span className="dano__meta">
                        {t.gravidade ? <span className={`grav grav--${t.gravidade}`}>{t.gravidade}</span> : null}
                        {fim && t.custo ? <span className="dano__custo">{euro(t.custo)}</span> : null}
                      </span>
                    </button>
                    {ativo ? (
                      <input
                        className="input dano__desc"
                        placeholder="Onde está e como é? (ex.: porta traseira esquerda, 5 cm)"
                        value={danos[t.id]}
                        onChange={(e) => setDanos((d) => ({ ...d, [t.id]: e.target.value }))}
                        autoFocus
                      />
                    ) : null}
                  </div>
                );
              })}
            </div>
          )}
          {imobiliza ? (
            <div className="alerta alerta--aviso" style={{ marginTop: 14 }}>
              <AlertTriangle size={18} />
              <span>
                Um dos danos impede o carro de circular. {fim ? 'O carro fica em reparação e sai do mapa.' : 'Fale com a DriveNow antes de conduzir.'}
              </span>
            </div>
          ) : null}
        </div>
      ) : null}

      {fim ? (
        <div className="fim-extra">
          <div className="fim-extra__col">
            <Campo
              rotulo="Quilómetros no painel"
              inputMode="numeric"
              value={kmFinal}
              onChange={(e) => {
                setKmFinal(e.target.value.replace(/[^\d.,]/g, ''));
                setErro(null);
              }}
              erro={erroKm}
              ajuda={
                kmDaViagem != null
                  ? `Viagem de ${fmtKmViagem(kmDaViagem)} (no início: ${fmtKm(kmInicio)})`
                  : kmInicio != null
                    ? `No início da viagem: ${fmtKm(kmInicio)}`
                    : undefined
              }
            />
            <div className="campo" style={{ marginTop: 18 }}>
              <label className="campo__rot" htmlFor="obs-fim">
                Observações (opcional)
              </label>
              <textarea id="obs-fim" className="input" value={obs} onChange={(e) => setObs(e.target.value)} rows={3} />
            </div>
          </div>
          <div className="fim-extra__col">
            <div className="campo__rot" style={{ marginBottom: 8 }}>
              <span>Onde deixou o carro</span>
              {posicaoGps ? (
                <button
                  type="button"
                  className="link-btn"
                  onClick={() => {
                    setLocal(posicaoGps);
                    setFocoMapa({ ...posicaoGps });
                  }}
                >
                  <LocateFixed size={14} style={{ verticalAlign: '-2px' }} /> A minha posição
                </button>
              ) : null}
            </div>
            <div className="mini-mapa mini-mapa--alto">
              {local ? (
                <MapContainer center={[local.lat, local.lng]} zoom={16} zoomControl={false} attributionControl={false}>
                  <Mosaico />
                  <AjustarTamanho />
                  {focoMapa ? <Centrar centro={focoMapa} /> : null}
                  <AoClicar fn={setLocal} />
                  <Marker
                    position={[local.lat, local.lng]}
                    icon={iconeCarro()}
                    draggable
                    eventHandlers={{
                      dragend: (e) => {
                        const p = (e.target as MarcadorLeaflet).getLatLng();
                        setLocal({ lat: p.lat, lng: p.lng });
                      },
                    }}
                  />
                </MapContainer>
              ) : null}
            </div>
            <p className="campo__ajuda" style={{ marginTop: 8 }}>
              {local ? moradaAproximada(local) : '-'} | toque no mapa para ajustar
            </p>
          </div>
        </div>
      ) : (
        <div className="campo" style={{ marginTop: 18 }}>
          <label className="campo__rot" htmlFor="obs-ini">
            Observações (opcional)
          </label>
          <textarea
            id="obs-ini"
            className="input"
            value={obs}
            onChange={(e) => setObs(e.target.value)}
            rows={2}
            placeholder="Ex.: interior limpo, bateria a 80%"
          />
        </div>
      )}

      {fim ? (
        <div className="estimativa">
          <div>
            <span>Taxa e tempo até agora</span>
            <b className="tabular">{euro(valorAteAgora)}</b>
          </div>
          <div>
            <span>
              {kmDaViagem != null ? `${fmtKmViagem(kmDaViagem)} x ${porKm(reserva.precoKm)}` : `Km da viagem (${porKm(reserva.precoKm)})`}
            </span>
            <b className="tabular">{kmDaViagem != null ? euro(valorKm) : '-'}</b>
          </div>
          {penalizacao > 0 ? (
            <div>
              <span>Danos reportados{penalizacao >= FRANQUIA ? ' (franquia)' : ''}</span>
              <b className="tabular">{euro(penalizacao)}</b>
            </div>
          ) : null}
          <div className="estimativa__total">
            <span>Total estimado</span>
            <b className="tabular">{euro(valorAteAgora + valorKm + penalizacao)}</b>
          </div>
          <p>
            <Info size={14} /> Debitado automaticamente em {utilizador?.meioPagamento?.descricao ?? 'no seu meio de pagamento'}. Danos
            repetidos não contam duas vezes.
          </p>
        </div>
      ) : (
        <div className="alerta alerta--info" style={{ marginTop: 18 }}>
          <Info size={18} />
          <span>
            Se reportar um dano agora, ele fica registado no carro e é cobrado a quem o usou antes de si - não a si.
          </span>
        </div>
      )}

      {erro ? (
        <div className="alerta alerta--erro" role="alert" style={{ marginTop: 16 }}>
          <AlertTriangle size={18} />
          <span>{erro}</span>
        </div>
      ) : null}
    </Modal>
  );
}
