package pt.drivenow.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import pt.drivenow.utilizadores.Utilizador;

/** Gera o token de login (JWT): o "subject" é o id do utilizador. */
@Service
public class TokenService {

    public record Token(String valor, Instant expiraEm) { }

    private final JwtEncoder encoder;
    private final long horas;

    public TokenService(JwtEncoder encoder, @Value("${drivenow.jwt-horas:24}") long horas) {
        this.encoder = encoder;
        this.horas = horas;
    }

    public Token gerar(Utilizador u) {
        Instant agora = Instant.now();
        Instant expira = agora.plus(horas, ChronoUnit.HOURS);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("drivenow")
                .issuedAt(agora)
                .expiresAt(expira)
                .subject(String.valueOf(u.getId()))
                .claim("email", u.getEmail())
                .claim("nome", u.getNome())
                .build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        String valor = encoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
        return new Token(valor, expira);
    }
}
