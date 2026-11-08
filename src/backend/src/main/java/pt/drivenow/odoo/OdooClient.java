package pt.drivenow.odoo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import pt.drivenow.config.HttpTimeouts;
import pt.drivenow.config.OdooProperties;

/**
 * Cliente da API JSON-2 do Odoo 19.
 *
 * Cada chamada é um POST para /json/2/{modelo}/{método}, com os argumentos do método
 * em JSON e a chave de API no cabeçalho Authorization.
 */
@Component
public class OdooClient {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LISTA_DE_REGISTOS =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<List<Number>> LISTA_DE_IDS =
            new ParameterizedTypeReference<>() { };
    private static final ParameterizedTypeReference<Object> QUALQUER =
            new ParameterizedTypeReference<>() { };

    /** Idioma das respostas do Odoo (nomes de estados, etc.). */
    private static final Map<String, Object> CONTEXTO = Map.of("lang", "pt_PT");

    private final RestClient http;
    private final ObjectMapper json;
    private final String url;

    public OdooClient(OdooProperties props, RestClient.Builder builder, ObjectMapper json) {
        this.url = props.url();
        this.json = json;
        RestClient.Builder b = builder
                .requestFactory(HttpTimeouts.fabrica())
                .baseUrl(props.url())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.apiKey());
        if (props.database() != null && !props.database().isBlank()) {
            b.defaultHeader("X-Odoo-Database", props.database());
        }
        this.http = b.build();
    }

    /** search_read: procura registos e devolve os campos pedidos. */
    public List<Map<String, Object>> searchRead(String model, List<?> domain, List<String> fields, String order) {
        Map<String, Object> body = new HashMap<>();
        body.put("domain", domain);
        body.put("fields", fields);
        if (order != null) {
            body.put("order", order);
        }
        List<Map<String, Object>> result = call(model, "search_read", body, LISTA_DE_REGISTOS);
        return result != null ? result : List.of();
    }

    /** create: cria um registo e devolve o id. */
    public long create(String model, Map<String, Object> values) {
        Map<String, Object> body = Map.of("vals_list", List.of(values));
        List<Number> ids = call(model, "create", body, LISTA_DE_IDS);
        if (ids == null || ids.isEmpty()) {
            throw new OdooException("O Odoo não devolveu o id do registo criado em " + model, null);
        }
        return ids.get(0).longValue();
    }

    /** write: altera registos existentes. */
    public void write(String model, List<Long> ids, Map<String, Object> values) {
        Map<String, Object> body = Map.of("ids", ids, "vals", values);
        call(model, "write", body, QUALQUER);
    }

    private <T> T call(String model, String method, Map<String, Object> args, ParameterizedTypeReference<T> type) {
        Map<String, Object> body = new HashMap<>(args);
        body.putIfAbsent("context", CONTEXTO);
        // O Odoo só lê o corpo quando o pedido traz Content-Length, por isso
        // o JSON é convertido em bytes antes de ser enviado (e não aos pedaços).
        byte[] bytes;
        try {
            bytes = json.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            throw new OdooException("Não foi possível converter o pedido para JSON", e);
        }
        try {
            return http.post()
                    .uri("/json/2/{model}/{method}", model, method)
                    .contentType(MediaType.APPLICATION_JSON)
                    .contentLength(bytes.length)
                    .body(bytes)
                    .retrieve()
                    .body(type);
        } catch (RestClientResponseException e) {
            throw new OdooException("Odoo " + model + "." + method + " falhou (HTTP "
                    + e.getStatusCode().value() + "): " + e.getResponseBodyAsString(), e);
        } catch (ResourceAccessException e) {
            throw new OdooException("Não foi possível contactar o Odoo em " + url, e);
        }
    }

    // ---- Ajudas para ler os valores que o Odoo devolve ----
    // O Odoo devolve "false" nos campos vazios e [id, "nome"] nos campos Many2one.

    public static String texto(Object value) {
        return (value == null || Boolean.FALSE.equals(value)) ? null : value.toString();
    }

    public static Long idDe(Object many2one) {
        if (many2one instanceof List<?> l && !l.isEmpty() && l.get(0) instanceof Number n) {
            return n.longValue();
        }
        if (many2one instanceof Number n) {
            return n.longValue();
        }
        return null;
    }

    public static String nomeDe(Object many2one) {
        if (many2one instanceof List<?> l && l.size() > 1) {
            return texto(l.get(1));
        }
        return null;
    }

    public static Double numero(Object value) {
        return value instanceof Number n ? n.doubleValue() : null;
    }

    public static Integer inteiro(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        String s = texto(value);
        if (s == null) {
            return null;
        }
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean booleano(Object value) {
        return Boolean.TRUE.equals(value);
    }

    /** Ajuda para escrever domínios do Odoo, ex.: dominio("is_damage", "=", true). */
    public static List<Object> condicao(String campo, String operador, Object valor) {
        List<Object> c = new ArrayList<>(3);
        c.add(campo);
        c.add(operador);
        c.add(valor);
        return c;
    }
}
