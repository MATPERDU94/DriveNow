package pt.drivenow.frota;

/**
 * Uma viatura da frota: dados e preços do Odoo + posição GPS e disponibilidade (backend).
 *
 * @param taxaEntrada valor cobrado ao reservar (€)
 * @param precoMinuto preço por minuto de viagem (€)
 * @param disponivel  false se a viatura está numa reserva ativa ou em reparação
 * @param motivoIndisponibilidade "Reservada" ou "Em reparação" (null se disponível)
 * @param distanciaKm distância ao ponto pedido (só quando a pesquisa indica lat/lng)
 */
public record ViaturaDto(
        Long id,
        String matricula,
        String modelo,
        Integer ano,
        Integer lugares,
        Double quilometragem,
        String estado,
        Double taxaEntrada,
        Double precoMinuto,
        Double latitude,
        Double longitude,
        boolean disponivel,
        String motivoIndisponibilidade,
        Double distanciaKm) {

    ViaturaDto comLocalizacao(Double lat, Double lng, String motivoIndisponibilidade, Double distanciaKm) {
        return new ViaturaDto(id, matricula, modelo, ano, lugares, quilometragem, estado,
                taxaEntrada, precoMinuto, lat, lng, motivoIndisponibilidade == null, motivoIndisponibilidade,
                distanciaKm);
    }
}
