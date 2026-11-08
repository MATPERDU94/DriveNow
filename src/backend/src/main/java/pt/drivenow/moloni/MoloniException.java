package pt.drivenow.moloni;

/** Erro ao comunicar com o Moloni. */
public class MoloniException extends RuntimeException {

    public MoloniException(String message) {
        super(message);
    }

    public MoloniException(String message, Throwable cause) {
        super(message, cause);
    }
}
