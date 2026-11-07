package pt.drivenow.odoo;

/** Erro ao comunicar com o Odoo. */
public class OdooException extends RuntimeException {

    public OdooException(String message, Throwable cause) {
        super(message, cause);
    }
}
