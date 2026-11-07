package pt.drivenow.reservas;

import java.math.BigDecimal;
import java.time.Instant;

public record ReservaDto(
        Long id,
        Long utilizadorId,
        String utilizadorNome,
        Long viaturaId,
        String matricula,
        EstadoReserva estado,
        Instant criadaEm,
        Instant inicioViagem,
        Instant fimViagem,
        BigDecimal taxaEntrada,
        BigDecimal precoMinuto,
        Long minutos,
        BigDecimal valorViagem,
        BigDecimal totalPenalizacoes,
        BigDecimal total) {

    public static ReservaDto de(Reserva r) {
        return new ReservaDto(
                r.getId(),
                r.getUtilizador().getId(),
                r.getUtilizador().getNome(),
                r.getViaturaOdooId(),
                r.getMatricula(),
                r.getEstado(),
                r.getCriadaEm(),
                r.getInicioViagem(),
                r.getFimViagem(),
                r.getTaxaEntrada(),
                r.getPrecoMinuto(),
                r.getMinutos(),
                r.getValorViagem(),
                r.getTotalPenalizacoes(),
                r.getTotal());
    }
}
