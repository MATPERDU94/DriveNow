package pt.drivenow.reservas;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    /** Ids (Odoo) das viaturas que estão em reservas com um destes estados. */
    @Query("select r.viaturaOdooId from Reserva r where r.estado in :estados")
    List<Long> viaturasComEstado(@Param("estados") Collection<EstadoReserva> estados);

    boolean existsByViaturaOdooIdAndEstadoIn(Long viaturaOdooId, Collection<EstadoReserva> estados);

    boolean existsByUtilizadorIdAndEstadoIn(Long utilizadorId, Collection<EstadoReserva> estados);

    List<Reserva> findByUtilizadorIdOrderByCriadaEmDesc(Long utilizadorId);
}
