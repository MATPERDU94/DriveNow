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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import pt.drivenow.n8n.N8nClient;
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

    private static final Logger log = LoggerFactory.getLogger(DanoService.class);

    private final OdooClient odoo;
    private final N8nClient n8n;

    public DanoService(OdooClient odoo, N8nClient n8n) {
        this.odoo = odoo;
        this.n8n = n8n;
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
        // Diagrama 4: o evento "dano reportado" vai para o n8n, que regista o dano no Odoo e devolve o id.
        // Se o n8n não estiver configurado ou não responder, o backend regista o dano diretamente.
        Map<String, Object> evento = new HashMap<>();
        evento.put("viaturaId", viaturaId);
        evento.put("tipoDanoId", tipoDanoId);
        evento.put("custo", custo);
        evento.put("descricao", descricao);
        Optional<Long> viaN8n = n8n.pedir("dano-reportado", evento)
                .map(r -> r.get("odooId"))
                .filter(Number.class::isInstance)
                .map(id -> ((Number) id).longValue());
        if (viaN8n.isPresent()) {
            return viaN8n.get();
        }
        log.info("Dano registado diretamente no Odoo (sem n8n)");
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
