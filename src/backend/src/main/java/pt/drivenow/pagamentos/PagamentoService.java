package pt.drivenow.pagamentos;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.drivenow.common.NaoEncontradoException;
import pt.drivenow.common.RegraNegocioException;
import pt.drivenow.relatos.Relato;
import pt.drivenow.relatos.RelatoDano;
import pt.drivenow.relatos.RelatoRepository;
import pt.drivenow.reservas.Reserva;
import pt.drivenow.reservas.ReservaRepository;

/**
 * Meio de pagamento e cobranças (simuladas), diagramas 1, 3 e 4.
 *
 * Cada cobrança é aprovada se o cliente tiver um meio de pagamento válido; é recusada se não tiver,
 * se o cartão estiver expirado ou se for o cartão de teste 4000 0000 0000 0002.
 * Cobranças recusadas ficam EM_DIVIDA e bloqueiam novas reservas; ao atualizar o meio de pagamento,
 * são cobradas de novo automaticamente.
 */
@Service
public class PagamentoService {

    private final MeioPagamentoRepository meios;
    private final CobrancaRepository cobrancas;
    private final ReservaRepository reservas;
    private final RelatoRepository relatos;

    public PagamentoService(MeioPagamentoRepository meios, CobrancaRepository cobrancas,
            ReservaRepository reservas, RelatoRepository relatos) {
        this.meios = meios;
        this.cobrancas = cobrancas;
        this.reservas = reservas;
        this.relatos = relatos;
    }

    // ---- Meio de pagamento ----

    @Transactional(readOnly = true)
    public Optional<MeioPagamento> meioDe(long utilizadorId) {
        return meios.findByUtilizadorId(utilizadorId);
    }

    /** Valida e guarda (ou substitui) o meio de pagamento do utilizador. */
    @Transactional
    public MeioPagamento guardarMeio(long utilizadorId, PedidoMeioPagamento p) {
        if (p == null || p.tipo() == null) {
            throw new RegraNegocioException("Indique o meio de pagamento (CARTAO ou CONTA_BANCARIA)");
        }
        if (p.titular() == null || p.titular().isBlank()) {
            throw new RegraNegocioException("Indique o titular do meio de pagamento");
        }
        MeioPagamento meio = meios.findByUtilizadorId(utilizadorId).orElseGet(() -> new MeioPagamento(utilizadorId));
        if (p.tipo() == TipoMeioPagamento.CARTAO) {
            String numero = ValidacaoPagamento.soDigitos(p.numero());
            if (!ValidacaoPagamento.luhn(numero)) {
                throw new RegraNegocioException("Número de cartão inválido");
            }
            if (!ValidacaoPagamento.validadeOk(p.validade())) {
                throw new RegraNegocioException("Validade do cartão inválida ou expirada (formato MM/AA)");
            }
            if (p.cvv() == null || !p.cvv().matches("\\d{3,4}")) {
                throw new RegraNegocioException("CVV inválido");
            }
            meio.definir(TipoMeioPagamento.CARTAO, p.titular().trim(), ValidacaoPagamento.marca(numero),
                    numero.substring(numero.length() - 4), p.validade(),
                    ValidacaoPagamento.CARTAO_RECUSA.equals(numero));
        } else {
            String iban = ValidacaoPagamento.soDigitos(p.iban()).toUpperCase();
            if (!ValidacaoPagamento.ibanOk(iban)) {
                throw new RegraNegocioException("IBAN inválido (formato PT50 + 21 dígitos)");
            }
            meio.definir(TipoMeioPagamento.CONTA_BANCARIA, p.titular().trim(), "IBAN",
                    iban.substring(iban.length() - 4), null, false);
        }
        return meios.save(meio);
    }

    // ---- Cobranças ----

    /** Cobra a viagem (ou a reserva cancelada) e regista o resultado na reserva. Devolve null se não houver valor. */
    @Transactional
    public Cobranca cobrarViagem(long reservaId) {
        Reserva r = reservas.findById(reservaId)
                .orElseThrow(() -> new NaoEncontradoException("Reserva " + reservaId + " não existe"));
        Optional<Cobranca> existente = cobrancas.findFirstByReservaIdAndTipo(reservaId, TipoCobranca.VIAGEM);
        if (existente.isPresent()) {
            return existente.get();
        }
        if (r.getTotal() == null || r.getTotal().signum() <= 0) {
            r.pagamentoRegistado(true, null);
            return null;
        }
        Cobranca c = new Cobranca(r.getUtilizador().getId(), r.getId(), null, TipoCobranca.VIAGEM, r.getTotal());
        tentar(c);
        r.pagamentoRegistado(c.paga(), c.getMeioDescricao());
        return cobrancas.save(c);
    }

    /** Cobra ao condutor anterior as penalizações de um relato de início. Devolve null se não houver. */
    @Transactional
    public Cobranca cobrarPenalizacoes(long relatoId) {
        if (cobrancas.findFirstByRelatoId(relatoId).isPresent()) {
            return cobrancas.findFirstByRelatoId(relatoId).get();
        }
        Relato relato = relatos.comDanosPorId(relatoId)
                .orElseThrow(() -> new NaoEncontradoException("Relato " + relatoId + " não existe"));
        BigDecimal total = BigDecimal.ZERO;
        Long responsavelId = null;
        for (RelatoDano d : relato.getDanos()) {
            if (d.temPenalizacao() && d.getReservaResponsavelId() != null) {
                total = total.add(d.getPenalizacao());
                responsavelId = d.getReservaResponsavelId();
            }
        }
        if (responsavelId == null || total.signum() <= 0) {
            return null;
        }
        Long idResponsavel = responsavelId;
        Reserva responsavel = reservas.findById(idResponsavel)
                .orElseThrow(() -> new NaoEncontradoException("Reserva " + idResponsavel + " não existe"));
        Cobranca c = new Cobranca(responsavel.getUtilizador().getId(), responsavel.getId(), relatoId,
                TipoCobranca.PENALIZACAO, total);
        tentar(c);
        return cobrancas.save(c);
    }

    /** Volta a cobrar as dívidas do utilizador; devolve as que ficaram pagas. */
    @Transactional
    public List<Cobranca> pagarDividas(long utilizadorId) {
        List<Cobranca> pagas = new ArrayList<>();
        for (Cobranca c : cobrancas.findByUtilizadorIdAndEstado(utilizadorId, EstadoCobranca.EM_DIVIDA)) {
            tentar(c);
            if (c.paga()) {
                pagas.add(c);
                if (c.getTipo() == TipoCobranca.VIAGEM) {
                    reservas.findById(c.getReservaId()).ifPresent(r -> r.pagamentoRegistado(true, c.getMeioDescricao()));
                }
            }
        }
        return pagas;
    }

    @Transactional(readOnly = true)
    public boolean temDivida(long utilizadorId) {
        return cobrancas.existsByUtilizadorIdAndEstado(utilizadorId, EstadoCobranca.EM_DIVIDA);
    }

    @Transactional(readOnly = true)
    public BigDecimal valorEmDivida(long utilizadorId) {
        return cobrancas.findByUtilizadorIdAndEstado(utilizadorId, EstadoCobranca.EM_DIVIDA).stream()
                .map(Cobranca::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public List<CobrancaDto> cobrancasDe(long utilizadorId) {
        return cobrancas.findByUtilizadorIdOrderByCriadaEmDesc(utilizadorId).stream().map(CobrancaDto::de).toList();
    }

    /** Simulação do banco. */
    private void tentar(Cobranca c) {
        MeioPagamento meio = meios.findByUtilizadorId(c.getUtilizadorId()).orElse(null);
        if (meio == null) {
            c.recusada(null, "Sem meio de pagamento associado");
        } else if (meio.expirado()) {
            c.recusada(meio.descricao(), "Cartão expirado");
        } else if (meio.isRecusaSimulada()) {
            c.recusada(meio.descricao(), "Pagamento recusado pelo banco (simulado)");
        } else {
            c.aprovada(meio.descricao());
        }
    }
}
