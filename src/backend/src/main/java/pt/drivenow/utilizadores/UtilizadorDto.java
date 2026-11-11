package pt.drivenow.utilizadores;

import java.math.BigDecimal;
import java.time.Instant;

import pt.drivenow.pagamentos.MeioPagamentoDto;

/** Dados do utilizador para a app (ecrã Perfil). Nunca inclui a password. */
public record UtilizadorDto(
        Long id,
        String nome,
        String email,
        String nif,
        String telefone,
        Instant criadoEm,
        MeioPagamentoDto meioPagamento,
        BigDecimal valorEmDivida) {

    public static UtilizadorDto de(Utilizador u, MeioPagamentoDto meio, BigDecimal divida) {
        return new UtilizadorDto(u.getId(), u.getNome(), u.getEmail(), u.getNif(), u.getTelefone(),
                u.getCriadoEm(), meio, divida);
    }
}
