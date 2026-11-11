package pt.drivenow.pagamentos;

public enum EstadoCobranca {
    PAGA,
    /** Pagamento recusado ou sem meio de pagamento: bloqueia novas reservas até ser pago. */
    EM_DIVIDA
}
