package pt.drivenow.auth;

import java.util.concurrent.CompletableFuture;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pt.drivenow.faturacao.FaturacaoService;

/**
 * POST /api/auth/registar: criar conta (dados pessoais + meio de pagamento) e devolver o token.
 * POST /api/auth/login: email + password → token.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record PedidoLogin(String email, String password) {
    }

    private final AuthService auth;
    private final FaturacaoService faturacao;

    public AuthController(AuthService auth, FaturacaoService faturacao) {
        this.auth = auth;
        this.faturacao = faturacao;
    }

    @PostMapping("/registar")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthService.Sessao registar(@RequestBody AuthService.PedidoRegisto pedido) {
        AuthService.Sessao sessao = auth.registar(pedido);
        // Diagrama 1: criar também o cliente no Moloni (em segundo plano; se falhar, é criado na 1.ª fatura).
        long id = sessao.utilizador().id();
        CompletableFuture.runAsync(() -> faturacao.registarClienteSemFalhar(id));
        return sessao;
    }

    @PostMapping("/login")
    public AuthService.Sessao login(@RequestBody PedidoLogin pedido) {
        return auth.login(pedido.email(), pedido.password());
    }
}
