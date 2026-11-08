package pt.drivenow.reservas;

public enum EstadoFatura {
    /** Viagem ainda não terminada ou fatura ainda não emitida. */
    PENDENTE,
    /** Fatura-recibo criada no Moloni em rascunho (sem valor fiscal, sem PDF). */
    RASCUNHO,
    /** Fatura-recibo finalizada no Moloni, com PDF. */
    EMITIDA,
    /** A emissão falhou; ver faturaErro. Pode tentar-se de novo com POST /api/reservas/{id}/faturar. */
    ERRO
}
