import { useEffect, useState } from 'react';

export interface Ponto {
  lat: number;
  lng: number;
}

/** Centro de Guimarães (zona de serviço da DriveNow). */
export const GUIMARAES: Ponto = { lat: 41.4425, lng: -8.2918 };

/** Se o utilizador estiver mais longe do que isto, a app mostra Guimarães. */
export const RAIO_ZONA_KM = 12;

/** Locais de Guimarães (os mesmos do backend, PosicoesIniciais.java), usados na pesquisa e nas moradas. */
export const LOCAIS: { nome: string; lat: number; lng: number }[] = [
  { nome: 'Largo do Toural', lat: 41.4417, lng: -8.2955 },
  { nome: 'Largo da Oliveira', lat: 41.4434, lng: -8.2938 },
  { nome: 'Castelo de Guimarães', lat: 41.448, lng: -8.2905 },
  { nome: 'Paço dos Duques', lat: 41.4466, lng: -8.2906 },
  { nome: 'Largo da Mumadona', lat: 41.4458, lng: -8.2922 },
  { nome: 'Estação de Guimarães', lat: 41.4378, lng: -8.2934 },
  { nome: 'Universidade do Minho (Azurém)', lat: 41.4525, lng: -8.2898 },
  { nome: 'Estádio D. Afonso Henriques', lat: 41.4459, lng: -8.3012 },
  { nome: 'Teleférico da Penha', lat: 41.4436, lng: -8.2868 },
  { nome: 'Centro Cultural Vila Flor', lat: 41.4389, lng: -8.2958 },
  { nome: 'Plataforma das Artes', lat: 41.4423, lng: -8.2988 },
  { nome: 'Mercado Municipal', lat: 41.4447, lng: -8.2985 },
  { nome: 'Parque da Cidade', lat: 41.4368, lng: -8.305 },
  { nome: 'GuimarãeShopping', lat: 41.45, lng: -8.299 },
  { nome: 'Espaço Guimarães', lat: 41.457, lng: -8.311 },
  { nome: 'Rua de Santa Maria', lat: 41.445, lng: -8.2937 },
  { nome: 'Igreja de São Francisco', lat: 41.4426, lng: -8.2916 },
  { nome: 'Multiusos de Guimarães', lat: 41.44, lng: -8.308 },
  { nome: 'Hospital Senhora da Oliveira', lat: 41.435, lng: -8.302 },
  { nome: 'Avenida de Londres', lat: 41.443, lng: -8.303 },
  { nome: 'Avenida D. Afonso Henriques', lat: 41.4405, lng: -8.2942 },
  { nome: 'Azurém', lat: 41.4535, lng: -8.285 },
  { nome: 'Creixomil', lat: 41.433, lng: -8.3085 },
  { nome: 'Urgezes', lat: 41.431, lng: -8.295 },
  { nome: 'Mesão Frio', lat: 41.4425, lng: -8.272 },
  { nome: 'Costa', lat: 41.4455, lng: -8.2775 },
  { nome: 'Santuário da Penha', lat: 41.432, lng: -8.265 },
  { nome: 'Silvares', lat: 41.449, lng: -8.317 },
  { nome: 'Fermentões', lat: 41.46, lng: -8.305 },
  { nome: 'Polvoreira', lat: 41.428, lng: -8.32 },
];

/** Distância em km (Haversine), igual ao LocalizacaoService do backend. */
export function distanciaKm(a: Ponto, b: Ponto): number {
  const R = 6371;
  const dLat = ((b.lat - a.lat) * Math.PI) / 180;
  const dLng = ((b.lng - a.lng) * Math.PI) / 180;
  const h =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((a.lat * Math.PI) / 180) * Math.cos((b.lat * Math.PI) / 180) * Math.sin(dLng / 2) ** 2;
  return 2 * R * Math.asin(Math.sqrt(h));
}

/** "Perto de Largo do Toural · Guimarães" */
export function moradaAproximada(p: Ponto): string {
  let melhor = LOCAIS[0];
  let dMelhor = Infinity;
  for (const l of LOCAIS) {
    const d = distanciaKm(p, l);
    if (d < dMelhor) {
      dMelhor = d;
      melhor = l;
    }
  }
  if (dMelhor > 3) return `${p.lat.toFixed(4)}, ${p.lng.toFixed(4)}`;
  return dMelhor < 0.08 ? `${melhor.nome} · Guimarães` : `Perto de ${melhor.nome} · Guimarães`;
}

export type EstadoGps = 'a-procurar' | 'ok' | 'fora-da-zona' | 'sem-permissao' | 'indisponivel';

/**
 * Posição do utilizador (GPS do browser). Se não houver permissão ou o utilizador estiver
 * fora de Guimarães, devolve o centro de Guimarães e indica o motivo.
 */
export function usePosicao() {
  const [estado, setEstado] = useState<EstadoGps>('a-procurar');
  const [posicao, setPosicao] = useState<Ponto>(GUIMARAES);
  const [real, setReal] = useState<Ponto | null>(null);

  useEffect(() => {
    if (!('geolocation' in navigator)) {
      setEstado('indisponivel');
      return;
    }
    let ativo = true;
    const id = navigator.geolocation.watchPosition(
      (p) => {
        if (!ativo) return;
        const pt = { lat: p.coords.latitude, lng: p.coords.longitude };
        setReal(pt);
        if (distanciaKm(pt, GUIMARAES) > RAIO_ZONA_KM) {
          setEstado('fora-da-zona');
          setPosicao(GUIMARAES);
        } else {
          setEstado('ok');
          setPosicao(pt);
        }
      },
      (e) => {
        if (!ativo) return;
        setEstado(e.code === e.PERMISSION_DENIED ? 'sem-permissao' : 'indisponivel');
      },
      { enableHighAccuracy: true, maximumAge: 15000, timeout: 10000 },
    );
    return () => {
      ativo = false;
      navigator.geolocation.clearWatch(id);
    };
  }, []);

  return { posicao, estado, real };
}
