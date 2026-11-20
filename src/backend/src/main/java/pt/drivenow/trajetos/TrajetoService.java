package pt.drivenow.trajetos;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pt.drivenow.localizacao.LocalizacaoService;
import pt.drivenow.reservas.EstadoReserva;
import pt.drivenow.reservas.Reserva;
import pt.drivenow.reservas.ReservaRepository;

/**
 * Trajeto das viagens (diagrama 2b): no fim da viagem, o backend pede ao serviço de rotas o percurso
 * entre o local de início (onde o carro estava na reserva) e o local de fim, e guarda-o.
 * Se o serviço não responder, a viagem termina na mesma e a app mostra a linha reta.
 */
@Service
public class TrajetoService {

    static final String FONTE_OSRM = "OSRM · dados © OpenStreetMap";

    private static final Logger log = LoggerFactory.getLogger(TrajetoService.class);

    private final ReservaRepository reservas;
    private final TrajetoRepository trajetos;
    private final RotasClient rotas;
    private final ObjectMapper json;

    public TrajetoService(ReservaRepository reservas, TrajetoRepository trajetos, RotasClient rotas, ObjectMapper json) {
        this.reservas = reservas;
        this.trajetos = trajetos;
        this.rotas = rotas;
        this.json = json;
    }

    /** Pede o percurso ao serviço de rotas e guarda-o. Nunca falha: sem resposta, fica sem trajeto. */
    public void calcular(long reservaId) {
        try {
            Reserva r = reservas.findById(reservaId).orElse(null);
            if (r == null || !temInicioEFim(r) || trajetos.existsById(reservaId)) {
                return;
            }
            rotas.percurso(r.getLatInicial(), r.getLngInicial(), r.getLatFinal(), r.getLngFinal())
                    .ifPresent(rota -> {
                        trajetos.save(new Trajeto(reservaId, paraJson(rota.pontos()),
                                arredondar(rota.distanciaKm(), 100), arredondar(rota.duracaoMin(), 10)));
                        log.info("Trajeto da reserva {} guardado: {} pontos, {} km", reservaId, rota.pontos().size(),
                                arredondar(rota.distanciaKm(), 100));
                    });
        } catch (RuntimeException e) {
            log.warn("Trajeto da reserva {} não calculado: {}", reservaId, e.getMessage());
        }
    }

    /**
     * Trajeto de uma viagem concluída. Se ainda não houver (o serviço não respondeu no fim da viagem),
     * tenta outra vez; se continuar sem resposta, devolve a linha reta entre o início e o fim.
     * Vazio para reservas canceladas, em curso ou feitas antes de se guardarem os locais.
     */
    public Optional<TrajetoDto> obter(long reservaId) {
        Reserva r = reservas.findById(reservaId).orElse(null);
        if (r == null || r.getEstado() != EstadoReserva.CONCLUIDA || !temInicioEFim(r)) {
            return Optional.empty();
        }
        if (!trajetos.existsById(reservaId)) {
            calcular(reservaId);
        }
        TrajetoDto.Ponto origem = new TrajetoDto.Ponto(r.getLatInicial(), r.getLngInicial());
        TrajetoDto.Ponto destino = new TrajetoDto.Ponto(r.getLatFinal(), r.getLngFinal());
        Optional<Trajeto> guardado = trajetos.findById(reservaId);
        if (guardado.isPresent()) {
            Trajeto t = guardado.get();
            return Optional.of(new TrajetoDto("RUAS", origem, destino, deJson(t.getPontos()), t.getDistanciaKm(),
                    t.getDuracaoMin(), FONTE_OSRM));
        }
        double reta = LocalizacaoService.distanciaKm(origem.lat(), origem.lng(), destino.lat(), destino.lng());
        return Optional.of(new TrajetoDto("LINHA_RETA", origem, destino,
                List.of(new double[] { origem.lat(), origem.lng() }, new double[] { destino.lat(), destino.lng() }),
                arredondar(reta, 100), null, null));
    }

    private static boolean temInicioEFim(Reserva r) {
        return r.getLatInicial() != null && r.getLngInicial() != null && r.getLatFinal() != null && r.getLngFinal() != null;
    }

    /** [[41.44170,-8.29550],...]: 5 casas decimais chegam (cerca de 1 metro). */
    static String paraJson(List<double[]> pontos) {
        return pontos.stream()
                .map(p -> String.format(Locale.ROOT, "[%.5f,%.5f]", p[0], p[1]))
                .collect(Collectors.joining(",", "[", "]"));
    }

    private List<double[]> deJson(String texto) {
        try {
            return Arrays.asList(json.readValue(texto, double[][].class));
        } catch (JsonProcessingException e) {
            log.warn("Trajeto guardado inválido: {}", e.getMessage());
            return List.of();
        }
    }

    private static double arredondar(double valor, int escala) {
        return Math.round(valor * escala) / (double) escala;
    }
}
