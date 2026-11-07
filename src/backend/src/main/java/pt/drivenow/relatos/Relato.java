package pt.drivenow.relatos;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import pt.drivenow.reservas.Reserva;

/** Relato do estado do carro feito pelo cliente no início ou no fim da viagem. */
@Entity
@Table(name = "relato", uniqueConstraints = @UniqueConstraint(columnNames = {"reserva_id", "tipo"}))
public class Relato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoRelato tipo;

    @Column(length = 2000)
    private String observacoes;

    @Column(nullable = false)
    private Instant criadoEm = Instant.now();

    @OneToMany(mappedBy = "relato", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RelatoDano> danos = new ArrayList<>();

    protected Relato() {
    }

    public Relato(Reserva reserva, TipoRelato tipo, String observacoes) {
        this.reserva = reserva;
        this.tipo = tipo;
        this.observacoes = observacoes;
    }

    public void adicionarDano(RelatoDano dano) {
        dano.setRelato(this);
        danos.add(dano);
    }

    public Long getId() { return id; }
    public Reserva getReserva() { return reserva; }
    public TipoRelato getTipo() { return tipo; }
    public String getObservacoes() { return observacoes; }
    public Instant getCriadoEm() { return criadoEm; }
    public List<RelatoDano> getDanos() { return danos; }
}
