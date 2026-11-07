package pt.drivenow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ligação ao Odoo (application.yml, secção "odoo").
 *
 * @param url      endereço do Odoo, ex.: http://localhost:8069
 * @param database nome da base de dados do Odoo
 * @param apiKey   chave de API de um utilizador do Odoo (Preferências → Segurança da conta)
 */
@ConfigurationProperties(prefix = "odoo")
public record OdooProperties(String url, String database, String apiKey) {
}
