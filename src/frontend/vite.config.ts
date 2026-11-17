import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import { viteSingleFile } from 'vite-plugin-singlefile';

// npm run dev  -> usa o backend real (proxy de /api para API_URL, por omissão http://localhost:8080)
// npm run demo -> modo demonstração: a API é simulada no browser, não precisa de backend
// npm run build:ficheiro -> modo demonstração num só ficheiro HTML (dist-ficheiro/index.html)
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const alvo = env.API_URL || 'http://localhost:8080';
  const proxy =
    mode === 'demo' || mode === 'ficheiro'
      ? undefined
      : { '/api': { target: alvo, changeOrigin: true } };

  return {
    plugins: [react(), ...(mode === 'ficheiro' ? [viteSingleFile()] : [])],
    base: mode === 'demo' || mode === 'ficheiro' ? './' : '/',
    server: { port: 5173, host: true, proxy },
    preview: { port: 4173, host: true, proxy },
  };
});
