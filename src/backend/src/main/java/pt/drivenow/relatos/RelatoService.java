package pt.drivenow.relatos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.drivenow.common.RegraNegocioException;
import pt.drivenow.danos.DanoService;
import pt.drivenow.danos.TipoDanoDto;
import pt.drivenow.frota.FrotaService;
import pt.drivenow.frota.ViaturaDto;
import pt.drivenow.localizacao.LocalizacaoService;
import pt.drivenow.reservas.EstadoReserva;
import pt.drivenow.reservas.Reserva;
import pt.drivenow.reservas.ReservaRepository;
import pt.drivenow.reservas.ReservaService;

/**
 * Relatos de início e de fim de viagem (diagramas 2 e 4).
 *
 * Para cada dano assinalado:
 * - se já está registado no Odoo e por reparar, é ignorado;
 * - senão, é registado no Odoo e cobrado ao responsável: no relato de FIM, o próprio cliente;
 *   no relato de INÍCIO, o condutor anterior (a última viagem concluída nesta viatura);
 * - o total de penalizações de uma viagem não passa do limite da franquia.
 */
@Service
public class RelatoService {

    public record DanoReportado(Long tipoDanoId, String descricao) {
    }

    private final RelatoRepository relatos;
    private final ReservaService reservas;
    private final ReservaRepository reservaRepository;
    private final DanoService danos;
    private final LocalizacaoService localizacao;
    private final FrotaService frota;
    private final BigDecimal franquia;

    public RelatoService(RelatoRepository relatos, ReservaService reservas, ReservaRepository reservaRepository,
            DanoService danos, LocalizacaoService localizacao, FrotaService frota,
            @Value("${drivenow.franquia:1000}") BigDecimal franquia) {
        this.relatos = relatos;
        this.reservas = reservas;
        this.reservaRepository = reservaRepository;
        this.danos = danos;
        this.localizacao = localizacao;
        this.frota = frota;
        this.franquia = franquia.setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public RelatoDto criar(long reservaId, TipoRelato tipo, String observacoes, List<DanoReportado> reportados,
            Double latitude, Double longitude, Double quilometragem) {
        Reserva reserva = reservas.buscar(reservaId);
        validarEstado(reserva, tipo);
        if (tipo == TipoRelato.FIM) {
            if (latitude == null || longitude == null) {
                throw new RegraNegocioException("O relato de FIM tem de indicar a latitude e a longitude onde o carro ficou");
            }
            LocalizacaoService.validar(latitude, longitude);
            if (quilometragem != null) {
                validarQuilometragem(reserva.getViaturaOdooId(), quilometragem);
            }
        }
        if (relatos.existsByReservaIdAndTipo(reservaId, tipo)) {
            throw new RegraNegocioException("Esta reserva já tem um relato de " + tipo);
        }

        // Quem paga os danos deste relato
        Reserva responsavel = tipo == TipoRelato.FIM
                ? reserva
                : reservaRepository.findFirstByViaturaOdooIdAndEstadoAndIdLessThanOrderByFimViagemDesc(
                        reserva.getViaturaOdooId(), EstadoReserva.CONCLUIDA, reserva.getId()).orElse(null);
        BigDecimal margemFranquia = responsavel == null ? BigDecimal.ZERO
                : franquia.subtract(tipo == TipoRelato.FIM ? BigDecimal.ZERO : responsavel.getTotalPenalizacoes())
                        .max(BigDecimal.ZERO);

        Relato relato = new Relato(reserva, tipo, observacoes);
        BigDecimal cobrado = BigDecimal.ZERO;

        for (DanoReportado d : reportados == null ? List.<DanoReportado>of() : reportados) {
            if (d == null || d.tipoDanoId() == null) {
                throw new RegraNegocioException("Cada dano tem de indicar o tipoDanoId");
            }
            TipoDanoDto tipoDano = danos.obterTipo(d.tipoDanoId())
                    .orElseThrow(() -> new RegraNegocioException("Tipo de dano " + d.tipoDanoId() + " não existe"));

            if (danos.existeDanoAberto(reserva.getViaturaOdooId(), tipoDano.id())) {
                relato.adicionarDano(new RelatoDano(tipoDano.id(), tipoDano.nome(), d.descricao(), zero(),
                        null, null, "Dano já registado e por reparar: ignorado"));
                continue;
            }

            BigDecimal custo = BigDecimal.valueOf(tipoDano.custo() == null ? 0 : tipoDano.custo())
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal penalizacao = zero();
            String nota;
            if (responsavel == null) {
                nota = "Sem condutor anterior identificado: não cobrado";
            } else {
                penalizacao = custo.min(margemFranquia.subtract(cobrado)).max(BigDecimal.ZERO)
                        .setScale(2, RoundingMode.HALF_UP);
                nota = tipo == TipoRelato.FIM
                        ? "Cobrado ao cliente"
                        : "Cobrado ao condutor anterior (reserva #" + responsavel.getId() + ")";
                if (penalizacao.compareTo(custo) < 0) {
                    nota += "; limitado pela franquia de " + franquia + " €";
                }
                cobrado = cobrado.add(penalizacao);
            }

            long odooId = danos.registar(reserva.getViaturaOdooId(), tipoDano.id(), custo.doubleValue(),
                    descricaoOdoo(reserva, tipo, responsavel, penalizacao, d.descricao()));
            relato.adicionarDano(new RelatoDano(tipoDano.id(), tipoDano.nome(), d.descricao(), penalizacao,
                    odooId, responsavel == null ? null : responsavel.getId(), nota));
        }

        relato = relatos.save(relato);
        if (tipo == TipoRelato.INICIO) {
            reserva.iniciarViagem();
            if (responsavel != null && cobrado.signum() > 0) {
                responsavel.adicionarPenalizacao(cobrado);
            }
        } else {
            reserva.terminarViagem(cobrado);
            localizacao.atualizar(reserva.getViaturaOdooId(), latitude, longitude);
            if (quilometragem != null) {
                frota.registarQuilometragem(reserva.getViaturaOdooId(), quilometragem);
            }
        }
        return RelatoDto.de(relato);
    }

    @Transactional(readOnly = true)
    public List<RelatoDto> daReserva(long reservaId) {
        return relatos.findByReservaIdOrderByCriadoEm(reservaId).stream().map(RelatoDto::de).toList();
    }

    private void validarQuilometragem(long viaturaId, double km) {
        Double atual = frota.obter(viaturaId).map(ViaturaDto::quilometragem).orElse(null);
        if (atual != null && km < atual) {
            throw new RegraNegocioException("A quilometragem final (" + km
                    + ") não pode ser inferior à atual (" + atual + ")");
        }
    }

    private static void validarEstado(Reserva reserva, TipoRelato tipo) {
        EstadoReserva esperado = tipo == TipoRelato.INICIO ? EstadoReserva.RESERVADA : EstadoReserva.EM_CURSO;
        if (reserva.getEstado() != esperado) {
            throw new RegraNegocioException("O relato de " + tipo + " exige a reserva no estado " + esperado
                    + " (estado atual: " + reserva.getEstado() + ")");
        }
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2);
    }

    private static String descricaoOdoo(Reserva reserva, TipoRelato tipo, Reserva responsavel, BigDecimal penalizacao,
            String descricaoCliente) {
        String texto = "Reserva #" + reserva.getId() + " | relato de " + tipo
                + " | reportado por: " + reserva.getUtilizador().getNome()
                + (responsavel == null ? " | sem responsável"
                        : " | responsável: reserva #" + responsavel.getId() + " (" + penalizacao + " €)");
        if (descricaoCliente != null && !descricaoCliente.isBlank()) {
            texto += " | " + descricaoCliente;
        }
        return texto;
    }
}
