package pt.drivenow.common;

/** Pedido que viola uma regra do negócio (ex.: viatura já reservada) → HTTP 409. */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String message) {
        super(message);
    }
}
