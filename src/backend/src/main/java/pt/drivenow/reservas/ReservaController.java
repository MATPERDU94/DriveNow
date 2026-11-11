package pt.drivenow.reservas;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pt.drivenow.auth.Autenticacao;
import pt.drivenow.faturacao.FaturacaoService;
import pt.drivenow.pagamentos.PagamentoService;
import pt.drivenow.pagamentos.PosPagamento;

/** Reservas do utilizador autenticado (o utilizador vem do token). */
@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    public record CriarReservaRequest(@NotNull Long viaturaId) {
    }

    private final ReservaService reservas;
    private final FaturacaoService faturacao;
    private final PagamentoService pagamentos;
    private final PosPagamento posPagamento;

    public ReservaController(ReservaService reservas, FaturacaoService faturacao, PagamentoService pagamentos,
            PosPagamento posPagamento) {
        this.reservas = reservas;
        this.faturacao = faturacao;
        this.pagamentos = pagamentos;
        this.posPagamento = posPagamento;
    }

    /** POST /api/reservas {"viaturaId": 35}: reservar; o temporizador começa já. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaDto criar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CriarReservaRequest pedido) {
        return reservas.criar(Autenticacao.utilizadorId(jwt), pedido.viaturaId());
    }

    /** Histórico de reservas e viagens (ecrã "Viagens e faturas"). */
    @GetMapping
    public List<ReservaDto> minhas(@AuthenticationPrincipal Jwt jwt) {
        return reservas.doUtilizador(Autenticacao.utilizadorId(jwt));
    }

    /** Reserva ativa (ecrã "Em viagem"); 204 se não houver. */
    @GetMapping("/ativa")
    public ResponseEntity<ReservaDto> ativa(@AuthenticationPrincipal Jwt jwt) {
        ReservaDto r = reservas.ativa(Autenticacao.utilizadorId(jwt));
        return r == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(r);
    }

    @GetMapping("/{id}")
    public ReservaDto obter(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return reservas.obterDoUtilizador(id, Autenticacao.utilizadorId(jwt));
    }

    /** Emite (ou volta a tentar emitir) a fatura-recibo no Moloni de uma viagem concluída. */
    @PostMapping("/{id}/faturar")
    public ReservaDto faturar(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        reservas.verificarDono(id, Autenticacao.utilizadorId(jwt));
        return faturacao.faturar(id);
    }

    /** Cancelar: o temporizador para, cobra-se a taxa de entrada e os minutos decorridos, e fatura-se. */
    @PostMapping("/{id}/cancelar")
    public ReservaDto cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        long utilizadorId = Autenticacao.utilizadorId(jwt);
        reservas.cancelar(id, utilizadorId);
        posPagamento.depoisDe(pagamentos.cobrarViagem(id));
        return reservas.obterDoUtilizador(id, utilizadorId);
    }
}
