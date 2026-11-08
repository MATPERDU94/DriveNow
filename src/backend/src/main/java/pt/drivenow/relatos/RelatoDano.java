package pt.drivenow.relatos;

import java.math.BigDecimal;

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

import pt.drivenow.reservas.EstadoFatura;

/** Um dano assinalado num relato. */
@Entity
@Table(name = "relato_dano")
public class RelatoDano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "relato_id")
    private Relato relato;

    /** Id do tipo de dano no Odoo (fleet.service.type). */
    @Column(nullable = false)
    private Long tipoDanoOdooId;

    private String nomeTipo;

    @Column(length = 1000)
    private String descricao;

    /** Penalização cobrada ao responsável (0 se o dano já estava registado ou não há responsável). */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal penalizacao = BigDecimal.ZERO;

    /** Reserva a quem a penalização é cobrada (a própria, no fim; a do condutor anterior, no início). */
    private Long reservaResponsavelId;

    /** Explicação do tratamento dado ao dano (ex.: "Já registado: ignorado"). */
    @Column(length = 500)
    private String nota;

    /** Id do registo criado no Odoo (fleet.vehicle.log.services); null se o dano já estava registado. */
    private Long odooRegistoId;

    /**
     * Faturação das penalizações cobradas ao condutor anterior (fatura própria).
     * As do relato de fim vão na fatura da própria viagem.
     */
    @Enumerated(EnumType.STRING)
    private EstadoFatura estadoFatura;

    private Long moloniDocumentoId;

    @Column(length = 2000)
    private String faturaErro;

    protected RelatoDano() {
    }

    public RelatoDano(Long tipoDanoOdooId, String nomeTipo, String descricao, BigDecimal penalizacao,
            Long odooRegistoId, Long reservaResponsavelId, String nota) {
        this.tipoDanoOdooId = tipoDanoOdooId;
        this.nomeTipo = nomeTipo;
        this.descricao = descricao;
        this.penalizacao = penalizacao;
        this.odooRegistoId = odooRegistoId;
        this.reservaResponsavelId = reservaResponsavelId;
        this.nota = nota;
    }

    void setRelato(Relato relato) { this.relato = relato; }

    public void faturaCriada(long documentoId, boolean finalizada) {
        this.moloniDocumentoId = documentoId;
        this.estadoFatura = finalizada ? EstadoFatura.EMITIDA : EstadoFatura.RASCUNHO;
        this.faturaErro = null;
    }

    public void faturaFalhou(String erro) {
        this.estadoFatura = EstadoFatura.ERRO;
        this.faturaErro = erro != null && erro.length() > 2000 ? erro.substring(0, 2000) : erro;
    }

    public boolean temPenalizacao() {
        return penalizacao != null && penalizacao.signum() > 0;
    }

    public Long getId() { return id; }
    public Long getTipoDanoOdooId() { return tipoDanoOdooId; }
    public String getNomeTipo() { return nomeTipo; }
    public String getDescricao() { return descricao; }
    public BigDecimal getPenalizacao() { return penalizacao; }
    public Long getReservaResponsavelId() { return reservaResponsavelId; }
    public String getNota() { return nota; }
    public Long getOdooRegistoId() { return odooRegistoId; }
    public EstadoFatura getEstadoFatura() { return estadoFatura; }
    public Long getMoloniDocumentoId() { return moloniDocumentoId; }
    public String getFaturaErro() { return faturaErro; }
}
