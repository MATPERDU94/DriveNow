package pt.drivenow.reservas;

import java.util.List;

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

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    public record CriarReservaRequest(@NotNull Long utilizadorId, @NotNull Long viaturaId) {
    }

    private final ReservaService reservas;

    public ReservaController(ReservaService reservas) {
        this.reservas = reservas;
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

    @PostMapping("/{id}/cancelar")
    public ReservaDto cancelar(@PathVariable long id) {
        return reservas.cancelar(id);
    }
}
