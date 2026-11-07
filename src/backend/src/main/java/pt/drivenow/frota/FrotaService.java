package pt.drivenow.frota;

import static pt.drivenow.odoo.OdooClient.idDe;
import static pt.drivenow.odoo.OdooClient.inteiro;
import static pt.drivenow.odoo.OdooClient.nomeDe;
import static pt.drivenow.odoo.OdooClient.numero;
import static pt.drivenow.odoo.OdooClient.texto;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import pt.drivenow.localizacao.LocalizacaoService;
import pt.drivenow.localizacao.PosicaoViatura;
import pt.drivenow.odoo.OdooClient;
import pt.drivenow.reservas.EstadoReserva;
import pt.drivenow.reservas.ReservaRepository;

/**
 * Frota: os dados de cada viatura vêm do Odoo (fleet.vehicle);
 * a posição GPS e a disponibilidade vêm da base de dados do backend.
 */
@Service
public class FrotaService {

    private static final String MODELO = "fleet.vehicle";
    private static final List<String> CAMPOS = List.of(
            "license_plate", "model_id", "model_year", "seats", "odometer", "state_id");
    private static final Set<EstadoReserva> ATIVAS = EnumSet.of(EstadoReserva.RESERVADA, EstadoReserva.EM_CURSO);

    private final OdooClient odoo;
    private final LocalizacaoService localizacao;
    private final ReservaRepository reservas;

    public FrotaService(OdooClient odoo, LocalizacaoService localizacao, ReservaRepository reservas) {
        this.odoo = odoo;
        this.localizacao = localizacao;
        this.reservas = reservas;
    }

    /**
     * Lista a frota para o mapa.
     *
     * @param lat              posição do cliente (opcional)
     * @param lng              posição do cliente (opcional)
     * @param raioKm           só viaturas até esta distância (opcional; exige lat/lng)
     * @param apenasDisponiveis esconde as viaturas que estão numa reserva ativa
     */
    public List<ViaturaDto> listar(Double lat, Double lng, Double raioKm, boolean apenasDisponiveis) {
        boolean comPosicao = lat != null && lng != null;
        if (comPosicao) {
            LocalizacaoService.validar(lat, lng);
        }
        Map<Long, PosicaoViatura> posicoes = localizacao.todas();
        Set<Long> reservadas = new HashSet<>(reservas.viaturasComEstado(ATIVAS));

        List<ViaturaDto> resultado = listarDoOdoo().stream()
                .map(v -> completar(v, posicoes.get(v.id()), reservadas, lat, lng))
                .filter(v -> !apenasDisponiveis || v.disponivel())
                .filter(v -> raioKm == null || (v.distanciaKm() != null && v.distanciaKm() <= raioKm))
                .toList();

        if (comPosicao) {
            return resultado.stream()
                    .sorted(Comparator.comparing(ViaturaDto::distanciaKm,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
        }
        return resultado;
    }

    public Optional<ViaturaDto> obter(long id) {
        Set<Long> reservadas = new HashSet<>(reservas.viaturasComEstado(ATIVAS));
        return odoo.searchRead(MODELO, List.of(OdooClient.condicao("id", "=", id)), CAMPOS, null).stream()
                .findFirst()
                .map(FrotaService::paraDto)
                .map(v -> completar(v, localizacao.daViatura(id).orElse(null), reservadas, null, null));
    }

    /** Só os dados do Odoo, sem posição nem disponibilidade. */
    public List<ViaturaDto> listarDoOdoo() {
        return odoo.searchRead(MODELO, List.of(), CAMPOS, "license_plate").stream()
                .map(FrotaService::paraDto)
                .toList();
    }

    private static ViaturaDto completar(ViaturaDto v, PosicaoViatura p, Set<Long> reservadas, Double lat, Double lng) {
        Double vLat = p != null ? p.getLatitude() : null;
        Double vLng = p != null ? p.getLongitude() : null;
        Double distancia = null;
        if (p != null && lat != null && lng != null) {
            distancia = Math.round(LocalizacaoService.distanciaKm(lat, lng, vLat, vLng) * 100) / 100.0;
        }
        return v.comLocalizacao(vLat, vLng, !reservadas.contains(v.id()), distancia);
    }

    private static ViaturaDto paraDto(Map<String, Object> r) {
        return new ViaturaDto(
                idDe(r.get("id")),
                texto(r.get("license_plate")),
                nomeDe(r.get("model_id")),
                inteiro(r.get("model_year")),
                inteiro(r.get("seats")),
                numero(r.get("odometer")),
                nomeDe(r.get("state_id")),
                null, null, true, null);
    }
}
