package pt.drivenow.frota;

/**
 * Uma viatura da frota: dados do Odoo + posição GPS e disponibilidade (backend).
 *
 * @param disponivel  false se a viatura está numa reserva ativa
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
        Double latitude,
        Double longitude,
        boolean disponivel,
        Double distanciaKm) {

    ViaturaDto comLocalizacao(Double lat, Double lng, boolean disponivel, Double distanciaKm) {
        return new ViaturaDto(id, matricula, modelo, ano, lugares, quilometragem, estado,
                lat, lng, disponivel, distanciaKm);
    }
}
