package pt.drivenow.danos;

import static pt.drivenow.odoo.OdooClient.booleano;
import static pt.drivenow.odoo.OdooClient.condicao;
import static pt.drivenow.odoo.OdooClient.idDe;
import static pt.drivenow.odoo.OdooClient.numero;
import static pt.drivenow.odoo.OdooClient.texto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import pt.drivenow.odoo.OdooClient;

/**
 * Tabela de custos de danos (fleet.service.type com is_damage = true)
 * e registo de danos por viatura (fleet.vehicle.log.services).
 */
@Service
public class DanoService {

    private static final String TIPOS = "fleet.service.type";
    private static final String REGISTOS = "fleet.vehicle.log.services";
    private static final List<String> ESTADOS_ABERTOS = List.of("new", "running");
    private static final List<String> CAMPOS_TIPO = List.of(
            "name", "damage_severity", "damage_cost", "blocks_vehicle", "damage_description");

    private final OdooClient odoo;

    public DanoService(OdooClient odoo) {
        this.odoo = odoo;
    }

    public List<TipoDanoDto> listarTipos() {
        return odoo.searchRead(TIPOS, List.of(condicao("is_damage", "=", true)), CAMPOS_TIPO, "name").stream()
                .map(DanoService::paraDto)
                .toList();
    }

    public Optional<TipoDanoDto> obterTipo(long id) {
        List<Object> dominio = List.of(condicao("id", "=", id), condicao("is_damage", "=", true));
        return odoo.searchRead(TIPOS, dominio, CAMPOS_TIPO, null).stream()
                .findFirst()
                .map(DanoService::paraDto);
    }

    /**
     * Regista um dano numa viatura do Odoo e devolve o id do registo criado.
     *
     * @param custo valor a registar (0 quando o dano não é cobrado ao cliente)
     */
    public long registar(long viaturaId, long tipoDanoId, double custo, String descricao) {
        Map<String, Object> valores = new HashMap<>();
        valores.put("vehicle_id", viaturaId);
        valores.put("service_type_id", tipoDanoId);
        valores.put("amount", custo);
        valores.put("description", descricao);
        valores.put("state", "new");
        return odoo.create(REGISTOS, valores);
    }

    /**
     * "Dano já registado?": existe no Odoo um registo deste tipo de dano nesta viatura
     * que ainda não foi concluído (estado Novo ou Em curso)?
     */
    public boolean existeDanoAberto(long viaturaId, long tipoDanoId) {
        List<Object> dominio = List.of(
                condicao("vehicle_id", "=", viaturaId),
                condicao("service_type_id", "=", tipoDanoId),
                condicao("state", "in", ESTADOS_ABERTOS));
        return !odoo.searchRead(REGISTOS, dominio, List.of("id"), null).isEmpty();
    }

    /**
     * Viaturas indisponíveis por dano: têm um dano por concluir de um tipo que imobiliza o carro.
     * Quando o gestor de frota marca o registo como Concluído no Odoo, a viatura volta a ficar disponível.
     */
    public Set<Long> viaturasImobilizadas() {
        List<Object> dominio = List.of(
                condicao("state", "in", ESTADOS_ABERTOS),
                condicao("service_type_id.is_damage", "=", true),
                condicao("service_type_id.blocks_vehicle", "=", true));
        return odoo.searchRead(REGISTOS, dominio, List.of("vehicle_id"), null).stream()
                .map(r -> idDe(r.get("vehicle_id")))
                .filter(id -> id != null)
                .collect(Collectors.toSet());
    }

    private static TipoDanoDto paraDto(Map<String, Object> r) {
        return new TipoDanoDto(
                idDe(r.get("id")),
                texto(r.get("name")),
                texto(r.get("damage_severity")),
                numero(r.get("damage_cost")),
                booleano(r.get("blocks_vehicle")),
                texto(r.get("damage_description")));
    }
}
