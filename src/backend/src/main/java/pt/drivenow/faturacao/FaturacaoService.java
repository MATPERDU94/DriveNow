package pt.drivenow.faturacao;

import static pt.drivenow.moloni.MoloniClient.params;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import pt.drivenow.common.NaoEncontradoException;
import pt.drivenow.common.Nif;
import pt.drivenow.common.RegraNegocioException;
import pt.drivenow.moloni.MoloniClient;
import pt.drivenow.moloni.MoloniException;
import pt.drivenow.relatos.Relato;
import pt.drivenow.relatos.RelatoDano;
import pt.drivenow.relatos.RelatoRepository;
import pt.drivenow.relatos.TipoRelato;
import pt.drivenow.reservas.EstadoFatura;
import pt.drivenow.reservas.EstadoReserva;
import pt.drivenow.reservas.Reserva;
import pt.drivenow.reservas.ReservaDto;
import pt.drivenow.reservas.ReservaRepository;
import pt.drivenow.utilizadores.Utilizador;
import pt.drivenow.utilizadores.UtilizadorRepository;

/**
 * Emite no Moloni a fatura-recibo de uma viagem terminada:
 * taxa de entrada + minutos de viagem + uma linha por penalização de dano.
 *
 * Os preços do tarifário incluem IVA; no Moloni as linhas levam o preço sem IVA.
 * Os artigos têm de existir no Moloni com as referências DN-TAXA, DN-MIN e DN-DANO.
 */
@Service
public class FaturacaoService {

    private static final Logger log = LoggerFactory.getLogger(FaturacaoService.class);

    static final String REF_TAXA = "DN-TAXA";
    static final String REF_MINUTO = "DN-MIN";
    static final String REF_DANO = "DN-DANO";
    /** NIF genérico de "consumidor final", usado quando o cliente não indicou NIF. */
    static final String NIF_CONSUMIDOR_FINAL = "999999990";
    private static final ZoneId LISBOA = ZoneId.of("Europe/Lisbon");

    private record Artigo(long id, String nome, List<Long> impostos) { }

    private final MoloniClient moloni;
    private final ReservaRepository reservas;
    private final RelatoRepository relatos;
    private final UtilizadorRepository utilizadores;

    // Configuração da conta Moloni, lida uma vez e guardada em memória.
    private Long empresaId;
    private Long serieId;
    private Long vencimentoId;
    private Long metodoPagamentoId;
    private final Map<String, Artigo> artigos = new HashMap<>();

    public FaturacaoService(MoloniClient moloni, ReservaRepository reservas, RelatoRepository relatos,
            UtilizadorRepository utilizadores) {
        this.moloni = moloni;
        this.reservas = reservas;
        this.relatos = relatos;
        this.utilizadores = utilizadores;
    }

    /** Emite a fatura; em caso de erro, regista-o na reserva e lança a exceção. */
    public ReservaDto faturar(long reservaId) {
        Reserva r = reservas.comUtilizador(reservaId)
                .orElseThrow(() -> new NaoEncontradoException("Reserva " + reservaId + " não existe"));
        if (r.getEstado() != EstadoReserva.CONCLUIDA && r.getEstado() != EstadoReserva.CANCELADA) {
            throw new RegraNegocioException("Só se faturam viagens concluídas ou canceladas (estado atual: "
                    + r.getEstado() + ")");
        }
        if (r.getEstadoFatura() == EstadoFatura.RASCUNHO || r.getEstadoFatura() == EstadoFatura.EMITIDA) {
            return ReservaDto.de(r);
        }
        try {
            emitir(r);
        } catch (RuntimeException e) {
            r.faturaFalhou(e.getMessage());
            reservas.save(r);
            throw e;
        }
        reservas.save(r);
        return ReservaDto.de(r);
    }

    /** Usado no fim da viagem: se a faturação falhar, a viagem termina na mesma. */
    public void faturarSemFalhar(long reservaId) {
        try {
            faturar(reservaId);
        } catch (RuntimeException e) {
            log.warn("Fatura da reserva {} não emitida: {}", reservaId, e.getMessage());
        }
    }

    /**
     * Penalizações cobradas ao condutor anterior (danos reportados no relato de início):
     * uma fatura-recibo à parte para esse cliente (diagrama 4).
     */
    public void faturarPenalizacoes(long relatoId) {
        Relato relato = relatos.comDanosPorId(relatoId)
                .orElseThrow(() -> new NaoEncontradoException("Relato " + relatoId + " não existe"));
        List<RelatoDano> porFaturar = relato.getDanos().stream()
                .filter(d -> d.temPenalizacao() && d.getReservaResponsavelId() != null)
                .filter(d -> d.getEstadoFatura() == null || d.getEstadoFatura() == EstadoFatura.ERRO
                        || d.getEstadoFatura() == EstadoFatura.PENDENTE)
                .toList();
        if (porFaturar.isEmpty()) {
            return;
        }
        long responsavelId = porFaturar.get(0).getReservaResponsavelId();
        Reserva responsavel = reservas.comUtilizador(responsavelId)
                .orElseThrow(() -> new NaoEncontradoException("Reserva " + responsavelId + " não existe"));
        try {
            Documento doc;
            synchronized (this) {
                carregarConfiguracao();
                double iva = moloni.config().iva();
                List<Map<String, Object>> linhas = new ArrayList<>();
                for (RelatoDano d : porFaturar) {
                    linhas.add(linha(artigo(REF_DANO), "Penalização: " + d.getNomeTipo() + " - "
                            + responsavel.getMatricula() + " (reportado na viagem seguinte)", 1, d.getPenalizacao(), iva));
                }
                doc = emitirDocumento(responsavel.getUtilizador(), linhas,
                        "DriveNow penalização reserva #" + responsavel.getId());
            }
            porFaturar.forEach(d -> d.faturaCriada(doc.id(), doc.finalizado()));
        } catch (RuntimeException e) {
            porFaturar.forEach(d -> d.faturaFalhou(e.getMessage()));
            relatos.save(relato);
            throw e;
        }
        relatos.save(relato);
    }

    public void faturarPenalizacoesSemFalhar(long relatoId) {
        try {
            faturarPenalizacoes(relatoId);
        } catch (RuntimeException e) {
            log.warn("Fatura das penalizações do relato {} não emitida: {}", relatoId, e.getMessage());
        }
    }

    private record Documento(long id, boolean finalizado, String pdf) { }

    private synchronized void emitir(Reserva r) {
        carregarConfiguracao();
        double iva = moloni.config().iva();

        List<Map<String, Object>> linhas = new ArrayList<>();
        if (r.getTaxaEntrada() != null && r.getTaxaEntrada().signum() > 0) {
            linhas.add(linha(artigo(REF_TAXA), "Taxa de entrada - " + r.getMatricula(), 1, r.getTaxaEntrada(), iva));
        }
        String descricao = (r.getEstado() == EstadoReserva.CANCELADA ? "Reserva cancelada " : "Viagem ")
                + r.getMatricula() + " (" + r.getMinutos() + " min)";
        linhas.add(linha(artigo(REF_MINUTO), descricao,
                r.getMinutos() == null ? 0 : r.getMinutos(), r.getPrecoMinuto(), iva));
        Relato fim = relatos.comDanos(r.getId(), TipoRelato.FIM).orElse(null);
        if (fim != null) {
            for (RelatoDano d : fim.getDanos()) {
                if (d.temPenalizacao()) {
                    linhas.add(linha(artigo(REF_DANO), "Penalização: " + d.getNomeTipo(), 1, d.getPenalizacao(), iva));
                }
            }
        }
        Documento doc = emitirDocumento(r.getUtilizador(), linhas, "DriveNow reserva #" + r.getId());
        r.faturaCriada(doc.id(), doc.finalizado(), doc.pdf());
    }

    private Documento emitirDocumento(Utilizador cliente, List<Map<String, Object>> linhas, String referencia) {
        long clienteId = garantirCliente(cliente);
        boolean fechar = moloni.config().fecharDocumentos();
        String hoje = LocalDate.now(LISBOA).toString();
        JsonNode criado = moloni.insert("invoiceReceipts/insert", params(
                "company_id", empresaId,
                "date", hoje,
                "expiration_date", hoje,
                "document_set_id", serieId,
                "customer_id", clienteId,
                "our_reference", referencia,
                "products", linhas,
                "status", fechar ? 1 : 0));
        long documentoId = criado.path("document_id").asLong();
        String pdf = null;
        if (fechar) {
            JsonNode link = moloni.call("documents/getPDFLink", params("company_id", empresaId, "document_id", documentoId));
            pdf = link.path("url").asText(null);
        }
        log.info("Fatura-recibo {} criada no Moloni ({}): {}", documentoId, fechar ? "finalizada" : "rascunho", referencia);
        return new Documento(documentoId, fechar, pdf);
    }

    private Map<String, Object> linha(Artigo a, String nome, long qtd, BigDecimal precoComIva, double iva) {
        BigDecimal semIva = precoComIva == null ? BigDecimal.ZERO
                : precoComIva.divide(BigDecimal.valueOf(1 + iva / 100), 4, RoundingMode.HALF_UP);
        List<Map<String, Object>> impostos = a.impostos().stream()
                .map(id -> (Map<String, Object>) new HashMap<String, Object>(Map.of("tax_id", id)))
                .toList();
        Map<String, Object> l = new HashMap<>();
        l.put("product_id", a.id());
        l.put("name", nome);
        l.put("qty", qtd);
        l.put("price", semIva);
        l.put("taxes", impostos);
        return l;
    }

    private long garantirCliente(Utilizador u) {
        if (u.getMoloniClienteId() != null) {
            return u.getMoloniClienteId();
        }
        long id;
        if (Nif.valido(u.getNif())) {
            try {
                id = obterOuCriarCliente(u, u.getNif(), u.getNome());
            } catch (MoloniException e) {
                if (!e.getMessage().contains("vat")) {
                    throw e;
                }
                log.warn("O Moloni recusou o NIF {}; a faturar como consumidor final", u.getNif());
                id = obterOuCriarCliente(u, NIF_CONSUMIDOR_FINAL, "Consumidor final");
            }
        } else {
            id = obterOuCriarCliente(u, NIF_CONSUMIDOR_FINAL, "Consumidor final");
        }
        u.setMoloniClienteId(id);
        utilizadores.save(u);
        return id;
    }

    private long obterOuCriarCliente(Utilizador u, String nif, String nome) {
        JsonNode existentes = moloni.call("customers/getByVat", params("company_id", empresaId, "vat", nif));
        if (existentes.isArray() && !existentes.isEmpty()) {
            return existentes.get(0).path("customer_id").asLong();
        }
        String numero = moloni.call("customers/getNextNumber", params("company_id", empresaId))
                .path("number").asText("DN" + u.getId());
        JsonNode criado = moloni.insert("customers/insert", params(
                "company_id", empresaId,
                "vat", nif,
                "number", numero,
                "name", nome,
                "language_id", 1,
                "address", "Desconhecida",
                "city", "Guimarães",
                "country_id", 1,
                "email", NIF_CONSUMIDOR_FINAL.equals(nif) ? null : u.getEmail(),
                "phone", NIF_CONSUMIDOR_FINAL.equals(nif) ? null : u.getTelefone(),
                "maturity_date_id", vencimentoId,
                "payment_method_id", metodoPagamentoId,
                "salesman_id", 0,
                "payment_day", 0,
                "discount", 0,
                "credit_limit", 0,
                "delivery_method_id", 0));
        return criado.path("customer_id").asLong();
    }

    private void carregarConfiguracao() {
        if (empresaId == null) {
            empresaId = moloni.config().companyId() != null
                    ? moloni.config().companyId()
                    : primeiroId(moloni.call("companies/getAll", Map.of()), "company_id", "empresa");
        }
        if (serieId == null) {
            serieId = primeiroId(moloni.call("documentSets/getAll", params("company_id", empresaId)),
                    "document_set_id", "série de documentos");
        }
        if (vencimentoId == null) {
            vencimentoId = primeiroId(moloni.call("maturityDates/getAll", params("company_id", empresaId)),
                    "maturity_date_id", "prazo de vencimento");
        }
        if (metodoPagamentoId == null) {
            metodoPagamentoId = primeiroId(moloni.call("paymentMethods/getAll", params("company_id", empresaId)),
                    "payment_method_id", "método de pagamento");
        }
    }

    private Artigo artigo(String referencia) {
        Artigo a = artigos.get(referencia);
        if (a != null) {
            return a;
        }
        JsonNode lista = moloni.call("products/getByReference", params("company_id", empresaId, "reference", referencia));
        if (!lista.isArray() || lista.isEmpty()) {
            throw new MoloniException("Falta criar no Moloni o artigo com a referência " + referencia
                    + " (Tabelas → Artigos, tipo Serviço, IVA " + (int) moloni.config().iva() + "%)");
        }
        JsonNode p = lista.get(0);
        List<Long> impostos = new ArrayList<>();
        for (JsonNode t : p.path("taxes")) {
            if (t.hasNonNull("tax_id")) {
                impostos.add(t.get("tax_id").asLong());
            }
        }
        if (impostos.isEmpty()) {
            throw new MoloniException("O artigo " + referencia + " no Moloni não tem IVA definido");
        }
        a = new Artigo(p.path("product_id").asLong(), p.path("name").asText(), impostos);
        artigos.put(referencia, a);
        return a;
    }

    private static long primeiroId(JsonNode lista, String campo, String descricao) {
        if (!lista.isArray() || lista.isEmpty() || !lista.get(0).hasNonNull(campo)) {
            throw new MoloniException("Não foi encontrado(a) nenhum(a) " + descricao + " na conta Moloni: " + lista);
        }
        return lista.get(0).get(campo).asLong();
    }
}
