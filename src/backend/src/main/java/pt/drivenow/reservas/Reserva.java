package pt.drivenow.reservas;

import java.math.BigDecimal;
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

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPenalizacoes = BigDecimal.ZERO;

    protected Reserva() {
    }

    public Reserva(Utilizador utilizador, Long viaturaOdooId, String matricula) {
        this.utilizador = utilizador;
        this.viaturaOdooId = viaturaOdooId;
        this.matricula = matricula;
    }

    public void iniciarViagem() {
        this.estado = EstadoReserva.EM_CURSO;
        this.inicioViagem = Instant.now();
    }

    public void terminarViagem(BigDecimal penalizacoes) {
        this.estado = EstadoReserva.CONCLUIDA;
        this.fimViagem = Instant.now();
        this.totalPenalizacoes = penalizacoes;
    }

    public void cancelar() {
        this.estado = EstadoReserva.CANCELADA;
    }

    public Long getId() { return id; }
    public Utilizador getUtilizador() { return utilizador; }
    public Long getViaturaOdooId() { return viaturaOdooId; }
    public String getMatricula() { return matricula; }
    public EstadoReserva getEstado() { return estado; }
    public Instant getCriadaEm() { return criadaEm; }
    public Instant getInicioViagem() { return inicioViagem; }
    public Instant getFimViagem() { return fimViagem; }
    public BigDecimal getTotalPenalizacoes() { return totalPenalizacoes; }
}
