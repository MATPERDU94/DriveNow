package pt.drivenow.common;

/** Recurso inexistente → HTTP 404. */
public class NaoEncontradoException extends RuntimeException {

    public NaoEncontradoException(String message) {
        super(message);
    }
}
