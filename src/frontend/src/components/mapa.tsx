import { useEffect, useRef, useState } from 'react';
import L from 'leaflet';
import { TileLayer, useMap, useMapEvents } from 'react-leaflet';
import type { Ponto } from '../lib/geo';

// Mapa do OpenStreetMap. Pode ser trocado com VITE_TILES_URL.
const TILES =
  (import.meta.env.VITE_TILES_URL as string | undefined) ||
  'https://tile.openstreetmap.org/{z}/{x}/{y}.png';

// Se o servidor de mapas não responder (sem internet, rede bloqueada), passa-se a um mapa ilustrativo
let mosaicoFalhou = false;

export function Mosaico() {
  const [falhou, setFalhou] = useState(mosaicoFalhou);
  if (falhou) return <MosaicoIlustrativo />;
  return (
    <TileLayer
      url={TILES}
      subdomains="abcd"
      maxZoom={19}
      attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
      eventHandlers={{
        tileerror: () => {
          mosaicoFalhou = true;
          setFalhou(true);
        },
      }}
    />
  );
}

// ----- Mapa ilustrativo (desenhado no browser, sem rede) -----

function hash(a: number, b: number, c: number) {
  let h = Math.imul(a, 374761393) ^ Math.imul(b, 668265263) ^ Math.imul(c, 2246822519 | 0);
  h = Math.imul(h ^ (h >>> 13), 1274126177);
  return (h ^ (h >>> 16)) >>> 0;
}

const ESPACO = 150; // distância média entre ruas, em píxeis do mapa

/** Posição da rua i (em píxeis do mundo, no nível de zoom z), com algum desvio para não parecer uma grelha. */
const rua = (i: number, eixo: number, z: number) => i * ESPACO + (hash(i, eixo, z) % 70);

function desenharMosaico(ctx: CanvasRenderingContext2D, x: number, y: number, z: number) {
  const x0 = x * 256;
  const y0 = y * 256;
  ctx.fillStyle = '#eceff3';
  ctx.fillRect(0, 0, 256, 256);

  const iMin = Math.floor((x0 - ESPACO) / ESPACO);
  const iMax = Math.ceil((x0 + 256 + ESPACO) / ESPACO);
  const jMin = Math.floor((y0 - ESPACO) / ESPACO);
  const jMax = Math.ceil((y0 + 256 + ESPACO) / ESPACO);

  // quarteirões: alguns são jardins
  for (let i = iMin; i < iMax; i++) {
    for (let j = jMin; j < jMax; j++) {
      const h = hash(i, j, z + 7);
      if (h % 9 === 0 || h % 23 === 5) {
        const ax = rua(i, 1, z) - x0;
        const bx = rua(i + 1, 1, z) - x0;
        const ay = rua(j, 2, z) - y0;
        const by = rua(j + 1, 2, z) - y0;
        ctx.fillStyle = h % 9 === 0 ? '#d9ead3' : '#e3e7ed';
        ctx.fillRect(ax + 8, ay + 8, bx - ax - 16, by - ay - 16);
      }
    }
  }

  // ruas
  ctx.fillStyle = '#ffffff';
  for (let i = iMin; i <= iMax; i++) {
    const largura = hash(i, 3, z) % 4 === 0 ? 10 : 5;
    ctx.fillRect(rua(i, 1, z) - x0 - largura / 2, 0, largura, 256);
  }
  for (let j = jMin; j <= jMax; j++) {
    const largura = hash(j, 4, z) % 4 === 0 ? 10 : 5;
    ctx.fillRect(0, rua(j, 2, z) - y0 - largura / 2, 256, largura);
  }
}

function MosaicoIlustrativo() {
  const map = useMap();
  useEffect(() => {
    const Camada = L.GridLayer.extend({
      createTile(coords: L.Coords) {
        const c = document.createElement('canvas');
        c.width = 256;
        c.height = 256;
        const ctx = c.getContext('2d');
        if (ctx) desenharMosaico(ctx, coords.x, coords.y, coords.z);
        return c;
      },
    }) as unknown as new (opcoes?: L.GridLayerOptions) => L.GridLayer;
    const camada = new Camada({ attribution: 'Mapa ilustrativo (sem ligação ao servidor de mapas)' });
    camada.addTo(map);
    return () => {
      camada.remove();
    };
  }, [map]);
  return null;
}

const CARRO_SVG =
  '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M5 17h-1a1 1 0 0 1-1-1v-4.5l2.2-5.1A2 2 0 0 1 7 5h10a2 2 0 0 1 1.8 1.4L21 11.5V16a1 1 0 0 1-1 1h-1"/><path d="M3 11.5h18"/><circle cx="7.5" cy="17" r="1.8"/><circle cx="16.5" cy="17" r="1.8"/></svg>';

const CARRO_GRANDE_SVG = CARRO_SVG.replace('width="18" height="18"', 'width="22" height="22"');

export function iconePreco(texto: string, selecionado: boolean) {
  return L.divIcon({
    className: `marcador-preco${selecionado ? ' marcador-preco--sel' : ''}`,
    html: `<div class="preco-pin${selecionado ? ' preco-pin--sel' : ''}">${CARRO_SVG}<span>${texto}</span></div>`,
    iconSize: [0, 0],
  });
}

export function iconePonto() {
  return L.divIcon({
    className: 'marcador-preco',
    html: `<div class="preco-ponto">${CARRO_SVG.replace('width="18" height="18"', 'width="15" height="15"')}</div>`,
    iconSize: [0, 0],
  });
}

export const iconeEu = L.divIcon({ className: 'marcador-preco', html: '<div class="eu-pin"></div>', iconSize: [0, 0] });

export function iconeCarro(claro = false) {
  return L.divIcon({
    className: 'marcador-preco',
    html: `<div class="carro-pin${claro ? ' carro-pin--sky' : ''}">${CARRO_GRANDE_SVG}</div>`,
    iconSize: [0, 0],
  });
}

/** Move o mapa sempre que recebe um novo objeto `centro` (cada pedido de centrar é um objeto novo). */
export function Centrar({ centro, zoom }: { centro: Ponto; zoom?: number }) {
  const map = useMap();
  const primeiro = useRef(true);
  useEffect(() => {
    if (primeiro.current) {
      primeiro.current = false;
      return;
    }
    map.flyTo([centro.lat, centro.lng], zoom ?? Math.max(map.getZoom(), 15), { duration: 0.6 });
  }, [centro, zoom, map]);
  return null;
}

/** Ajusta o tamanho do mapa quando o contentor muda (ex.: painel lateral, ecrã pequeno). */
export function AjustarTamanho() {
  const map = useMap();
  useEffect(() => {
    const el = map.getContainer();
    const ro = new ResizeObserver(() => map.invalidateSize());
    ro.observe(el);
    return () => ro.disconnect();
  }, [map]);
  return null;
}

export function AoClicar({ fn }: { fn: (p: Ponto) => void }) {
  useMapEvents({ click: (e) => fn({ lat: e.latlng.lat, lng: e.latlng.lng }) });
  return null;
}
