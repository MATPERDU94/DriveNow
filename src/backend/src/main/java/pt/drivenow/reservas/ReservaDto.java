package pt.drivenow.reservas;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * @param minutosDecorridos minutos desde a reserva (atualizado ao momento enquanto a reserva está ativa)
 * @param valorAteAgora     taxa de entrada + minutos × preço por minuto até agora (ecrã "Em viagem")
 */
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
        BigDecimal total,
        Long minutosDecorridos,
        BigDecimal valorAteAgora,
        EstadoFatura estadoFatura,
        Long moloniDocumentoId,
        String faturaPdfUrl,
        String faturaErro) {

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
                r.getTotal(),
                r.ativa() ? Long.valueOf(r.minutosAteAgora()) : r.getMinutos(),
                r.ativa() ? r.valorAteAgora() : r.getTotal(),
                r.getEstadoFatura(),
                r.getMoloniDocumentoId(),
                r.getFaturaPdfUrl(),
                r.getFaturaErro());
    }
}
