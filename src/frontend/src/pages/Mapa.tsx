import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Circle, MapContainer, Marker, useMap, useMapEvents } from 'react-leaflet';
import { AlertTriangle, Car, Crosshair, LocateFixed, MapPin, Search, Users, X, Zap } from 'lucide-react';
import { api, type ApiError } from '../api/client';
import type { Viatura } from '../api/types';
import { useSessao } from '../state/sessao';
import { useToast } from '../state/toast';
import { AjustarTamanho, Centrar, Mosaico, iconeEu, iconePonto, iconePreco } from '../components/mapa';
import { Botao, Spinner } from '../components/ui';
import { ConfirmarReserva } from '../components/ConfirmarReserva';
import { LOCAIS, distanciaKm, usePosicao, type Ponto } from '../lib/geo';
import { distancia, euro, minutosAPe, nomeModelo, porKm, porMinuto } from '../lib/format';
import { useIntervalo, useTitulo } from '../lib/hooks';

const normalizar = (s: string) =>
  s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim();

type Sugestao =
  | { tipo: 'local'; nome: string; ponto: Ponto }
  | { tipo: 'carro'; v: Viatura };

function PinCarro({ v, sel, compacto, aoClicar }: { v: Viatura; sel: boolean; compacto: boolean; aoClicar: () => void }) {
  const icone = useMemo(
    () => (compacto ? iconePonto() : iconePreco(porMinuto(v.precoMinuto), sel)),
    [v.precoMinuto, sel, compacto],
  );
  if (v.latitude == null || v.longitude == null) return null;
  return (
    <Marker
      position={[v.latitude, v.longitude]}
      icon={icone}
      zIndexOffset={sel ? 1000 : compacto ? -100 : 0}
      eventHandlers={{ click: aoClicar }}
      keyboard
      title={`${nomeModelo(v.modelo)} · ${v.matricula} · ${porMinuto(v.precoMinuto)}`}
    />
  );
}

/**
 * Pinos dos carros. Para os preços não se sobreporem, só os carros mais próximos (e o selecionado)
 * mostram a etiqueta com o preço; os outros aparecem como um ponto. Ao aproximar o mapa aparecem mais etiquetas.
 */
function PinosCarros({ viaturas, selId, aoSelecionar }: { viaturas: Viatura[]; selId: number | null; aoSelecionar: (v: Viatura) => void }) {
  const map = useMap();
  const [vista, setVista] = useState(0);
  useMapEvents({
    zoomend: () => setVista((n) => n + 1),
    moveend: () => setVista((n) => n + 1),
    resize: () => setVista((n) => n + 1),
  });

  const comEtiqueta = useMemo(() => {
    const ocupado: { x: number; y: number }[] = [];
    const res = new Set<number>();
    const ordem = [...viaturas].sort((a, b) =>
      a.id === selId ? -1 : b.id === selId ? 1 : (a.distanciaKm ?? 0) - (b.distanciaKm ?? 0),
    );
    for (const v of ordem) {
      if (v.latitude == null || v.longitude == null) continue;
      const p = map.latLngToContainerPoint([v.latitude, v.longitude]);
      // etiqueta com cerca de 140 x 44 px, centrada no ponto
      const colide = ocupado.some((o) => Math.abs(o.x - p.x) < 146 && Math.abs(o.y - p.y) < 48);
      if (!colide || v.id === selId) {
        ocupado.push({ x: p.x, y: p.y });
        res.add(v.id);
      }
    }
    return res;
  }, [viaturas, selId, vista, map]); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <>
      {viaturas.map((v) => (
        <PinCarro key={v.id} v={v} sel={v.id === selId} compacto={!comEtiqueta.has(v.id)} aoClicar={() => aoSelecionar(v)} />
      ))}
    </>
  );
}

export default function Mapa() {
  useTitulo('Mapa');
  const nav = useNavigate();
  const toast = useToast();
  const { utilizador, ativa, definirAtiva, atualizarAtiva } = useSessao();
  const { posicao, estado: estadoGps } = usePosicao();

  const [referencia, setReferencia] = useState<{ nome: string; ponto: Ponto } | null>(null);
  // Centro da pesquisa: a zona escolhida ou a posição do utilizador (só muda se mexer mais de 50 m)
  const centroBruto = referencia?.ponto ?? posicao;
  const [centro, setCentro] = useState<Ponto>(centroBruto);
  const [foco, setFoco] = useState<Ponto>(centroBruto);
  const [viaturas, setViaturas] = useState<Viatura[] | null>(null);
  const [erroLista, setErroLista] = useState<string | null>(null);
  const [selId, setSelId] = useState<number | null>(null);
  const [aReservar, setAReservar] = useState(false);
  const [erroReserva, setErroReserva] = useState<string | null>(null);
  // Carro à espera de confirmação (o temporizador só começa depois de confirmar)
  const [aConfirmar, setAConfirmar] = useState<Viatura | null>(null);
  const [pesquisa, setPesquisa] = useState('');
  const [abertoSug, setAbertoSug] = useState(false);
  const listaRef = useRef<HTMLDivElement>(null);
  const escolhaManual = useRef(false);

  const carregar = useCallback(async () => {
    try {
      const lista = await api.viaturas({ lat: centro.lat, lng: centro.lng, disponiveis: true });
      const comDist = lista
        .filter((v) => v.latitude != null && v.longitude != null)
        .map((v) => ({
          ...v,
          distanciaKm: v.distanciaKm ?? distanciaKm(centro, { lat: v.latitude!, lng: v.longitude! }),
        }))
        .sort((a, b) => (a.distanciaKm ?? 0) - (b.distanciaKm ?? 0));
      setViaturas(comDist);
      setErroLista(null);
      // Enquanto o utilizador não escolher um carro, fica selecionado o mais próximo
      setSelId((atual) =>
        escolhaManual.current && atual != null && comDist.some((v) => v.id === atual) ? atual : comDist[0]?.id ?? null,
      );
    } catch (e) {
      setErroLista((e as ApiError).message);
      setViaturas((v) => v ?? []);
    }
  }, [centro.lat, centro.lng]);

  useEffect(() => {
    void carregar();
  }, [carregar]);
  useEffect(() => {
    if (distanciaKm(centro, centroBruto) > 0.05) {
      setCentro(centroBruto);
      setFoco(centroBruto);
    }
  }, [centroBruto.lat, centroBruto.lng]); // eslint-disable-line react-hooks/exhaustive-deps
  // O mapa atualiza-se sozinho (carros que ficam livres ou são reservados por outros)
  useIntervalo(() => void carregar(), 30000);

  const sel = viaturas?.find((v) => v.id === selId) ?? null;
  const perto = viaturas?.filter((v) => (v.distanciaKm ?? 99) <= 1).length ?? 0;

  const sugestoes = useMemo<Sugestao[]>(() => {
    const t = normalizar(pesquisa);
    if (!t) return LOCAIS.slice(0, 5).map((l) => ({ tipo: 'local' as const, nome: l.nome, ponto: l }));
    const locais = LOCAIS.filter((l) => normalizar(l.nome).includes(t))
      .slice(0, 5)
      .map((l) => ({ tipo: 'local' as const, nome: l.nome, ponto: l }));
    const carros = (viaturas ?? [])
      .filter((v) => normalizar(`${nomeModelo(v.modelo)} ${v.matricula}`).includes(t))
      .slice(0, 4)
      .map((v) => ({ tipo: 'carro' as const, v }));
    return [...locais, ...carros];
  }, [pesquisa, viaturas]);

  function escolherSugestao(s: Sugestao) {
    setAbertoSug(false);
    if (s.tipo === 'local') {
      escolhaManual.current = false;
      setReferencia({ nome: s.nome, ponto: s.ponto });
      setPesquisa(s.nome);
    } else {
      selecionar(s.v, true);
      setPesquisa('');
    }
  }

  function selecionar(v: Viatura, centrar = false) {
    escolhaManual.current = true;
    setSelId(v.id);
    setErroReserva(null);
    if (centrar && v.latitude != null && v.longitude != null) setFoco({ lat: v.latitude, lng: v.longitude });
    window.setTimeout(() => {
      listaRef.current?.querySelector(`[data-id="${v.id}"]`)?.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
    }, 50);
  }

  function pedirConfirmacao() {
    if (!sel) return;
    setErroReserva(null);
    setAConfirmar(sel);
  }

  async function reservar() {
    const carro = aConfirmar;
    if (!carro) return;
    setAReservar(true);
    setErroReserva(null);
    try {
      const r = await api.reservar(carro.id);
      definirAtiva(r);
      setAConfirmar(null);
      toast(`${nomeModelo(carro.modelo)} reservado. O tempo começou a contar.`);
      nav('/viagem');
    } catch (e) {
      setErroReserva((e as ApiError).message);
      void carregar();
      void atualizarAtiva();
    } finally {
      setAReservar(false);
    }
  }

  const divida = utilizador?.valorEmDivida ?? 0;
  const semMeio = !utilizador?.meioPagamento;

  return (
    <div className="mapa-pagina">
      <div className="mapa-area">
        <MapContainer
          center={[centro.lat, centro.lng]}
          zoom={15}
          minZoom={11}
          zoomControl={false}
          className="mapa-leaflet"
          attributionControl
        >
          <Mosaico />
          <AjustarTamanho />
          <Centrar centro={foco} />
          <Circle
            center={[centro.lat, centro.lng]}
            radius={1000}
            pathOptions={{ color: '#1d6fb8', weight: 1.5, opacity: 0.35, dashArray: '6 8', fillColor: '#6cc4f8', fillOpacity: 0.07 }}
          />
          <Marker position={[posicao.lat, posicao.lng]} icon={iconeEu} interactive={false} />
          <PinosCarros viaturas={viaturas ?? []} selId={selId} aoSelecionar={(v) => selecionar(v)} />
        </MapContainer>

        <div className="mapa-topo">
          <div className="pesquisa">
            <Search size={20} className="pesquisa__ico" />
            <input
              className="pesquisa__input"
              placeholder="Pesquisar morada ou zona"
              value={pesquisa}
              onChange={(e) => {
                setPesquisa(e.target.value);
                setAbertoSug(true);
              }}
              onFocus={() => setAbertoSug(true)}
              onBlur={() => window.setTimeout(() => setAbertoSug(false), 150)}
              onKeyDown={(e) => {
                if (e.key === 'Enter' && sugestoes[0]) escolherSugestao(sugestoes[0]);
                if (e.key === 'Escape') setAbertoSug(false);
              }}
              aria-label="Pesquisar morada ou zona"
            />
            {referencia || pesquisa ? (
              <button
                className="pesquisa__limpar"
                aria-label="Limpar pesquisa"
                onClick={() => {
                  setPesquisa('');
                  setReferencia(null);
                }}
              >
                <X size={18} />
              </button>
            ) : null}
            {abertoSug && sugestoes.length > 0 ? (
              <ul className="sugestoes" role="listbox">
                {sugestoes.map((s) => (
                  <li key={s.tipo === 'local' ? `l-${s.nome}` : `c-${s.v.id}`}>
                    <button type="button" onMouseDown={(e) => e.preventDefault()} onClick={() => escolherSugestao(s)}>
                      {s.tipo === 'local' ? <MapPin size={18} /> : <Car size={18} />}
                      <span>
                        {s.tipo === 'local' ? s.nome : `${nomeModelo(s.v.modelo)} · ${s.v.matricula}`}
                        <small>{s.tipo === 'local' ? 'Guimarães' : `${distancia(s.v.distanciaKm)} · ${porMinuto(s.v.precoMinuto)}`}</small>
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            ) : null}
          </div>
          <div className="chip-mapa" aria-live="polite">
            <span className="chip-mapa__ponto" />
            {viaturas === null ? (
              'A procurar carros…'
            ) : (
              <>
                <b>{perto}</b> {perto === 1 ? 'carro' : 'carros'} num raio de 1 km
              </>
            )}
          </div>
        </div>

        <div className="mapa-controlos">
          <button
            className="mapa-botao"
            title="Centrar na minha posição"
            aria-label="Centrar na minha posição"
            onClick={() => {
              setReferencia(null);
              setPesquisa('');
              setFoco({ ...posicao });
            }}
          >
            <LocateFixed size={20} />
          </button>
        </div>

        {estadoGps === 'fora-da-zona' || estadoGps === 'sem-permissao' ? (
          <div className="mapa-aviso">
            <Crosshair size={16} />
            {estadoGps === 'fora-da-zona' ? 'Fora da zona DriveNow: a mostrar Guimarães' : 'Sem localização: a mostrar Guimarães'}
          </div>
        ) : null}
      </div>

      <aside className="painel">
        {ativa ? (
          <div className="cartao-navy painel__ativa">
            <span className="etiqueta-sec">
              <span className="ponto-vivo" /> {ativa.estado === 'EM_CURSO' ? 'Em viagem' : 'Reserva ativa'}
            </span>
            <p className="painel__ativa-txt">
              Tem o carro <b>{ativa.matricula}</b> {ativa.estado === 'EM_CURSO' ? 'em viagem' : 'reservado'}. Só pode ter uma
              reserva de cada vez.
            </p>
            <Link to="/viagem" className="btn btn--sky btn--bloco">
              {ativa.estado === 'EM_CURSO' ? 'Ver viagem' : 'Ver reserva'}
            </Link>
          </div>
        ) : sel ? (
          <div className="cartao-navy selecionado">
            <div className="selecionado__cab">
              <div style={{ minWidth: 0 }}>
                <span className="etiqueta-sec">Carro selecionado</span>
                <h2 className="selecionado__modelo">{nomeModelo(sel.modelo)}</h2>
                <p className="selecionado__sub">
                  Elétrico · {sel.matricula}
                  {sel.ano ? ` · ${sel.ano}` : ''}
                </p>
              </div>
              {sel.lugares ? (
                <span className="chip-navy">
                  <Users size={15} /> {sel.lugares} lugares
                </span>
              ) : null}
            </div>
            <div className="selecionado__tiles">
              <div className="tile-navy">
                <div className="tile-navy__rot">Distância</div>
                <div className="tile-navy__val">{distancia(sel.distanciaKm)}</div>
                {sel.distanciaKm != null ? <div className="tile-navy__extra">{minutosAPe(sel.distanciaKm)} min a pé</div> : null}
              </div>
              <div className="tile-navy">
                <div className="tile-navy__rot">Taxa de entrada</div>
                <div className="tile-navy__val">{euro(sel.taxaEntrada)}</div>
                <div className="tile-navy__extra">por viagem</div>
              </div>
              <div className="tile-navy">
                <div className="tile-navy__rot">Preço</div>
                <div className="tile-navy__val tile-navy__val--sky">{porMinuto(sel.precoMinuto)}</div>
                <div className="tile-navy__extra">+ {porKm(sel.precoKm)} · IVA incl.</div>
              </div>
            </div>

            {erroReserva ? (
              <div className="alerta alerta--navy" role="alert">
                <AlertTriangle size={18} />
                <span>
                  {erroReserva}
                  {/[.!?]$/.test(erroReserva) ? '' : '.'}
                  {/dívida|cartão|conta bancária/i.test(erroReserva) ? (
                    <>
                      {' '}
                      <Link to="/perfil">Ir para o perfil</Link>
                    </>
                  ) : null}
                </span>
              </div>
            ) : null}

            <Botao variante="sky" className="btn--bloco btn--lg" onClick={pedirConfirmacao}>
              Reservar este carro
            </Botao>
            <p className="selecionado__nota">O temporizador começa no momento da reserva.</p>
          </div>
        ) : (
          <div className="cartao-navy selecionado selecionado--vazio">
            <span className="etiqueta-sec">Carro selecionado</span>
            <p>{viaturas === null ? 'A procurar carros perto de si…' : 'Escolha um carro no mapa ou na lista.'}</p>
          </div>
        )}

        {!ativa && (divida > 0 || semMeio) ? (
          <div className="alerta alerta--aviso painel__alerta">
            <AlertTriangle size={18} />
            <span>
              {divida > 0 ? (
                <>
                  Tem <strong>{euro(divida)}</strong> em dívida. Atualize o meio de pagamento para voltar a reservar.
                </>
              ) : (
                <>Associe um cartão ou uma conta bancária para poder reservar.</>
              )}{' '}
              <Link to="/perfil">Abrir perfil</Link>
            </span>
          </div>
        ) : null}

        <div className="painel__lista-cab">
          <h3>Carros mais próximos</h3>
          {referencia ? <span className="chip chip--cinza">perto de {referencia.nome}</span> : null}
        </div>

        <div className="lista-carros" ref={listaRef}>
          {viaturas === null ? (
            <div className="vazio">
              <Spinner grande />
            </div>
          ) : erroLista ? (
            <div className="alerta alerta--erro">
              <AlertTriangle size={18} />
              <span>
                Não foi possível carregar os carros. {erroLista}{' '}
                <button className="link-btn" onClick={() => void carregar()}>
                  Tentar outra vez
                </button>
              </span>
            </div>
          ) : viaturas.length === 0 ? (
            <div className="vazio">
              <Car size={34} />
              <h3>Não há carros livres agora</h3>
              <p>O mapa atualiza-se sozinho quando um carro ficar livre.</p>
            </div>
          ) : (
            viaturas.map((v) => (
              <button
                key={v.id}
                data-id={v.id}
                className={`item-carro${v.id === selId ? ' item-carro--sel' : ''}`}
                onClick={() => selecionar(v, true)}
              >
                <span className="item-carro__txt">
                  <span className="item-carro__nome">{nomeModelo(v.modelo)}</span>
                  <span className="item-carro__sub">
                    <Zap size={13} /> Elétrico · {distancia(v.distanciaKm)} · {v.matricula}
                  </span>
                </span>
                <span className="item-carro__preco">{porMinuto(v.precoMinuto)}</span>
              </button>
            ))
          )}
        </div>
      </aside>

      {aConfirmar ? (
        <ConfirmarReserva
          viatura={viaturas?.find((v) => v.id === aConfirmar.id) ?? aConfirmar}
          meioPagamento={utilizador?.meioPagamento?.descricao ?? null}
          aReservar={aReservar}
          erro={erroReserva}
          aoFechar={() => setAConfirmar(null)}
          aoConfirmar={reservar}
        />
      ) : null}
    </div>
  );
}
