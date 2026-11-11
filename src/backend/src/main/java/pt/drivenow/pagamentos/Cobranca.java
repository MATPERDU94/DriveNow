package pt.drivenow.pagamentos;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Uma cobrança ao meio de pagamento do cliente (simulada). */
@Entity
@Table(name = "cobranca")
public class Cobranca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long utilizadorId;

    /** Reserva a que a cobrança diz respeito. */
    @Column(nullable = false)
    private Long reservaId;

    /** Relato que originou a penalização (só nas cobranças PENALIZACAO). */
    private Long relatoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoCobranca tipo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCobranca estado = EstadoCobranca.EM_DIVIDA;

    private String meioDescricao;

    private String motivoRecusa;

    private int tentativas;

    @Column(nullable = false)
    private Instant criadaEm = Instant.now();

    private Instant pagaEm;

    protected Cobranca() {
    }

    public Cobranca(Long utilizadorId, Long reservaId, Long relatoId, TipoCobranca tipo, BigDecimal valor) {
        this.utilizadorId = utilizadorId;
        this.reservaId = reservaId;
        this.relatoId = relatoId;
        this.tipo = tipo;
        this.valor = valor;
    }

    void aprovada(String meio) {
        this.tentativas++;
        this.estado = EstadoCobranca.PAGA;
        this.meioDescricao = meio;
        this.motivoRecusa = null;
        this.pagaEm = Instant.now();
    }

    void recusada(String meio, String motivo) {
        this.tentativas++;
        this.estado = EstadoCobranca.EM_DIVIDA;
        this.meioDescricao = meio;
        this.motivoRecusa = motivo;
    }

    public boolean paga() {
        return estado == EstadoCobranca.PAGA;
    }

    public Long getId() { return id; }
    public Long getUtilizadorId() { return utilizadorId; }
    public Long getReservaId() { return reservaId; }
    public Long getRelatoId() { return relatoId; }
    public TipoCobranca getTipo() { return tipo; }
    public BigDecimal getValor() { return valor; }
    public EstadoCobranca getEstado() { return estado; }
    public String getMeioDescricao() { return meioDescricao; }
    public String getMotivoRecusa() { return motivoRecusa; }
    public int getTentativas() { return tentativas; }
    public Instant getCriadaEm() { return criadaEm; }
    public Instant getPagaEm() { return pagaEm; }
}
