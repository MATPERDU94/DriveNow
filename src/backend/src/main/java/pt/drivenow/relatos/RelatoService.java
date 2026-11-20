package pt.drivenow.relatos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.drivenow.common.RegraNegocioException;
import pt.drivenow.danos.DanoService;
import pt.drivenow.danos.TipoDanoDto;
import pt.drivenow.frota.FrotaService;
import pt.drivenow.frota.ViaturaDto;
import pt.drivenow.localizacao.LocalizacaoService;
import pt.drivenow.n8n.N8nClient;
import pt.drivenow.reservas.EstadoReserva;
import pt.drivenow.reservas.Reserva;
import pt.drivenow.reservas.ReservaRepository;
import pt.drivenow.reservas.ReservaService;
import pt.drivenow.revisoes.RevisaoService;

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

    /** Uma viagem com mais km do que isto é quase de certeza um erro ao escrever os km do painel. */
    static final double MAX_KM_VIAGEM = 1000;

    private final RelatoRepository relatos;
    private final ReservaService reservas;
    private final ReservaRepository reservaRepository;
    private final DanoService danos;
    private final LocalizacaoService localizacao;
    private final FrotaService frota;
    private final N8nClient n8n;
    private final RevisaoService revisoes;
    private final BigDecimal franquia;

    public RelatoService(RelatoRepository relatos, ReservaService reservas, ReservaRepository reservaRepository,
            DanoService danos, LocalizacaoService localizacao, FrotaService frota, N8nClient n8n,
            RevisaoService revisoes, @Value("${drivenow.franquia:1000}") BigDecimal franquia) {
        this.relatos = relatos;
        this.reservas = reservas;
        this.reservaRepository = reservaRepository;
        this.danos = danos;
        this.localizacao = localizacao;
        this.frota = frota;
        this.n8n = n8n;
        this.revisoes = revisoes;
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
            validarQuilometragem(reserva, quilometragem);
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
            reserva.terminarViagem(cobrado, quilometragem, latitude, longitude);
            localizacao.atualizar(reserva.getViaturaOdooId(), latitude, longitude);
            // Diagrama 2b: o evento "viagem terminada" vai para o n8n, que atualiza a quilometragem no Odoo.
            // Se o n8n não estiver configurado ou não responder, o backend regista-a diretamente.
            Map<String, Object> evento = new LinkedHashMap<>();
            evento.put("reservaId", reserva.getId());
            evento.put("viaturaId", reserva.getViaturaOdooId());
            evento.put("matricula", reserva.getMatricula());
            evento.put("quilometragem", quilometragem);
            evento.put("kmViagem", reserva.getKmViagem());
            evento.put("latitude", latitude);
            evento.put("longitude", longitude);
            if (!n8n.enviar("viagem-terminada", evento)) {
                frota.registarQuilometragem(reserva.getViaturaOdooId(), quilometragem);
                // Diagrama 6 sem n8n: se o carro chegou aos km da revisão, cria a ordem de revisão no Odoo.
                revisoes.verificarDepoisDaViagem(reserva.getViaturaOdooId(), quilometragem);
            }
        }
        return RelatoDto.de(relato);
    }

    @Transactional(readOnly = true)
    public List<RelatoDto> daReserva(long reservaId) {
        return relatos.findByReservaIdOrderByCriadoEm(reservaId).stream().map(RelatoDto::de).toList();
    }

    /**
     * Diagrama 2b: os km do painel são obrigatórios no fim da viagem e não podem ser inferiores
     * aos km do carro no momento da reserva.
     */
    private void validarQuilometragem(Reserva reserva, Double km) {
        if (km == null) {
            throw new RegraNegocioException("Indique os km do painel no fim da viagem");
        }
        if (km.isNaN() || km.isInfinite() || km < 0) {
            throw new RegraNegocioException("Os km do painel não são válidos");
        }
        // Reservas feitas antes de existirem km iniciais: compara-se com os km atuais do Odoo.
        Double inicial = reserva.getKmInicial() != null ? reserva.getKmInicial()
                : frota.obter(reserva.getViaturaOdooId()).map(ViaturaDto::quilometragem).orElse(null);
        if (inicial == null) {
            return;
        }
        if (km < inicial) {
            throw new RegraNegocioException("Os km do painel (" + formatarKm(km)
                    + ") não podem ser inferiores aos km no início da viagem (" + formatarKm(inicial) + ")");
        }
        if (km - inicial > MAX_KM_VIAGEM) {
            throw new RegraNegocioException("Com " + formatarKm(km) + " km no painel, a viagem teria "
                    + formatarKm(km - inicial) + " km (máximo " + formatarKm(MAX_KM_VIAGEM)
                    + " km). Confirme os km do painel");
        }
    }

    private static String formatarKm(double km) {
        return km == Math.rint(km) ? String.valueOf((long) km) : String.valueOf(Math.round(km * 10) / 10.0);
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
