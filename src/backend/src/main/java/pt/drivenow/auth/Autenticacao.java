package pt.drivenow.auth;

import org.springframework.security.oauth2.jwt.Jwt;

/** Ajuda para obter o utilizador autenticado a partir do token. */
public final class Autenticacao {

    private Autenticacao() {
    }

    public static long utilizadorId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
