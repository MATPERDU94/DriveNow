package pt.drivenow.reservas;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pt.drivenow.faturacao.FaturacaoService;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    public record CriarReservaRequest(@NotNull Long utilizadorId, @NotNull Long viaturaId) {
    }

    private final ReservaService reservas;
    private final FaturacaoService faturacao;

    public ReservaController(ReservaService reservas, FaturacaoService faturacao) {
        this.reservas = reservas;
        this.faturacao = faturacao;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservaDto criar(@Valid @RequestBody CriarReservaRequest pedido) {
        return reservas.criar(pedido.utilizadorId(), pedido.viaturaId());
    }

    /** GET /api/reservas?utilizadorId=1: histórico de reservas do utilizador. */
    @GetMapping
    public List<ReservaDto> doUtilizador(@RequestParam long utilizadorId) {
        return reservas.doUtilizador(utilizadorId);
    }

    @GetMapping("/{id}")
    public ReservaDto obter(@PathVariable long id) {
        return reservas.obter(id);
    }

    /** Emite (ou volta a tentar emitir) a fatura-recibo no Moloni de uma viagem concluída. */
    @PostMapping("/{id}/faturar")
    public ReservaDto faturar(@PathVariable long id) {
        return faturacao.faturar(id);
    }

    /** Cancelar: o temporizador para e a reserva é faturada (taxa de entrada + minutos decorridos). */
    @PostMapping("/{id}/cancelar")
    public ReservaDto cancelar(@PathVariable long id) {
        ReservaDto cancelada = reservas.cancelar(id);
        CompletableFuture.runAsync(() -> faturacao.faturarSemFalhar(id));
        return cancelada;
    }
}
