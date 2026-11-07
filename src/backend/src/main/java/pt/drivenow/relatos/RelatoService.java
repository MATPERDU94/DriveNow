package pt.drivenow.relatos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.drivenow.common.RegraNegocioException;
import pt.drivenow.danos.DanoService;
import pt.drivenow.danos.TipoDanoDto;
import pt.drivenow.localizacao.LocalizacaoService;
import pt.drivenow.reservas.EstadoReserva;
import pt.drivenow.reservas.Reserva;
import pt.drivenow.reservas.ReservaService;

/**
 * Relatos de início e de fim de viagem.
 *
 * Cada dano assinalado é registado no Odoo (Frota → Danos → Registo de danos).
 * Só os danos novos do relato de FIM são cobrados ao cliente: os do relato de INÍCIO
 * já existiam, e um dano de FIM do mesmo tipo de um já assinalado no INÍCIO também não é cobrado.
 */
@Service
public class RelatoService {

    public record DanoReportado(Long tipoDanoId, String descricao) {
    }

    private final RelatoRepository relatos;
    private final ReservaService reservas;
    private final DanoService danos;
    private final LocalizacaoService localizacao;

    public RelatoService(RelatoRepository relatos, ReservaService reservas, DanoService danos,
            LocalizacaoService localizacao) {
        this.relatos = relatos;
        this.reservas = reservas;
        this.danos = danos;
        this.localizacao = localizacao;
    }

    @Transactional
    public RelatoDto criar(long reservaId, TipoRelato tipo, String observacoes, List<DanoReportado> reportados,
            Double latitude, Double longitude) {
        Reserva reserva = reservas.buscar(reservaId);
        validarEstado(reserva, tipo);
        if (tipo == TipoRelato.FIM) {
            if (latitude == null || longitude == null) {
                throw new RegraNegocioException("O relato de FIM tem de indicar a latitude e a longitude onde o carro ficou");
            }
            LocalizacaoService.validar(latitude, longitude);
        }
        if (relatos.existsByReservaIdAndTipo(reservaId, tipo)) {
            throw new RegraNegocioException("Esta reserva já tem um relato de " + tipo);
        }

        Set<Long> jaExistiam = tipo == TipoRelato.FIM ? tiposDoRelatoDeInicio(reservaId) : Set.of();
        Relato relato = new Relato(reserva, tipo, observacoes);

        for (DanoReportado d : reportados == null ? List.<DanoReportado>of() : reportados) {
            if (d == null || d.tipoDanoId() == null) {
                throw new RegraNegocioException("Cada dano tem de indicar o tipoDanoId");
            }
            TipoDanoDto tipoDano = danos.obterTipo(d.tipoDanoId())
                    .orElseThrow(() -> new RegraNegocioException("Tipo de dano " + d.tipoDanoId() + " não existe"));

            BigDecimal custo = BigDecimal.valueOf(tipoDano.custo() == null ? 0 : tipoDano.custo())
                    .setScale(2, RoundingMode.HALF_UP);
            boolean cobrado = tipo == TipoRelato.FIM && !jaExistiam.contains(tipoDano.id());
            BigDecimal penalizacao = cobrado ? custo : BigDecimal.ZERO.setScale(2);

            long odooId = danos.registar(reserva.getViaturaOdooId(), tipoDano.id(), custo.doubleValue(),
                    descricaoOdoo(reserva, tipo, cobrado, d.descricao()));

            relato.adicionarDano(new RelatoDano(tipoDano.id(), tipoDano.nome(), d.descricao(), penalizacao, odooId));
        }

        relato = relatos.save(relato);
        if (tipo == TipoRelato.INICIO) {
            reserva.iniciarViagem();
        } else {
            BigDecimal total = relato.getDanos().stream()
                    .map(RelatoDano::getPenalizacao)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            reserva.terminarViagem(total);
            localizacao.atualizar(reserva.getViaturaOdooId(), latitude, longitude);
        }
        return RelatoDto.de(relato);
    }

    @Transactional(readOnly = true)
    public List<RelatoDto> daReserva(long reservaId) {
        return relatos.findByReservaIdOrderByCriadoEm(reservaId).stream().map(RelatoDto::de).toList();
    }

    private static void validarEstado(Reserva reserva, TipoRelato tipo) {
        EstadoReserva esperado = tipo == TipoRelato.INICIO ? EstadoReserva.RESERVADA : EstadoReserva.EM_CURSO;
        if (reserva.getEstado() != esperado) {
            throw new RegraNegocioException("O relato de " + tipo + " exige a reserva no estado " + esperado
                    + " (estado atual: " + reserva.getEstado() + ")");
        }
    }

    private Set<Long> tiposDoRelatoDeInicio(long reservaId) {
        return relatos.findByReservaIdAndTipo(reservaId, TipoRelato.INICIO)
                .map(r -> r.getDanos().stream().map(RelatoDano::getTipoDanoOdooId).collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    private static String descricaoOdoo(Reserva reserva, TipoRelato tipo, boolean cobrado, String descricaoCliente) {
        String texto = "Reserva #" + reserva.getId() + " | relato de " + tipo
                + " | cliente: " + reserva.getUtilizador().getNome()
                + (cobrado ? " | cobrado ao cliente" : " | não cobrado");
        if (descricaoCliente != null && !descricaoCliente.isBlank()) {
            texto += " | " + descricaoCliente;
        }
        return texto;
    }
}
