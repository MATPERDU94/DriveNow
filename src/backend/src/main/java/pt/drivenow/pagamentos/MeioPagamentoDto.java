package pt.drivenow.pagamentos;

/** O que a app mostra do meio de pagamento (ecrã Perfil): nunca o número completo. */
public record MeioPagamentoDto(
        TipoMeioPagamento tipo,
        String titular,
        String marca,
        String ultimosDigitos,
        String validade,
        String descricao,
        boolean expirado) {

    public static MeioPagamentoDto de(MeioPagamento m) {
        return m == null ? null : new MeioPagamentoDto(m.getTipo(), m.getTitular(), m.getMarca(),
                m.getUltimosDigitos(), m.getValidade(), m.descricao(), m.expirado());
    }
}
