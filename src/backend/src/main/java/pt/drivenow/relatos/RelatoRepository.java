package pt.drivenow.relatos;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RelatoRepository extends JpaRepository<Relato, Long> {

    boolean existsByReservaIdAndTipo(Long reservaId, TipoRelato tipo);

    Optional<Relato> findByReservaIdAndTipo(Long reservaId, TipoRelato tipo);

    List<Relato> findByReservaIdOrderByCriadoEm(Long reservaId);

    /** Relato já com os danos carregados (para usar fora de uma transação). */
    @Query("select distinct r from Relato r left join fetch r.danos where r.reserva.id = :reservaId and r.tipo = :tipo")
    Optional<Relato> comDanos(@Param("reservaId") Long reservaId, @Param("tipo") TipoRelato tipo);

    @Query("select distinct r from Relato r left join fetch r.danos where r.id = :id")
    Optional<Relato> comDanosPorId(@Param("id") Long id);
}
