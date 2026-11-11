package pt.drivenow.auth;

import java.math.BigDecimal;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.drivenow.common.Nif;
import pt.drivenow.common.RegraNegocioException;
import pt.drivenow.pagamentos.MeioPagamento;
import pt.drivenow.pagamentos.MeioPagamentoDto;
import pt.drivenow.pagamentos.PagamentoService;
import pt.drivenow.pagamentos.PedidoMeioPagamento;
import pt.drivenow.utilizadores.Utilizador;
import pt.drivenow.utilizadores.UtilizadorDto;
import pt.drivenow.utilizadores.UtilizadorRepository;

/** Criar conta e iniciar sessão (diagrama 1 e ecrãs "Criar conta" / "Iniciar sessão"). */
@Service
public class AuthService {

    public record PedidoRegisto(
            String nome,
            String email,
            String nif,
            String telefone,
            String password,
            boolean aceitaTermos,
            PedidoMeioPagamento pagamento) {
    }

    public record Sessao(String token, java.time.Instant expiraEm, UtilizadorDto utilizador) {
    }

    private final UtilizadorRepository utilizadores;
    private final PagamentoService pagamentos;
    private final PasswordEncoder passwords;
    private final TokenService tokens;

    public AuthService(UtilizadorRepository utilizadores, PagamentoService pagamentos, PasswordEncoder passwords,
            TokenService tokens) {
        this.utilizadores = utilizadores;
        this.pagamentos = pagamentos;
        this.passwords = passwords;
        this.tokens = tokens;
    }

    @Transactional
    public Sessao registar(PedidoRegisto p) {
        validarDados(p.nome(), p.email(), p.nif(), p.telefone());
        if (utilizadores.existsByEmailIgnoreCase(p.email().trim())) {
            throw new RegraNegocioException("Já existe uma conta com o email " + p.email());
        }
        if (p.password() == null || p.password().length() < 8) {
            throw new RegraNegocioException("A password tem de ter pelo menos 8 caracteres");
        }
        if (!p.aceitaTermos()) {
            throw new RegraNegocioException("É preciso aceitar os termos e a política de privacidade");
        }
        if (p.pagamento() == null) {
            throw new RegraNegocioException("Associe um cartão ou uma conta bancária");
        }

        Utilizador u = new Utilizador(p.nome().trim(), p.email().trim().toLowerCase(), p.nif(), limpar(p.telefone()));
        u.definirPassword(passwords.encode(p.password()));
        u.aceitarTermos();
        u = utilizadores.save(u);
        MeioPagamento meio = pagamentos.guardarMeio(u.getId(), p.pagamento());

        TokenService.Token t = tokens.gerar(u);
        return new Sessao(t.valor(), t.expiraEm(), UtilizadorDto.de(u, MeioPagamentoDto.de(meio), BigDecimal.ZERO));
    }

    @Transactional(readOnly = true)
    public Sessao login(String email, String password) {
        Utilizador u = utilizadores.findByEmailIgnoreCase(email == null ? "" : email.trim())
                .orElseThrow(CredenciaisInvalidasException::new);
        if (u.getPasswordHash() == null || password == null || !passwords.matches(password, u.getPasswordHash())) {
            throw new CredenciaisInvalidasException();
        }
        TokenService.Token t = tokens.gerar(u);
        MeioPagamentoDto meio = MeioPagamentoDto.de(pagamentos.meioDe(u.getId()).orElse(null));
        return new Sessao(t.valor(), t.expiraEm(), UtilizadorDto.de(u, meio, pagamentos.valorEmDivida(u.getId())));
    }

    /** Validações comuns ao registo e à edição do perfil. */
    static void validarDados(String nome, String email, String nif, String telefone) {
        if (nome == null || nome.isBlank()) {
            throw new RegraNegocioException("Indique o nome completo");
        }
        if (email == null || !email.trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new RegraNegocioException("Email inválido");
        }
        if (!Nif.valido(nif)) {
            throw new RegraNegocioException("NIF inválido (9 dígitos com dígito de controlo correto)");
        }
        String tel = limpar(telefone);
        if (tel != null && !tel.matches("\\+?\\d{9,15}")) {
            throw new RegraNegocioException("Telemóvel inválido");
        }
    }

    static String limpar(String telefone) {
        return telefone == null || telefone.isBlank() ? null : telefone.replaceAll("\\s", "");
    }
}
