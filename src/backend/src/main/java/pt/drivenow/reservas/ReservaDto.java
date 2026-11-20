package pt.drivenow.reservas;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * @param minutosDecorridos minutos desde a reserva (atualizado ao momento enquanto a reserva está ativa)
 * @param valorAteAgora     taxa de entrada + minutos × preço por minuto até agora (ecrã "Em viagem");
 *                          os km só se somam no fim da viagem
 * @param kmInicial         km do carro no momento da reserva
 * @param kmFinal           km do painel indicados no fim da viagem
 * @param kmViagem          km finais - km iniciais (0 se a reserva foi cancelada)
 * @param valorKm           km da viagem × preço por km
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
        BigDecimal precoKm,
        Long minutos,
        BigDecimal valorViagem,
        Double kmInicial,
        Double kmFinal,
        Double kmViagem,
        BigDecimal valorKm,
        BigDecimal totalPenalizacoes,
        BigDecimal total,
        Long minutosDecorridos,
        BigDecimal valorAteAgora,
        EstadoPagamento estadoPagamento,
        String pagamentoDescricao,
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
                r.getPrecoKm(),
                r.getMinutos(),
                r.getValorViagem(),
                r.getKmInicial(),
                r.getKmFinal(),
                r.getKmViagem(),
                r.getValorKm(),
                r.getTotalPenalizacoes(),
                r.getTotal(),
                r.ativa() ? Long.valueOf(r.minutosAteAgora()) : r.getMinutos(),
                r.ativa() ? r.valorAteAgora() : r.getTotal(),
                r.getEstadoPagamento(),
                r.getPagamentoDescricao(),
                r.getEstadoFatura(),
                r.getMoloniDocumentoId(),
                r.getFaturaPdfUrl(),
                r.getFaturaErro());
    }
}
