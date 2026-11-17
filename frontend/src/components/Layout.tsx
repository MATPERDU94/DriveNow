import { NavLink, Navigate, Outlet, useLocation } from 'react-router-dom';
import { Map as IconeMapa, ReceiptText, UserRound } from 'lucide-react';
import { useSessao } from '../state/sessao';
import { Carregar, Marca } from './ui';
import { cronometro, iniciais } from '../lib/format';
import { useSegundosDesde } from '../lib/hooks';
import type { Reserva } from '../api/types';

function PilulaViagem({ r }: { r: Reserva }) {
  const s = useSegundosDesde(r.criadaEm);
  return (
    <NavLink to="/viagem" className="pilula-viagem" title="Ver a viagem">
      <span className="ponto-vivo" />
      {r.estado === 'EM_CURSO' ? 'Em viagem' : 'Reservado'}
      <strong>{cronometro(s)}</strong>
    </NavLink>
  );
}

export function Topo() {
  const { utilizador, ativa } = useSessao();
  const { pathname } = useLocation();
  return (
    <header className="topo">
      <Marca para="/mapa" />
      <div style={{ display: 'flex', alignItems: 'center' }}>
        {ativa && pathname !== '/viagem' ? <PilulaViagem r={ativa} /> : null}
        <nav className="topo__nav" aria-label="Principal">
          <NavLink to="/mapa" className="topo__link">
            <IconeMapa size={22} />
            Mapa
          </NavLink>
          <NavLink to="/viagens" className="topo__link">
            <ReceiptText size={22} />
            Viagens e faturas
          </NavLink>
          <NavLink to="/perfil" className="topo__link">
            <UserRound size={22} />
            Perfil
          </NavLink>
          <NavLink to="/perfil" className="avatar" aria-label="Perfil" title={utilizador?.nome}>
            {iniciais(utilizador?.nome)}
          </NavLink>
        </nav>
      </div>
    </header>
  );
}

/** Páginas que exigem sessão: cabeçalho + conteúdo. */
export function AreaCliente() {
  const { utilizador } = useSessao();
  const loc = useLocation();
  if (utilizador === undefined) return <Carregar />;
  if (utilizador === null) return <Navigate to="/entrar" replace state={{ de: loc.pathname }} />;
  return (
    <>
      <Topo />
      <Outlet />
    </>
  );
}

/** Páginas públicas (entrar, criar conta): quem já tem sessão vai para o mapa. */
export function SoSemSessao() {
  const { utilizador } = useSessao();
  if (utilizador === undefined) return <Carregar />;
  if (utilizador) return <Navigate to="/mapa" replace />;
  return <Outlet />;
}
