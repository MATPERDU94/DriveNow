package pt.drivenow.revisoes;

import static pt.drivenow.odoo.OdooClient.condicao;
import static pt.drivenow.odoo.OdooClient.idDe;
import static pt.drivenow.odoo.OdooClient.numero;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import pt.drivenow.frota.ViaturaDto;
import pt.drivenow.odoo.OdooClient;

/**
 * Revisões por km (diagrama 6): todos os carros vão à oficina DriveNow a cada 15 000 km.
 *
 * O Odoo (módulo drivenow_danos) guarda em cada carro os "km da próxima revisão" e, quando a oficina
 * conclui uma ordem de revisão, passa-os para os km atuais + 15 000. O backend tira o carro do mapa
 * ("Revisão por fazer") quando o conta-km chega a esses km ou quando há uma ordem de revisão por concluir.
 */
@Service
public class RevisaoService {

    /** A partir daqui a revisão está próxima (aviso ao gestor de frota). */
    static final double AVISO_KM = 1000;

    private static final String REGISTOS = "fleet.vehicle.log.services";
    private static final List<String> ESTADOS_ABERTOS = List.of("new", "running");

    private static final Logger log = LoggerFactory.getLogger(RevisaoService.class);

    private final OdooClient odoo;

    public RevisaoService(OdooClient odoo) {
        this.odoo = odoo;
    }

    /** O conta-km do carro chegou aos km da próxima revisão? */
    public static boolean chegouARevisao(ViaturaDto v) {
        return v.kmProximaRevisao() != null && v.kmProximaRevisao() > 0
                && v.quilometragem() != null && v.quilometragem() >= v.kmProximaRevisao();
    }

    /** Viaturas com uma ordem de revisão por concluir (estado Novo ou Em curso). */
    public Set<Long> viaturasComRevisaoAberta() {
        List<Object> dominio = List.of(
                condicao("service_type_id.is_revision", "=", true),
                condicao("state", "in", ESTADOS_ABERTOS));
        return odoo.searchRead(REGISTOS, dominio, List.of("vehicle_id"), null).stream()
                .map(r -> idDe(r.get("vehicle_id")))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Diagrama 6, quando o n8n não está ligado: depois de uma viagem, se o carro chegou aos km da revisão
     * e ainda não tem ordem de revisão aberta, cria a ordem "Revisão" no Odoo (a oficina vê-a em
     * Frota → Revisões → Ordens de revisão). Nunca falha: um erro aqui não pode estragar o fim da viagem.
     */
    public void verificarDepoisDaViagem(long viaturaId, double km) {
        try {
            Map<String, Object> carro = odoo.searchRead("fleet.vehicle", List.of(condicao("id", "=", viaturaId)),
                    List.of("license_plate", "next_service_km"), null).stream().findFirst().orElse(null);
            Double proxima = carro == null ? null : numero(carro.get("next_service_km"));
            if (proxima == null || proxima <= 0) {
                return;
            }
            Object matricula = carro.get("license_plate");
            if (km < proxima) {
                if (proxima - km <= AVISO_KM) {
                    log.info("Revisão próxima: {} está a {} km da revisão dos {} km", matricula,
                            Math.round(proxima - km), Math.round(proxima));
                }
                return;
            }
            if (viaturasComRevisaoAberta().contains(viaturaId)) {
                return;
            }
            Long tipo = odoo.searchRead("fleet.service.type", List.of(condicao("is_revision", "=", true)),
                    List.of("id"), "id").stream().findFirst().map(r -> idDe(r.get("id"))).orElse(null);
            if (tipo == null) {
                log.warn("Não existe o serviço 'Revisão' no Odoo: atualize o módulo drivenow_danos");
                return;
            }
            long ordem = odoo.create(REGISTOS, Map.of(
                    "vehicle_id", viaturaId,
                    "service_type_id", tipo,
                    "state", "new",
                    "description", "Revisão dos " + Math.round(proxima) + " km (conta-km: " + Math.round(km) + " km)"));
            log.info("Ordem de revisão {} criada no Odoo para {} ({} km)", ordem, matricula, Math.round(km));
        } catch (RuntimeException e) {
            log.warn("Não foi possível verificar a revisão da viatura {}: {}", viaturaId, e.getMessage());
        }
    }
}
