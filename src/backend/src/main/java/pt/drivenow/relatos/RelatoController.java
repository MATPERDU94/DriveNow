package pt.drivenow.relatos;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
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
import pt.drivenow.pagamentos.PagamentoService;
import pt.drivenow.pagamentos.PosPagamento;
import pt.drivenow.reservas.ReservaService;

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
    private final ReservaService reservas;
    private final PagamentoService pagamentos;
    private final PosPagamento posPagamento;

    public RelatoController(RelatoService relatos, ReservaService reservas, PagamentoService pagamentos,
            PosPagamento posPagamento) {
        this.relatos = relatos;
        this.reservas = reservas;
        this.pagamentos = pagamentos;
        this.posPagamento = posPagamento;
    }

    /**
     * Relato de INÍCIO: começa a viagem; danos novos são cobrados ao condutor anterior.
     * Relato de FIM: termina a viagem, cobra o valor ao meio de pagamento e depois emite a fatura (diagrama 3).
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RelatoDto criar(@AuthenticationPrincipal Jwt jwt, @PathVariable long reservaId,
            @Valid @RequestBody CriarRelatoRequest pedido) {
        reservas.verificarDono(reservaId, Autenticacao.utilizadorId(jwt));
        RelatoDto relato = relatos.criar(reservaId, pedido.tipo(), pedido.observacoes(), pedido.danos(),
                pedido.latitude(), pedido.longitude(), pedido.quilometragem());
        if (pedido.tipo() == TipoRelato.FIM) {
            // Primeiro o pagamento; a fatura só é emitida (em segundo plano) se o pagamento for aprovado.
            posPagamento.depoisDe(pagamentos.cobrarViagem(reservaId));
        } else {
            // Danos reportados no início e cobrados ao condutor anterior: cobrança e fatura à parte.
            posPagamento.depoisDe(pagamentos.cobrarPenalizacoes(relato.id()));
        }
        return relato;
    }

    @GetMapping
    public List<RelatoDto> listar(@AuthenticationPrincipal Jwt jwt, @PathVariable long reservaId) {
        reservas.verificarDono(reservaId, Autenticacao.utilizadorId(jwt));
        return relatos.daReserva(reservaId);
    }
}
