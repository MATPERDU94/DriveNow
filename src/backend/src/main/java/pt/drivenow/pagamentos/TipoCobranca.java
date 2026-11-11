package pt.drivenow.pagamentos;

public enum TipoCobranca {
    /** Taxa de entrada + minutos + penalizações do relato de fim. */
    VIAGEM,
    /** Penalização cobrada mais tarde ao condutor anterior (dano reportado no início da viagem seguinte). */
    PENALIZACAO
}
