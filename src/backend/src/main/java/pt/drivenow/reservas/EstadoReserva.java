package pt.drivenow.reservas;

public enum EstadoReserva {
    /** Reservada, à espera do relato de início. */
    RESERVADA,
    /** Viagem a decorrer (depois do relato de início). */
    EM_CURSO,
    /** Viagem terminada (depois do relato de fim). */
    CONCLUIDA,
    CANCELADA
}
