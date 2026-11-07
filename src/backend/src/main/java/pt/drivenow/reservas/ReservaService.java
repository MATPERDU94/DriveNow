package pt.drivenow.reservas;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.drivenow.common.NaoEncontradoException;
import pt.drivenow.common.RegraNegocioException;
import pt.drivenow.frota.FrotaService;
import pt.drivenow.frota.ViaturaDto;
import pt.drivenow.utilizadores.Utilizador;
import pt.drivenow.utilizadores.UtilizadorRepository;

@Service
public class ReservaService {

    private static final Set<EstadoReserva> ATIVAS = EnumSet.of(EstadoReserva.RESERVADA, EstadoReserva.EM_CURSO);

    private final ReservaRepository reservas;
    private final UtilizadorRepository utilizadores;
    private final FrotaService frota;

    public ReservaService(ReservaRepository reservas, UtilizadorRepository utilizadores, FrotaService frota) {
        this.reservas = reservas;
        this.utilizadores = utilizadores;
        this.frota = frota;
    }

    @Transactional
    public ReservaDto criar(long utilizadorId, long viaturaId) {
        Utilizador utilizador = utilizadores.findById(utilizadorId)
                .orElseThrow(() -> new NaoEncontradoException("Utilizador " + utilizadorId + " não existe"));
        ViaturaDto viatura = frota.obter(viaturaId)
                .orElseThrow(() -> new NaoEncontradoException("Viatura " + viaturaId + " não existe no Odoo"));

        if (reservas.existsByUtilizadorIdAndEstadoIn(utilizadorId, ATIVAS)) {
            throw new RegraNegocioException("O utilizador já tem uma reserva ativa");
        }
        if (reservas.existsByViaturaOdooIdAndEstadoIn(viaturaId, ATIVAS)) {
            throw new RegraNegocioException("A viatura " + viatura.matricula() + " já está reservada");
        }
        if (viatura.precoMinuto() == null || viatura.precoMinuto() <= 0) {
            throw new RegraNegocioException("A viatura " + viatura.matricula()
                    + " ainda não tem preço por minuto definido no Odoo (Frota → Tarifário)");
        }
        BigDecimal taxa = BigDecimal.valueOf(viatura.taxaEntrada() != null ? viatura.taxaEntrada() : 0);
        BigDecimal porMinuto = BigDecimal.valueOf(viatura.precoMinuto());
        return ReservaDto.de(reservas.save(new Reserva(utilizador, viaturaId, viatura.matricula(), taxa, porMinuto)));
    }

    @Transactional(readOnly = true)
    public ReservaDto obter(long id) {
        return ReservaDto.de(buscar(id));
    }

    @Transactional(readOnly = true)
    public List<ReservaDto> doUtilizador(long utilizadorId) {
        return reservas.findByUtilizadorIdOrderByCriadaEmDesc(utilizadorId).stream().map(ReservaDto::de).toList();
    }

    @Transactional
    public ReservaDto cancelar(long id) {
        Reserva r = buscar(id);
        if (r.getEstado() != EstadoReserva.RESERVADA) {
            throw new RegraNegocioException("Só é possível cancelar reservas que ainda não começaram");
            // Nota: a taxa de entrada não é devolvida ao cancelar.
        }
        r.cancelar();
        return ReservaDto.de(r);
    }

    /** Usado também pelos relatos (dentro da mesma transação). */
    public Reserva buscar(long id) {
        return reservas.findById(id).orElseThrow(() -> new NaoEncontradoException("Reserva " + id + " não existe"));
    }
}
