import { BrowserRouter, HashRouter, MemoryRouter, Navigate, Route, Routes } from 'react-router-dom';
import { SessaoProvider } from './state/sessao';
import { ToastProvider } from './state/toast';
import { AreaCliente, SoSemSessao } from './components/Layout';
import { FaixaDemo } from './components/FaixaDemo';
import { MODO_DEMO, MODO_FICHEIRO } from './api/client';
import Entrar from './pages/Entrar';
import CriarConta from './pages/CriarConta';
import Mapa from './pages/Mapa';
import Viagem from './pages/Viagem';
import Resumo from './pages/Resumo';
import Viagens from './pages/Viagens';
import Perfil from './pages/Perfil';

// Modo demonstração: HashRouter, para funcionar em qualquer servidor estático.
// Versão num só ficheiro: MemoryRouter (as rotas não aparecem no endereço).
const Router = MODO_FICHEIRO ? MemoryRouter : MODO_DEMO ? HashRouter : BrowserRouter;

export default function App() {
  return (
    <Router>
      <ToastProvider>
        <SessaoProvider>
          <Routes>
            <Route element={<SoSemSessao />}>
              <Route path="/entrar" element={<Entrar />} />
              <Route path="/criar-conta" element={<CriarConta />} />
            </Route>
            <Route element={<AreaCliente />}>
              <Route path="/mapa" element={<Mapa />} />
              <Route path="/viagem" element={<Viagem />} />
              <Route path="/viagens" element={<Viagens />} />
              <Route path="/viagens/:id" element={<Resumo />} />
              <Route path="/perfil" element={<Perfil />} />
            </Route>
            <Route path="*" element={<Navigate to="/mapa" replace />} />
          </Routes>
          {MODO_DEMO ? <FaixaDemo /> : null}
        </SessaoProvider>
      </ToastProvider>
    </Router>
  );
}
