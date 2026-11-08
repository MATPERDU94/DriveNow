package pt.drivenow.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.http.client.JdkClientHttpRequestFactory;

/** Tempos limite das chamadas ao Odoo e ao Moloni: nunca ficar à espera indefinidamente. */
public final class HttpTimeouts {

    private HttpTimeouts() {
    }

    public static JdkClientHttpRequestFactory fabrica() {
        HttpClient cliente = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(cliente);
        fabrica.setReadTimeout(Duration.ofSeconds(30));
        return fabrica;
    }
}
