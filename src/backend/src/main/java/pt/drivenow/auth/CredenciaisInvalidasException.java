package pt.drivenow.auth;

/** Email ou password errados → HTTP 401. */
public class CredenciaisInvalidasException extends RuntimeException {

    public CredenciaisInvalidasException() {
        super("Email ou password incorretos");
    }
}
