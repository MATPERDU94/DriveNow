package pt.drivenow.reservas;

public enum EstadoPagamento {
    /** Ainda não cobrada (reserva ativa). */
    PENDENTE,
    PAGO,
    /** Cobrança recusada: o cliente tem de atualizar o meio de pagamento. */
    EM_DIVIDA
}
