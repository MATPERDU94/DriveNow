package pt.drivenow.pagamentos;

import java.math.BigDecimal;
import java.time.Instant;

public record CobrancaDto(
        Long id,
        Long reservaId,
        TipoCobranca tipo,
        BigDecimal valor,
        EstadoCobranca estado,
        String meio,
        String motivoRecusa,
        int tentativas,
        Instant criadaEm,
        Instant pagaEm) {

    public static CobrancaDto de(Cobranca c) {
        return new CobrancaDto(c.getId(), c.getReservaId(), c.getTipo(), c.getValor(), c.getEstado(),
                c.getMeioDescricao(), c.getMotivoRecusa(), c.getTentativas(), c.getCriadaEm(), c.getPagaEm());
    }
}
