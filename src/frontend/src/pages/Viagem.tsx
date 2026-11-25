import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { MapContainer, Marker } from 'react-leaflet';
import { CalendarDays, CarFront, Footprints, Gauge, Navigation, Users, Zap } from 'lucide-react';
import { api, type ApiError } from '../api/client';
import type { Viatura } from '../api/types';
import { useSessao } from '../state/sessao';
import { useToast } from '../state/toast';
import { AjustarTamanho, Mosaico, iconeCarro, iconeEu } from '../components/mapa';
import { EstadoCarroModal } from '../components/EstadoCarroModal';
import { Botao, Carregar, Modal } from '../components/ui';
import { cronometro, distancia, euro, km, minutosAPe, minutosCobrados, nomeModelo, porKm, porMinuto } from '../lib/format';
import { distanciaKm, moradaAproximada, usePosicao, type Ponto } from '../lib/geo';
import { useSegundosDesde, useTitulo } from '../lib/hooks';

export default function Viagem() {
  const { ativa, atualizarAtiva, definirAtiva, atualizarUtilizador } = useSessao();
  const toast = useToast();
  const nav = useNavigate();
  const { posicao, estado: estadoGps } = usePosicao();
  const [carregou, setCarregou] = useState(false);
  const [viatura, setViatura] = useState<Viatura | null>(null);
  const [modal, setModal] = useState<'INICIO' | 'FIM' | 'CANCELAR' | null>(null);
  const [aCancelar, setACancelar] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  const emCurso = ativa?.estado === 'EM_CURSO';
  useTitulo(ativa ? (emCurso ? 'Em viagem' : 'Reserva ativa') : 'Viagem');

  useEffect(() => {
    void atualizarAtiva().finally(() => setCarregou(true));
  }, [atualizarAtiva]);

  useEffect(() => {
    if (!ativa) return;
    api
      .viatura(ativa.viaturaId)
      .then(setViatura)
      .catch(() => setViatura(null));
  }, [ativa?.viaturaId]); // eslint-disable-line react-hooks/exhaustive-deps

  const segundos = useSegundosDesde(ativa?.criadaEm);
  const minutos = minutosCobrados(segundos);
  const valor = ativa ? ativa.taxaEntrada + minutos * ativa.precoMinuto : 0;

  const posCarro: Ponto | null = useMemo(
    () =>
      viatura?.latitude != null && viatura?.longitude != null ? { lat: viatura.latitude, lng: viatura.longitude } : null,
    [viatura?.latitude, viatura?.longitude],
  );
  const gpsOk = estadoGps === 'ok';
  const distCarro = posCarro && gpsOk ? distanciaKm(posicao, posCarro) : null;

  if (!carregou && !ativa) return <Carregar />;

  if (!ativa) {
    return (
      <main className="pagina pagina--estreita">
        <div className="cartao vazio vazio--grande">
          <CarFront size={40} />
          <h3>Não tem nenhuma viagem a decorrer</h3>
          <p>Escolha um carro no mapa para reservar. O tempo começa a contar no momento da reserva.</p>
          <Link to="/mapa" className="btn btn--navy" style={{ marginTop: 22 }}>
            Ir para o mapa
          </Link>
        </div>
      </main>
    );
  }

  const modelo = nomeModelo(viatura?.modelo ?? '') || ativa.matricula;

  async function cancelar() {
    if (!ativa) return;
    setACancelar(true);
    setErro(null);
    try {
      const r = await api.cancelar(ativa.id);
      definirAtiva(null);
      void atualizarUtilizador();
      setModal(null);
      toast('Reserva cancelada.');
      nav(`/viagens/${r.id}`, { replace: true, state: { acabou: true } });
    } catch (e) {
      setErro((e as ApiError).message);
      setModal(null);
      void atualizarAtiva();
    } finally {
      setACancelar(false);
    }
  }

  return (
    <main className="pagina viagem">
      <section className="cartao-navy viagem__principal">
        <div className="viagem__cab">
          <div style={{ minWidth: 0 }}>
            <span className="etiqueta-sec">
              <span className="ponto-vivo" /> {emCurso ? 'Em viagem' : 'Reservado'}
            </span>
            <h1 className="viagem__titulo">
              {viatura ? nomeModelo(viatura.modelo) : modelo} | <span className="matricula">{ativa.matricula}</span>
            </h1>
          </div>
          <span className="chip-navy">{porMinuto(ativa.precoMinuto)}</span>
        </div>

        {/* Detalhes do carro reservado */}
        <ul className="viagem__ficha" aria-label="Detalhes do carro">
          <li>
            <Zap size={16} /> Elétrico
          </li>
          {viatura?.lugares ? (
            <li>
              <Users size={16} /> {viatura.lugares} lugares
            </li>
          ) : null}
          {viatura?.ano ? (
            <li>
              <CalendarDays size={16} /> {viatura.ano}
            </li>
          ) : null}
          {(ativa.kmInicial ?? viatura?.quilometragem) != null ? (
            <li>
              <Gauge size={16} /> {km(ativa.kmInicial ?? viatura?.quilometragem)}
            </li>
          ) : null}
          <li>Taxa de entrada {euro(ativa.taxaEntrada)}</li>
          <li>{porKm(ativa.precoKm)}</li>
        </ul>

        <div className="viagem__tiles">
          <div className="tile-navy viagem__tile">
            <div className="tile-navy__rot">{emCurso ? 'Tempo decorrido' : 'Tempo desde a reserva'}</div>
            <div className="viagem__grande tabular">{cronometro(segundos)}</div>
          </div>
          <div className="tile-navy viagem__tile">
            <div className="tile-navy__rot">Valor até agora</div>
            <div className={`viagem__grande viagem__grande--sky tabular${euro(valor).length > 7 ? ' viagem__grande--longo' : ''}`}>
              {euro(valor)}
            </div>
            <div className="viagem__det">
              {euro(ativa.taxaEntrada)} de entrada + {minutos} min x {porMinuto(ativa.precoMinuto)}
              <br />
              Os km somam-se no fim ({porKm(ativa.precoKm)})
            </div>
          </div>
        </div>

        {erro ? (
          <div className="alerta alerta--navy" role="alert" style={{ marginBottom: 16 }}>
            <span>{erro}</span>
          </div>
        ) : null}

        {emCurso ? (
          <Botao variante="sky" className="btn--bloco btn--lg" onClick={() => setModal('FIM')}>
            Terminar viagem
          </Botao>
        ) : (
          <div className="viagem__acoes">
            <Botao variante="sky" className="btn--lg" onClick={() => setModal('INICIO')}>
              Iniciar viagem
            </Botao>
            <Botao variante="navy-claro" className="btn--lg" onClick={() => setModal('CANCELAR')}>
              Cancelar reserva
            </Botao>
          </div>
        )}
        {!emCurso ? (
          <p className="viagem__nota">O tempo conta desde a reserva. Inicie a viagem quando estiver junto ao carro.</p>
        ) : null}
      </section>

      <aside className="viagem__lado">
        <div className="cartao cartao--pad onde">
          <h2 className="onde__titulo">{emCurso ? 'Onde começou a viagem' : 'Onde está o carro'}</h2>
          <div className="mini-mapa">
            {posCarro ? (
              <MapContainer
                center={[posCarro.lat, posCarro.lng]}
                zoom={16}
                zoomControl={false}
                attributionControl={false}
                dragging={false}
                scrollWheelZoom={false}
                doubleClickZoom={false}
                touchZoom={false}
                keyboard={false}
              >
                <Mosaico />
                <AjustarTamanho />
                <Marker position={[posCarro.lat, posCarro.lng]} icon={iconeCarro()} />
                {gpsOk && !emCurso ? <Marker position={[posicao.lat, posicao.lng]} icon={iconeEu} interactive={false} /> : null}
              </MapContainer>
            ) : null}
          </div>
          <p className="onde__morada">{posCarro ? moradaAproximada(posCarro) : 'A obter a localização...'}</p>
          {!emCurso && distCarro != null ? (
            <p className="onde__dist">
              <Footprints size={15} /> {distancia(distCarro)} | {minutosAPe(distCarro)} min a pé
            </p>
          ) : null}
          {!emCurso && posCarro ? (
            <a
              className="btn btn--claro btn--sm onde__ir"
              href={`https://www.google.com/maps/dir/?api=1&destination=${posCarro.lat},${posCarro.lng}&travelmode=walking`}
              target="_blank"
              rel="noreferrer"
            >
              <Navigation size={16} /> Como lá chegar
            </a>
          ) : null}
        </div>

        <div className="cartao-dica">
          <h2>{emCurso ? 'Antes de terminar' : 'Antes de começar'}</h2>
          {emCurso ? (
            <ul>
              <li>Estacione numa zona permitida</li>
              <li>Veja os km no painel: vai precisar deles</li>
              <li>Feche portas e janelas</li>
              <li>O valor é debitado automaticamente</li>
            </ul>
          ) : (
            <ul>
              <li>Dê uma volta ao carro e veja se tem danos</li>
              <li>Danos que já lá estavam não lhe são cobrados</li>
              <li>O tempo já está a contar desde a reserva</li>
            </ul>
          )}
        </div>
      </aside>

      {modal === 'INICIO' || modal === 'FIM' ? (
        <EstadoCarroModal
          tipo={modal}
          reserva={ativa}
          viatura={viatura}
          posicaoGps={gpsOk ? posicao : null}
          valorAteAgora={valor}
          aoFechar={() => setModal(null)}
          aoConcluir={async () => {
            if (modal === 'INICIO') {
              setModal(null);
              await atualizarAtiva();
              toast('Boa viagem! A viagem começou.');
            } else {
              const id = ativa.id;
              setModal(null);
              definirAtiva(null);
              void atualizarUtilizador();
              nav(`/viagens/${id}`, { replace: true, state: { acabou: true } });
            }
          }}
        />
      ) : null}

      {modal === 'CANCELAR' ? (
        <Modal
          titulo="Cancelar a reserva?"
          aoFechar={() => setModal(null)}
          rodape={
            <>
              <Botao variante="claro" className="btn--md" onClick={() => setModal(null)} disabled={aCancelar}>
                Manter reserva
              </Botao>
              <Botao variante="perigo" className="btn--md" onClick={cancelar} carregando={aCancelar}>
                Cancelar reserva
              </Botao>
            </>
          }
        >
          <p style={{ color: 'var(--muted)' }}>
            O carro volta a ficar livre para outros clientes. Paga a taxa de entrada e os minutos em que o carro esteve
            guardado para si (sem km, porque o carro não saiu do sítio).
          </p>
          <div className="estimativa" style={{ marginTop: 18 }}>
            <div>
              <span>Taxa de entrada</span>
              <b className="tabular">{euro(ativa.taxaEntrada)}</b>
            </div>
            <div>
              <span>
                {minutos} min x {porMinuto(ativa.precoMinuto)}
              </span>
              <b className="tabular">{euro(minutos * ativa.precoMinuto)}</b>
            </div>
            <div className="estimativa__total">
              <span>A pagar agora</span>
              <b className="tabular">{euro(valor)}</b>
            </div>
          </div>
        </Modal>
      ) : null}
    </main>
  );
}
