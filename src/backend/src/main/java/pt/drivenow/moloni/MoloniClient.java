package pt.drivenow.moloni;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import pt.drivenow.config.HttpTimeouts;
import pt.drivenow.config.MoloniProperties;

/**
 * Cliente da API do Moloni (https://api.moloni.pt/v1).
 *
 * Autenticação OAuth 2.0 com grant_type=password: o token dura 1 hora e é pedido de novo
 * automaticamente quando expira. Cada chamada é um POST para /{endpoint}/ com o access_token
 * no endereço e os parâmetros em JSON (json=true).
 */
@Component
public class MoloniClient {

    private static final String BASE_URL = "https://api.moloni.pt/v1";

    private final RestClient http;
    private final ObjectMapper json;
    private final MoloniProperties props;

    private String accessToken;
    private Instant expiraEm = Instant.EPOCH;

    public MoloniClient(MoloniProperties props, RestClient.Builder builder, ObjectMapper json) {
        this.props = props;
        this.json = json;
        this.http = builder.requestFactory(HttpTimeouts.fabrica()).baseUrl(BASE_URL).build();
    }

    public MoloniProperties config() {
        return props;
    }

    /**
     * Chama um endpoint do Moloni, ex.: call("customers/getByVat", Map.of("company_id", 1, "vat", "...")).
     */
    public JsonNode call(String endpoint, Map<String, Object> params) {
        byte[] body;
        try {
            body = json.writeValueAsBytes(params == null ? Map.of() : params);
        } catch (JsonProcessingException e) {
            throw new MoloniException("Não foi possível converter o pedido para JSON", e);
        }
        String token = token();
        String resposta;
        try {
            resposta = http.post()
                    .uri(u -> u.path("/" + endpoint + "/")
                            .queryParam("access_token", token)
                            .queryParam("json", "true")
                            .queryParam("human_errors", "true")
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .contentLength(body.length)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            throw new MoloniException("Moloni " + endpoint + " falhou (HTTP " + e.getStatusCode().value()
                    + "): " + e.getResponseBodyAsString(), e);
        } catch (ResourceAccessException e) {
            throw new MoloniException("Não foi possível contactar o Moloni", e);
        }
        JsonNode node = ler(resposta);
        if (node.isObject() && node.has("error")) {
            throw new MoloniException("Moloni " + endpoint + ": " + node.path("error_description").asText(node.toString()));
        }
        return node;
    }

    /** Para inserts: o Moloni devolve {"valid": 1, ...}; caso contrário, a resposta são os erros. */
    public JsonNode insert(String endpoint, Map<String, Object> params) {
        JsonNode r = call(endpoint, params);
        if (!r.isObject() || r.path("valid").asInt(0) != 1) {
            throw new MoloniException("Moloni " + endpoint + " recusou o pedido: " + r);
        }
        return r;
    }

    private synchronized String token() {
        if (accessToken != null && Instant.now().isBefore(expiraEm.minusSeconds(60))) {
            return accessToken;
        }
        if (vazio(props.clientId()) || vazio(props.clientSecret()) || vazio(props.username()) || vazio(props.password())) {
            throw new MoloniException("Faltam as credenciais do Moloni no .env (MOLONI_CLIENT_ID, MOLONI_CLIENT_SECRET, "
                    + "MOLONI_USERNAME, MOLONI_PASSWORD)");
        }
        String resposta;
        try {
            resposta = http.get()
                    .uri(u -> u.path("/grant/")
                            .queryParam("grant_type", "password")
                            .queryParam("client_id", "{cid}")
                            .queryParam("client_secret", "{secret}")
                            .queryParam("username", "{user}")
                            .queryParam("password", "{pass}")
                            .build(props.clientId(), props.clientSecret(), props.username(), props.password()))
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            throw new MoloniException("Login no Moloni falhou (HTTP " + e.getStatusCode().value() + "): "
                    + e.getResponseBodyAsString(), e);
        } catch (ResourceAccessException e) {
            throw new MoloniException("Não foi possível contactar o Moloni", e);
        }
        JsonNode node = ler(resposta);
        if (!node.hasNonNull("access_token")) {
            throw new MoloniException("Login no Moloni falhou: " + node);
        }
        accessToken = node.get("access_token").asText();
        expiraEm = Instant.now().plusSeconds(node.path("expires_in").asLong(3600));
        return accessToken;
    }

    private JsonNode ler(String resposta) {
        try {
            return json.readTree(resposta == null ? "null" : resposta);
        } catch (JsonProcessingException e) {
            throw new MoloniException("Resposta inválida do Moloni: " + resposta, e);
        }
    }

    private static boolean vazio(String s) {
        return s == null || s.isBlank();
    }

    /** Ajuda para criar mapas de parâmetros que aceitam valores null (ignorados). */
    public static Map<String, Object> params(Object... chaveValor) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i + 1 < chaveValor.length; i += 2) {
            if (chaveValor[i + 1] != null) {
                m.put((String) chaveValor[i], chaveValor[i + 1]);
            }
        }
        return m;
    }
}
