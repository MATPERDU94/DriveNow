import { useEffect, useMemo, useState } from 'react';
import { MapContainer, Marker, Polyline } from 'react-leaflet';
import { Route } from 'lucide-react';
import { api } from '../api/client';
import type { Trajeto } from '../api/types';
import { AjustarTamanho, Enquadrar, Mosaico, iconeCarro, iconeInicio } from './mapa';
import { duracao, kmViagem } from '../lib/format';
import { moradaAproximada } from '../lib/geo';

/**
 * Trajeto da viagem (diagrama 2b): percurso pelas ruas entre onde o carro estava na reserva
 * e onde o cliente o deixou, calculado pelo serviço de rotas no fim da viagem.
 */
export function TrajetoViagem({ reservaId, kmPainel }: { reservaId: number; kmPainel: number | null }) {
  // undefined = a carregar; null = sem trajeto (ex.: reserva cancelada)
  const [t, setT] = useState<Trajeto | null | undefined>(undefined);

  useEffect(() => {
    let ativo = true;
    api
      .trajeto(reservaId)
      .then((x) => ativo && setT(x))
      .catch(() => ativo && setT(null));
    return () => {
      ativo = false;
    };
  }, [reservaId]);

  const linha = useMemo(() => (t ? t.pontos.map(([lat, lng]) => [lat, lng] as [number, number]) : []), [t]);
  const enquadrar = useMemo(
    () => (t ? [...linha, [t.origem.lat, t.origem.lng] as [number, number], [t.destino.lat, t.destino.lng] as [number, number]] : []),
    [t, linha],
  );

  if (t === null) return null;
  const reta = t?.tipo === 'LINHA_RETA';

  return (
    <section className="cartao cartao--pad resumo__bloco trajeto">
      <div className="trajeto__cab">
        <h2 className="cartao__titulo trajeto__titulo">
          <Route size={20} /> Trajeto
        </h2>
        {t ? (
          <span className={`chip${reta ? ' chip--cinza' : ''}`}>
            {reta ? `${kmViagem(t.distanciaKm)} em linha reta` : `${kmViagem(t.distanciaKm)} pelas ruas`}
          </span>
        ) : null}
      </div>

      <div className="mini-mapa mini-mapa--trajeto">
        {t ? (
          <MapContainer
            center={[t.origem.lat, t.origem.lng]}
            zoom={15}
            zoomControl={false}
            dragging={false}
            scrollWheelZoom={false}
            doubleClickZoom={false}
            touchZoom={false}
            keyboard={false}
          >
            <Mosaico />
            <AjustarTamanho />
            <Enquadrar pontos={enquadrar} />
            {linha.length > 1 ? (
              <>
                {/* A classe tem de ir como propriedade: o react-leaflet só a aplica ao criar a linha */}
                <Polyline positions={linha} className="trajeto__contorno" interactive={false} />
                <Polyline
                  positions={linha}
                  className={`trajeto__linha${reta ? ' trajeto__linha--reta' : ''}`}
                  interactive={false}
                />
              </>
            ) : null}
            <Marker position={[t.origem.lat, t.origem.lng]} icon={iconeInicio} interactive={false} />
            <Marker position={[t.destino.lat, t.destino.lng]} icon={iconeCarro()} interactive={false} />
          </MapContainer>
        ) : null}
      </div>

      {t ? (
        <>
          <ol className="trajeto__pontos">
            <li>
              <span className="trajeto__marca" />
              <div>
                <small>Início · onde estava o carro</small>
                <b>{moradaAproximada(t.origem)}</b>
              </div>
            </li>
            <li>
              <span className="trajeto__marca trajeto__marca--fim" />
              <div>
                <small>Fim · onde deixou o carro</small>
                <b>{moradaAproximada(t.destino)}</b>
              </div>
            </li>
          </ol>
          <p className="trajeto__nota">
            {reta
              ? 'O serviço de rotas não respondeu: mostra-se a linha reta entre o início e o fim.'
              : `Percurso mais direto pelas ruas: ${kmViagem(t.distanciaKm)}${
                  t.duracaoMin ? `, cerca de ${duracao(Math.max(1, t.duracaoMin))} de condução` : ''
                }.`}{' '}
            É um trajeto simulado (o carro não é seguido por GPS durante a viagem); os km cobrados são os do painel
            {kmPainel != null ? ` (${kmViagem(kmPainel)})` : ''}.
          </p>
          {t.fonte ? <p className="trajeto__fonte">Rotas: {t.fonte}</p> : null}
        </>
      ) : (
        <p className="trajeto__nota">A carregar o trajeto…</p>
      )}
    </section>
  );
}
