package pt.drivenow.utilizadores;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pt.drivenow.auth.Autenticacao;
import pt.drivenow.common.NaoEncontradoException;
import pt.drivenow.common.Nif;
import pt.drivenow.common.RegraNegocioException;
import pt.drivenow.pagamentos.Cobranca;
import pt.drivenow.pagamentos.CobrancaDto;
import pt.drivenow.pagamentos.MeioPagamentoDto;
import pt.drivenow.pagamentos.PagamentoService;
import pt.drivenow.pagamentos.PedidoMeioPagamento;
import pt.drivenow.pagamentos.PosPagamento;

/**
 * O utilizador autenticado (ecrã "Perfil"):
 * GET/PUT /api/me, PUT /api/me/pagamento, GET /api/me/cobrancas, POST /api/me/cobrancas/pagar.
 */
@RestController
@RequestMapping("/api/me")
public class UtilizadorController {

    public record PedidoDados(String nome, String email, String nif, String telefone) {
    }

    private final UtilizadorRepository utilizadores;
    private final PagamentoService pagamentos;
    private final PosPagamento posPagamento;

    public UtilizadorController(UtilizadorRepository utilizadores, PagamentoService pagamentos,
            PosPagamento posPagamento) {
        this.utilizadores = utilizadores;
        this.pagamentos = pagamentos;
        this.posPagamento = posPagamento;
    }

    @GetMapping
    public UtilizadorDto eu(@AuthenticationPrincipal Jwt jwt) {
        return dto(buscar(Autenticacao.utilizadorId(jwt)));
    }

    @PutMapping
    @Transactional
    public UtilizadorDto atualizar(@AuthenticationPrincipal Jwt jwt, @RequestBody PedidoDados p) {
        Utilizador u = buscar(Autenticacao.utilizadorId(jwt));
        if (p.nome() == null || p.nome().isBlank()) {
            throw new RegraNegocioException("Indique o nome completo");
        }
        if (p.email() == null || !p.email().trim().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new RegraNegocioException("Email inválido");
        }
        String email = p.email().trim().toLowerCase();
        if (!email.equalsIgnoreCase(u.getEmail()) && utilizadores.existsByEmailIgnoreCase(email)) {
            throw new RegraNegocioException("Já existe uma conta com o email " + email);
        }
        if (!Nif.valido(p.nif())) {
            throw new RegraNegocioException("NIF inválido (9 dígitos com dígito de controlo correto)");
        }
        String tel = p.telefone() == null || p.telefone().isBlank() ? null : p.telefone().replaceAll("\\s", "");
        if (tel != null && !tel.matches("\\+?\\d{9,15}")) {
            throw new RegraNegocioException("Telemóvel inválido");
        }
        u.atualizarDados(p.nome().trim(), email, p.nif(), tel);
        return dto(utilizadores.save(u));
    }

    /** Alterar o meio de pagamento; as dívidas pendentes são cobradas de novo de imediato. */
    @PutMapping("/pagamento")
    public UtilizadorDto alterarPagamento(@AuthenticationPrincipal Jwt jwt, @RequestBody PedidoMeioPagamento p) {
        long id = Autenticacao.utilizadorId(jwt);
        pagamentos.guardarMeio(id, p);
        List<Cobranca> pagas = pagamentos.pagarDividas(id);
        posPagamento.depoisDe(pagas);
        return dto(buscar(id));
    }

    @GetMapping("/cobrancas")
    public List<CobrancaDto> cobrancas(@AuthenticationPrincipal Jwt jwt) {
        return pagamentos.cobrancasDe(Autenticacao.utilizadorId(jwt));
    }

    /** Tentar pagar de novo as dívidas pendentes com o meio de pagamento atual. */
    @PostMapping("/cobrancas/pagar")
    public List<CobrancaDto> pagarDividas(@AuthenticationPrincipal Jwt jwt) {
        long id = Autenticacao.utilizadorId(jwt);
        List<Cobranca> pagas = pagamentos.pagarDividas(id);
        posPagamento.depoisDe(pagas);
        return pagamentos.cobrancasDe(id);
    }

    private Utilizador buscar(long id) {
        return utilizadores.findById(id).orElseThrow(() -> new NaoEncontradoException("Utilizador não existe"));
    }

    private UtilizadorDto dto(Utilizador u) {
        return UtilizadorDto.de(u, MeioPagamentoDto.de(pagamentos.meioDe(u.getId()).orElse(null)),
                pagamentos.valorEmDivida(u.getId()));
    }
}
