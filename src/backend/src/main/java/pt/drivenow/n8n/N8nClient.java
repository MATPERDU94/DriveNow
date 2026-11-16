package pt.drivenow.n8n;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Envia eventos de negócio ao n8n (webhooks), que orquestra as integrações com o SAP, o Moloni e o Odoo.
 * Cada evento é um POST para {n8n.url}/webhook/{evento} com os dados em JSON.
 */
@Component
public class N8nClient {

    private static final Logger log = LoggerFactory.getLogger(N8nClient.class);
    private static final ParameterizedTypeReference<Map<String, Object>> MAPA = new ParameterizedTypeReference<>() {
    };

    private final RestClient http;
    private final boolean configurado;

    public N8nClient(@Value("${n8n.url:}") String url) {
        this.configurado = url != null && !url.isBlank();
        // HTTP/1.1: o cliente HTTP do Java tenta por defeito passar a HTTP/2 ("Upgrade: h2c"),
        // e o n8n trata esse pedido como uma ligação websocket e não responde.
        HttpClient cliente = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(cliente);
        fabrica.setReadTimeout(Duration.ofSeconds(10));
        this.http = RestClient.builder()
                .baseUrl(configurado ? url : "http://localhost")
                .requestFactory(fabrica)
                .build();
    }

    /**
     * Envia o evento ao n8n sem esperar dados de volta. Devolve false se o n8n não estiver
     * configurado (N8N_URL vazio) ou não responder; nesse caso quem chama decide o que fazer.
     */
    public boolean enviar(String evento, Map<String, Object> dados) {
        if (!configurado) {
            return false;
        }
        try {
            http.post()
                    .uri("/webhook/{evento}", evento)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(dados)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Evento '{}' enviado ao n8n: {}", evento, dados);
            return true;
        } catch (RestClientException e) {
            log.warn("Não foi possível enviar o evento '{}' ao n8n: {}", evento, e.getMessage());
            return false;
        }
    }

    /**
     * Envia o evento ao n8n e devolve a resposta do fluxo (JSON). Vazio se o n8n não estiver
     * configurado, não responder ou responder sem JSON.
     */
    public Optional<Map<String, Object>> pedir(String evento, Map<String, Object> dados) {
        if (!configurado) {
            return Optional.empty();
        }
        try {
            Map<String, Object> resposta = http.post()
                    .uri("/webhook/{evento}", evento)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(dados)
                    .retrieve()
                    .body(MAPA);
            log.info("Evento '{}' enviado ao n8n: {} -> resposta {}", evento, dados, resposta);
            return Optional.ofNullable(resposta);
        } catch (RestClientException e) {
            log.warn("Não foi possível enviar o evento '{}' ao n8n: {}", evento, e.getMessage());
            return Optional.empty();
        }
    }
}
