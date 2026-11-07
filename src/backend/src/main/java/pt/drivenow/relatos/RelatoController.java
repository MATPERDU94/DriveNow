package pt.drivenow.relatos;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservas/{reservaId}/relatos")
public class RelatoController {

    /**
     * Exemplo:
     * {"tipo": "FIM", "observacoes": "Risco na porta", "latitude": 41.4525, "longitude": -8.2898,
     *  "danos": [{"tipoDanoId": 5, "descricao": "porta traseira"}]}
     * A latitude e a longitude (onde o carro ficou estacionado) são obrigatórias no relato de FIM.
     */
    public record CriarRelatoRequest(
            @NotNull TipoRelato tipo,
            String observacoes,
            List<RelatoService.DanoReportado> danos,
            Double latitude,
            Double longitude) {
    }

    private final RelatoService relatos;

    public RelatoController(RelatoService relatos) {
        this.relatos = relatos;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RelatoDto criar(@PathVariable long reservaId, @Valid @RequestBody CriarRelatoRequest pedido) {
        return relatos.criar(reservaId, pedido.tipo(), pedido.observacoes(), pedido.danos(),
                pedido.latitude(), pedido.longitude());
    }

    @GetMapping
    public List<RelatoDto> listar(@PathVariable long reservaId) {
        return relatos.daReserva(reservaId);
    }
}
