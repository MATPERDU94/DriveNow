package pt.drivenow.relatos;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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

    /** Penalização cobrada ao cliente (0 se o dano já existia ou se é um relato de início). */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal penalizacao = BigDecimal.ZERO;

    /** Id do registo criado no Odoo (fleet.vehicle.log.services). */
    private Long odooRegistoId;

    protected RelatoDano() {
    }

    public RelatoDano(Long tipoDanoOdooId, String nomeTipo, String descricao, BigDecimal penalizacao, Long odooRegistoId) {
        this.tipoDanoOdooId = tipoDanoOdooId;
        this.nomeTipo = nomeTipo;
        this.descricao = descricao;
        this.penalizacao = penalizacao;
        this.odooRegistoId = odooRegistoId;
    }

    void setRelato(Relato relato) { this.relato = relato; }

    public Long getId() { return id; }
    public Long getTipoDanoOdooId() { return tipoDanoOdooId; }
    public String getNomeTipo() { return nomeTipo; }
    public String getDescricao() { return descricao; }
    public BigDecimal getPenalizacao() { return penalizacao; }
    public Long getOdooRegistoId() { return odooRegistoId; }
}
