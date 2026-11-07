package pt.drivenow.localizacao;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Última posição GPS conhecida de uma viatura (id = id da viatura no Odoo). */
@Entity
@Table(name = "posicao_viatura")
public class PosicaoViatura {

    @Id
    private Long viaturaOdooId;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private Instant atualizadaEm = Instant.now();

    protected PosicaoViatura() {
    }

    public PosicaoViatura(Long viaturaOdooId, double latitude, double longitude) {
        this.viaturaOdooId = viaturaOdooId;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void mover(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.atualizadaEm = Instant.now();
    }

    public Long getViaturaOdooId() { return viaturaOdooId; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public Instant getAtualizadaEm() { return atualizadaEm; }
}
