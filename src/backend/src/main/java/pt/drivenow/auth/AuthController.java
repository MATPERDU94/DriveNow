package pt.drivenow.auth;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pt.drivenow.faturacao.FaturacaoService;
import pt.drivenow.n8n.N8nClient;

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
    private final N8nClient n8n;

    public AuthController(AuthService auth, FaturacaoService faturacao, N8nClient n8n) {
        this.auth = auth;
        this.faturacao = faturacao;
        this.n8n = n8n;
    }

    @PostMapping("/registar")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthService.Sessao registar(@RequestBody AuthService.PedidoRegisto pedido) {
        AuthService.Sessao sessao = auth.registar(pedido);
        // Diagrama 1: criar também o cliente no Moloni (em segundo plano; se falhar, é criado na 1.ª fatura).
        long id = sessao.utilizador().id();
        CompletableFuture.runAsync(() -> faturacao.registarClienteSemFalhar(id));
        // Diagrama 1: evento "cliente registado" para o n8n, que cria o cliente (Business Partner) no SAP.
        Map<String, Object> evento = eventoClienteRegistado(id, pedido);
        CompletableFuture.runAsync(() -> n8n.enviar("cliente-registado", evento));
        return sessao;
    }

    private static Map<String, Object> eventoClienteRegistado(long id, AuthService.PedidoRegisto p) {
        String nome = p.nome().trim().replaceAll("\\s+", " ");
        int espaco = nome.indexOf(' ');
        Map<String, Object> evento = new LinkedHashMap<>();
        evento.put("utilizadorId", id);
        evento.put("nome", nome);
        evento.put("primeiroNome", espaco < 0 ? nome : nome.substring(0, espaco));
        evento.put("apelido", espaco < 0 ? nome : nome.substring(espaco + 1));
        evento.put("email", p.email().trim().toLowerCase());
        evento.put("nif", p.nif());
        evento.put("telefone", p.telefone());
        return evento;
    }

    @PostMapping("/login")
    public AuthService.Sessao login(@RequestBody PedidoLogin pedido) {
        return auth.login(pedido.email(), pedido.password());
    }
}
