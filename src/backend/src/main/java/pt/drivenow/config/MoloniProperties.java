package pt.drivenow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ligação ao Moloni (application.yml, secção "moloni").
 *
 * @param companyId        id da empresa no Moloni (vazio: a primeira da conta)
 * @param fecharDocumentos false: faturas em rascunho (sem valor fiscal, sem PDF); true: finalizadas, com PDF
 * @param iva              taxa de IVA (%) incluída nos preços do tarifário
 */
@ConfigurationProperties(prefix = "moloni")
public record MoloniProperties(
        String clientId,
        String clientSecret,
        String username,
        String password,
        Long companyId,
        boolean fecharDocumentos,
        double iva) {
}
