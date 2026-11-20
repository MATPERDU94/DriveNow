package pt.drivenow.trajetos;

import java.util.List;

/**
 * Trajeto de uma viagem para o ecrã "Viagem terminada".
 *
 * @param tipo        RUAS (percurso do serviço de rotas) ou LINHA_RETA (o serviço não respondeu)
 * @param origem      onde estava o carro quando foi reservado
 * @param destino     onde o cliente deixou o carro
 * @param pontos      pontos do percurso, cada um [latitude, longitude]
 * @param distanciaKm distância pelas ruas (ou em linha reta)
 * @param duracaoMin  tempo de condução estimado pelo serviço de rotas (null em linha reta)
 * @param fonte       de onde veio o percurso, para mostrar na app
 */
public record TrajetoDto(
        String tipo,
        Ponto origem,
        Ponto destino,
        List<double[]> pontos,
        Double distanciaKm,
        Double duracaoMin,
        String fonte) {

    public record Ponto(double lat, double lng) {
    }
}
