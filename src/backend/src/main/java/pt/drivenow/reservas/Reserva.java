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
 * Preço: taxa de entrada (cobrada ao reservar) + minutos de viagem × preço por minuto + penalizações.
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

    /** Minutos de viagem (cada minuto começado conta). */
    private Long minutos;

    /** minutos × preço por minuto. */
    @Column(precision = 10, scale = 2)
    private BigDecimal valorViagem;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPenalizacoes = BigDecimal.ZERO;

    /** taxa de entrada + valor da viagem + penalizações. */
    @Column(precision = 10, scale = 2)
    private BigDecimal total;

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
        this.fimViagem = Instant.now();
        this.minutos = minutosEntre(inicioViagem, fimViagem);
        this.valorViagem = precoMinuto().multiply(BigDecimal.valueOf(minutos)).setScale(2, RoundingMode.HALF_UP);
        this.totalPenalizacoes = penalizacoes.setScale(2, RoundingMode.HALF_UP);
        this.total = taxaEntrada().add(valorViagem).add(totalPenalizacoes);
    }

    public void cancelar() {
        this.estado = EstadoReserva.CANCELADA;
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
}
