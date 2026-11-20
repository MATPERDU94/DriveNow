package pt.drivenow.trajetos;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Serviço de rotas (OSRM): devolve o percurso de carro pelas ruas entre dois pontos.
 * Por omissão usa o servidor público router.project-osrm.org (dados do OpenStreetMap);
 * o endereço muda-se com ROTAS_URL e, se ficar vazio, não se pedem rotas.
 *
 * Pedido: GET {url}/route/v1/driving/{lng1},{lat1};{lng2},{lat2}?overview=full&geometries=geojson
 */
@Component
public class RotasClient {

    /**
     * @param pontos      pontos do percurso, cada um {latitude, longitude}
     * @param distanciaKm distância pelas ruas
     * @param duracaoMin  tempo de condução estimado pelo serviço
     */
    public record Rota(List<double[]> pontos, double distanciaKm, double duracaoMin) {
    }

    private static final Logger log = LoggerFactory.getLogger(RotasClient.class);

    private final String url;
    private final RestClient http;

    public RotasClient(@Value("${drivenow.rotas-url:https://router.project-osrm.org}") String url) {
        this.url = url == null ? "" : url.trim().replaceAll("/+$", "");
        // Tempos curtos: o fim da viagem não pode ficar à espera do serviço de rotas.
        HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(cliente);
        fabrica.setReadTimeout(Duration.ofSeconds(6));
        // As regras do servidor público pedem um User-Agent que identifique a aplicação.
        this.http = RestClient.builder()
                .requestFactory(fabrica)
                .defaultHeader("User-Agent", "DriveNow/1.0 (projeto academico IAIE, Universidade do Minho)")
                .build();
    }

    public boolean ativo() {
        return !url.isEmpty();
    }

    /** Percurso de carro entre o ponto 1 e o ponto 2. Vazio se o serviço estiver desligado ou não responder. */
    public Optional<Rota> percurso(double lat1, double lng1, double lat2, double lng2) {
        if (!ativo()) {
            return Optional.empty();
        }
        // O OSRM recebe longitude,latitude
        String coordenadas = String.format(Locale.ROOT, "%.6f,%.6f;%.6f,%.6f", lng1, lat1, lng2, lat2);
        try {
            JsonNode resposta = http.get()
                    .uri(URI.create(url + "/route/v1/driving/" + coordenadas + "?overview=full&geometries=geojson"))
                    .retrieve()
                    .body(JsonNode.class);
            return lerRota(resposta);
        } catch (RuntimeException e) {
            log.warn("Serviço de rotas sem resposta para {}: {}", coordenadas, e.getMessage());
            return Optional.empty();
        }
    }

    /** Lê a primeira rota da resposta do OSRM (geometria em GeoJSON: [[longitude, latitude], ...]). */
    static Optional<Rota> lerRota(JsonNode resposta) {
        if (resposta == null || !"Ok".equals(resposta.path("code").asText())) {
            log.warn("O serviço de rotas não encontrou percurso: {}", resposta == null ? null : resposta.path("code").asText());
            return Optional.empty();
        }
        JsonNode rota = resposta.path("routes").path(0);
        List<double[]> pontos = new ArrayList<>();
        for (JsonNode c : rota.path("geometry").path("coordinates")) {
            pontos.add(new double[] { c.path(1).asDouble(), c.path(0).asDouble() });
        }
        if (pontos.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Rota(pontos, rota.path("distance").asDouble() / 1000.0,
                rota.path("duration").asDouble() / 60.0));
    }
}
