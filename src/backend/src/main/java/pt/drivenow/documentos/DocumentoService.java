package pt.drivenow.documentos;

import static pt.drivenow.odoo.OdooClient.condicao;
import static pt.drivenow.odoo.OdooClient.idDe;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import pt.drivenow.odoo.OdooClient;

/**
 * Documentos das viaturas (diagrama 5): seguro, inspeção periódica e IUC.
 * Ficam no Odoo como contratos da viatura (fleet.vehicle.log.contract), cada um com o tipo e a data de validade.
 * Uma viatura com um destes documentos caducado (e o contrato não fechado) não pode ser reservada.
 * Quando o gestor de frota renova o documento no Odoo, a viatura volta ao mapa sozinha.
 */
@Service
public class DocumentoService {

    public static final List<String> TIPOS = List.of("Seguro", "Inspeção periódica", "IUC");
    private static final String CONTRATOS = "fleet.vehicle.log.contract";

    private final OdooClient odoo;

    public DocumentoService(OdooClient odoo) {
        this.odoo = odoo;
    }

    /** Viaturas com pelo menos um documento obrigatório caducado. */
    public Set<Long> viaturasComDocumentosCaducados() {
        List<Object> dominio = List.of(
                condicao("cost_subtype_id.name", "in", TIPOS),
                condicao("state", "!=", "closed"),
                condicao("expiration_date", "<", LocalDate.now().toString()));
        return odoo.searchRead(CONTRATOS, dominio, List.of("vehicle_id"), null).stream()
                .map(r -> idDe(r.get("vehicle_id")))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
