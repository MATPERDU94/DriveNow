import { Link } from 'react-router-dom';
import { AlertTriangle, CalendarDays, Footprints, Gauge, MapPin, Timer, Users, Zap } from 'lucide-react';
import type { Viatura } from '../api/types';
import { Botao, Modal } from './ui';
import { distancia, euro, km, minutosAPe, nomeModelo, porMinuto } from '../lib/format';
import { moradaAproximada } from '../lib/geo';

/** Minutos usados no exemplo de preço (o mesmo exemplo do documento de funcionamento). */
const MINUTOS_EXEMPLO = 30;

/**
 * Confirmação antes de reservar: o temporizador começa no momento da reserva,
 * por isso o cliente revê o carro e os preços antes de confirmar.
 */
export function ConfirmarReserva({
  viatura: v,
  meioPagamento,
  aReservar,
  erro,
  aoFechar,
  aoConfirmar,
}: {
  viatura: Viatura;
  meioPagamento: string | null;
  aReservar: boolean;
  erro: string | null;
  aoFechar: () => void;
  aoConfirmar: () => void;
}) {
  const exemplo = v.taxaEntrada + MINUTOS_EXEMPLO * v.precoMinuto;
  const local = v.latitude != null && v.longitude != null ? moradaAproximada({ lat: v.latitude, lng: v.longitude }) : null;

  return (
    <Modal
      titulo="Confirmar reserva"
      subtitulo="O temporizador começa assim que confirmar."
      aoFechar={aReservar ? () => {} : aoFechar}
      rodape={
        <>
          <Botao variante="claro" className="btn--md" onClick={aoFechar} disabled={aReservar}>
            Voltar
          </Botao>
          <Botao variante="sky" className="btn--md" onClick={aoConfirmar} carregando={aReservar}>
            Confirmar reserva
          </Botao>
        </>
      }
    >
      <div className="ficha">
        <div className="ficha__cab">
          <div style={{ minWidth: 0 }}>
            <p className="ficha__modelo">{nomeModelo(v.modelo)}</p>
            <p className="ficha__sub">
              <Zap size={15} /> Elétrico · <span className="matricula">{v.matricula}</span>
            </p>
          </div>
          <span className="chip-navy ficha__preco">{porMinuto(v.precoMinuto)}</span>
        </div>
        {local ? (
          <p className="ficha__local">
            <MapPin size={16} /> {local}
            {v.distanciaKm != null ? (
              <span className="ficha__pe">
                <Footprints size={15} /> {distancia(v.distanciaKm)} · {minutosAPe(v.distanciaKm)} min a pé
              </span>
            ) : null}
          </p>
        ) : null}
      </div>

      <dl className="detalhes">
        <div>
          <dt>
            <Users size={16} /> Lugares
          </dt>
          <dd>{v.lugares ?? '—'}</dd>
        </div>
        <div>
          <dt>
            <CalendarDays size={16} /> Ano
          </dt>
          <dd>{v.ano ?? '—'}</dd>
        </div>
        <div>
          <dt>
            <Gauge size={16} /> Quilómetros
          </dt>
          <dd>{km(v.quilometragem)}</dd>
        </div>
        <div>
          <dt>
            <Zap size={16} /> Motor
          </dt>
          <dd>Elétrico</dd>
        </div>
      </dl>

      <div className="estimativa">
        <div>
          <span>Taxa de entrada (paga uma vez)</span>
          <b className="tabular">{euro(v.taxaEntrada)}</b>
        </div>
        <div>
          <span>Preço por minuto</span>
          <b className="tabular">{porMinuto(v.precoMinuto)}</b>
        </div>
        <div className="estimativa__total">
          <span>Exemplo: viagem de {MINUTOS_EXEMPLO} min</span>
          <b className="tabular">{euro(exemplo)}</b>
        </div>
        <p>
          Preços com IVA incluído. Pago no fim da viagem{meioPagamento ? ` com ${meioPagamento}` : ''}.
        </p>
      </div>

      <div className="alerta alerta--info" style={{ marginTop: 16 }}>
        <Timer size={18} />
        <span>
          Ao confirmar, o carro fica guardado para si e <strong>o tempo começa a contar logo</strong>, mesmo antes de iniciar a
          viagem. Se cancelar antes de começar, paga a taxa de entrada e os minutos até cancelar.
        </span>
      </div>

      {erro ? (
        <div className="alerta alerta--erro" role="alert" style={{ marginTop: 14 }}>
          <AlertTriangle size={18} />
          <span>
            {erro}
            {/[.!?]$/.test(erro) ? '' : '.'}
            {/dívida|cartão|conta bancária/i.test(erro) ? (
              <>
                {' '}
                <Link to="/perfil">Ir para o perfil</Link>
              </>
            ) : null}
          </span>
        </div>
      ) : null}
    </Modal>
  );
}
