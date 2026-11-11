package pt.drivenow.reservas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import pt.drivenow.utilizadores.Utilizador;

/**
 * Reserva de uma viatura e respetiva viagem.
 *
 * Preço: taxa de entrada (cobrada ao reservar) + minutos × preço por minuto + penalizações.
 * O temporizador começa no momento da reserva (criadaEm) e para no fim da viagem ou no cancelamento.
 * Os preços são copiados do Odoo no momento da reserva, para não mudarem a meio da viagem.
 */
@Entity
@Table(name = "reserva")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilizador_id")
    private Utilizador utilizador;

    /** Id da viatura no Odoo (fleet.vehicle). */
    @Column(nullable = false)
    private Long viaturaOdooId;

    private String matricula;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoReserva estado = EstadoReserva.RESERVADA;

    @Column(nullable = false)
    private Instant criadaEm = Instant.now();

    private Instant inicioViagem;

    private Instant fimViagem;

    @Column(precision = 10, scale = 2)
    private BigDecimal taxaEntrada;

    @Column(precision = 10, scale = 2)
    private BigDecimal precoMinuto;

    /** Minutos desde a reserva até ao fim ou cancelamento (cada minuto começado conta). */
    private Long minutos;

    /** minutos × preço por minuto. */
    @Column(precision = 10, scale = 2)
    private BigDecimal valorViagem;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPenalizacoes = BigDecimal.ZERO;

    /** taxa de entrada + valor da viagem + penalizações. */
    @Column(precision = 10, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    private EstadoPagamento estadoPagamento = EstadoPagamento.PENDENTE;

    /** Meio usado no pagamento, ex.: "Visa •••• 4242" (ecrã "Viagem terminada"). */
    private String pagamentoDescricao;

    @Enumerated(EnumType.STRING)
    private EstadoFatura estadoFatura = EstadoFatura.PENDENTE;

    /** Id da fatura-recibo no Moloni. */
    private Long moloniDocumentoId;

    @Column(length = 1000)
    private String faturaPdfUrl;

    @Column(length = 2000)
    private String faturaErro;

    protected Reserva() {
    }

    public Reserva(Utilizador utilizador, Long viaturaOdooId, String matricula,
            BigDecimal taxaEntrada, BigDecimal precoMinuto) {
        this.utilizador = utilizador;
        this.viaturaOdooId = viaturaOdooId;
        this.matricula = matricula;
        this.taxaEntrada = taxaEntrada.setScale(2, RoundingMode.HALF_UP);
        this.precoMinuto = precoMinuto.setScale(2, RoundingMode.HALF_UP);
        this.total = this.taxaEntrada;
    }

    public void iniciarViagem() {
        this.estado = EstadoReserva.EM_CURSO;
        this.inicioViagem = Instant.now();
    }

    public void terminarViagem(BigDecimal penalizacoes) {
        this.estado = EstadoReserva.CONCLUIDA;
        pararTemporizador();
        this.totalPenalizacoes = penalizacoes.setScale(2, RoundingMode.HALF_UP);
        this.total = taxaEntrada().add(valorViagem).add(totalPenalizacoes);
    }

    /** Cancelar antes de começar: paga a taxa de entrada e os minutos que o carro esteve bloqueado. */
    public void cancelar() {
        this.estado = EstadoReserva.CANCELADA;
        pararTemporizador();
        this.total = taxaEntrada().add(valorViagem).add(totalPenalizacoes);
    }

    /** Penalização cobrada mais tarde (dano reportado pelo condutor seguinte). */
    public void adicionarPenalizacao(BigDecimal valor) {
        this.totalPenalizacoes = totalPenalizacoes.add(valor).setScale(2, RoundingMode.HALF_UP);
        this.total = (total != null ? total : taxaEntrada()).add(valor);
    }

    /** Minutos já contados (para reservas ativas: até agora). */
    public long minutosAteAgora() {
        return minutosEntre(criadaEm, fimViagem != null ? fimViagem : Instant.now());
    }

    /** Valor da reserva ativa até agora: taxa de entrada + minutos × preço por minuto. */
    public BigDecimal valorAteAgora() {
        return taxaEntrada().add(precoMinuto().multiply(BigDecimal.valueOf(minutosAteAgora())))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void pararTemporizador() {
        this.fimViagem = Instant.now();
        this.minutos = minutosEntre(criadaEm, fimViagem);
        this.valorViagem = precoMinuto().multiply(BigDecimal.valueOf(minutos)).setScale(2, RoundingMode.HALF_UP);
    }

    public boolean ativa() {
        return estado == EstadoReserva.RESERVADA || estado == EstadoReserva.EM_CURSO;
    }

    public void pagamentoRegistado(boolean pago, String meio) {
        this.estadoPagamento = pago ? EstadoPagamento.PAGO : EstadoPagamento.EM_DIVIDA;
        this.pagamentoDescricao = meio;
    }

    public void faturaCriada(long documentoId, boolean finalizada, String pdfUrl) {
        this.moloniDocumentoId = documentoId;
        this.estadoFatura = finalizada ? EstadoFatura.EMITIDA : EstadoFatura.RASCUNHO;
        this.faturaPdfUrl = pdfUrl;
        this.faturaErro = null;
    }

    public void faturaFalhou(String erro) {
        this.estadoFatura = EstadoFatura.ERRO;
        this.faturaErro = erro != null && erro.length() > 2000 ? erro.substring(0, 2000) : erro;
    }

    /** Minutos arredondados para cima, no mínimo 1. */
    static long minutosEntre(Instant inicio, Instant fim) {
        long segundos = Duration.between(inicio, fim).getSeconds();
        return Math.max(1, (segundos + 59) / 60);
    }

    // Reservas criadas antes de existirem preços ficam com 0.
    private BigDecimal taxaEntrada() { return taxaEntrada != null ? taxaEntrada : BigDecimal.ZERO; }
    private BigDecimal precoMinuto() { return precoMinuto != null ? precoMinuto : BigDecimal.ZERO; }

    public Long getId() { return id; }
    public Utilizador getUtilizador() { return utilizador; }
    public Long getViaturaOdooId() { return viaturaOdooId; }
    public String getMatricula() { return matricula; }
    public EstadoReserva getEstado() { return estado; }
    public Instant getCriadaEm() { return criadaEm; }
    public Instant getInicioViagem() { return inicioViagem; }
    public Instant getFimViagem() { return fimViagem; }
    public BigDecimal getTaxaEntrada() { return taxaEntrada; }
    public BigDecimal getPrecoMinuto() { return precoMinuto; }
    public Long getMinutos() { return minutos; }
    public BigDecimal getValorViagem() { return valorViagem; }
    public BigDecimal getTotalPenalizacoes() { return totalPenalizacoes; }
    public BigDecimal getTotal() { return total; }
    public EstadoPagamento getEstadoPagamento() { return estadoPagamento != null ? estadoPagamento : EstadoPagamento.PENDENTE; }
    public String getPagamentoDescricao() { return pagamentoDescricao; }
    public EstadoFatura getEstadoFatura() { return estadoFatura != null ? estadoFatura : EstadoFatura.PENDENTE; }
    public Long getMoloniDocumentoId() { return moloniDocumentoId; }
    public String getFaturaPdfUrl() { return faturaPdfUrl; }
    public String getFaturaErro() { return faturaErro; }
}
