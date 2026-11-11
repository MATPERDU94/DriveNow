package pt.drivenow.pagamentos;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.stereotype.Component;

import pt.drivenow.faturacao.FaturacaoService;

/**
 * Depois de uma cobrança ser paga, emite a fatura-recibo no Moloni em segundo plano (diagrama 3:
 * primeiro o pagamento, depois a faturação). Cobranças em dívida só são faturadas quando forem pagas.
 */
@Component
public class PosPagamento {

    private final FaturacaoService faturacao;

    public PosPagamento(FaturacaoService faturacao) {
        this.faturacao = faturacao;
    }

    public void depoisDe(Cobranca c) {
        if (c == null || !c.paga()) {
            return;
        }
        Long reservaId = c.getReservaId();
        Long relatoId = c.getRelatoId();
        if (c.getTipo() == TipoCobranca.VIAGEM) {
            CompletableFuture.runAsync(() -> faturacao.faturarSemFalhar(reservaId));
        } else if (relatoId != null) {
            CompletableFuture.runAsync(() -> faturacao.faturarPenalizacoesSemFalhar(relatoId));
        }
    }

    public void depoisDe(List<Cobranca> pagas) {
        pagas.forEach(this::depoisDe);
    }
}
