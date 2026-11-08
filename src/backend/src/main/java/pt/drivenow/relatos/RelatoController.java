package pt.drivenow.relatos;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pt.drivenow.faturacao.FaturacaoService;

@RestController
@RequestMapping("/api/reservas/{reservaId}/relatos")
public class RelatoController {

    /**
     * Exemplo:
     * {"tipo": "FIM", "observacoes": "Risco na porta", "latitude": 41.4525, "longitude": -8.2898, "quilometragem": 3912,
     *  "danos": [{"tipoDanoId": 5, "descricao": "porta traseira"}]}
     * A latitude e a longitude (onde o carro ficou estacionado) são obrigatórias no relato de FIM.
     * A quilometragem é opcional: se vier, é registada no Odoo.
     */
    public record CriarRelatoRequest(
            @NotNull TipoRelato tipo,
            String observacoes,
            List<RelatoService.DanoReportado> danos,
            Double latitude,
            Double longitude,
            Double quilometragem) {
    }

    private final RelatoService relatos;
    private final FaturacaoService faturacao;

    public RelatoController(RelatoService relatos, FaturacaoService faturacao) {
        this.relatos = relatos;
        this.faturacao = faturacao;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RelatoDto criar(@PathVariable long reservaId, @Valid @RequestBody CriarRelatoRequest pedido) {
        RelatoDto relato = relatos.criar(reservaId, pedido.tipo(), pedido.observacoes(), pedido.danos(),
                pedido.latitude(), pedido.longitude(), pedido.quilometragem());
        if (pedido.tipo() == TipoRelato.FIM) {
            // A fatura é emitida em segundo plano, para a app não esperar pelo Moloni.
            // A viagem já ficou terminada e guardada; se o Moloni falhar, a fatura fica em ERRO
            // e pode ser emitida mais tarde com POST /api/reservas/{id}/faturar.
            CompletableFuture.runAsync(() -> faturacao.faturarSemFalhar(reservaId));
        } else {
            // Danos reportados no início e cobrados ao condutor anterior: fatura à parte.
            Long relatoId = relato.id();
            CompletableFuture.runAsync(() -> faturacao.faturarPenalizacoesSemFalhar(relatoId));
        }
        return relato;
    }

    @GetMapping
    public List<RelatoDto> listar(@PathVariable long reservaId) {
        return relatos.daReserva(reservaId);
    }
}
