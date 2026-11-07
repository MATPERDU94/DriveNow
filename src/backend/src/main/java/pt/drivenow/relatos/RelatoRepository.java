package pt.drivenow.relatos;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RelatoRepository extends JpaRepository<Relato, Long> {

    boolean existsByReservaIdAndTipo(Long reservaId, TipoRelato tipo);

    Optional<Relato> findByReservaIdAndTipo(Long reservaId, TipoRelato tipo);

    List<Relato> findByReservaIdOrderByCriadoEm(Long reservaId);
}
