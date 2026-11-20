package pt.drivenow.trajetos;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Trajeto de uma viagem (diagrama 2b): o percurso pelas ruas entre o sítio onde o carro estava
 * na reserva e o sítio onde o cliente o deixou, pedido ao serviço de rotas no fim da viagem.
 * É um trajeto simulado: a DriveNow não segue o carro por GPS durante a viagem.
 */
@Entity
@Table(name = "trajeto")
public class Trajeto {

    /** Uma reserva tem no máximo um trajeto. */
    @Id
    private Long reservaId;

    /** Pontos do percurso em JSON: [[latitude, longitude], ...]. */
    @Column(nullable = false, columnDefinition = "text")
    private String pontos;

    private Double distanciaKm;

    private Double duracaoMin;

    @Column(nullable = false)
    private Instant criadoEm = Instant.now();

    protected Trajeto() {
    }

    public Trajeto(Long reservaId, String pontos, double distanciaKm, double duracaoMin) {
        this.reservaId = reservaId;
        this.pontos = pontos;
        this.distanciaKm = distanciaKm;
        this.duracaoMin = duracaoMin;
    }

    public Long getReservaId() { return reservaId; }
    public String getPontos() { return pontos; }
    public Double getDistanciaKm() { return distanciaKm; }
    public Double getDuracaoMin() { return duracaoMin; }
    public Instant getCriadoEm() { return criadoEm; }
}
