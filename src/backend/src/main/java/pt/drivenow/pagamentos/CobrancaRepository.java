package pt.drivenow.pagamentos;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CobrancaRepository extends JpaRepository<Cobranca, Long> {

    List<Cobranca> findByUtilizadorIdOrderByCriadaEmDesc(Long utilizadorId);

    List<Cobranca> findByUtilizadorIdAndEstado(Long utilizadorId, EstadoCobranca estado);

    boolean existsByUtilizadorIdAndEstado(Long utilizadorId, EstadoCobranca estado);

    Optional<Cobranca> findFirstByReservaIdAndTipo(Long reservaId, TipoCobranca tipo);

    Optional<Cobranca> findFirstByRelatoId(Long relatoId);
}
