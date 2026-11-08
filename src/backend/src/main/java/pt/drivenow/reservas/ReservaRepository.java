package pt.drivenow.reservas;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    /** Ids (Odoo) das viaturas que estão em reservas com um destes estados. */
    @Query("select r.viaturaOdooId from Reserva r where r.estado in :estados")
    List<Long> viaturasComEstado(@Param("estados") Collection<EstadoReserva> estados);

    /** Reserva já com o utilizador carregado (para usar fora de uma transação). */
    @Query("select r from Reserva r join fetch r.utilizador where r.id = :id")
    Optional<Reserva> comUtilizador(@Param("id") Long id);

    boolean existsByViaturaOdooIdAndEstadoIn(Long viaturaOdooId, Collection<EstadoReserva> estados);

    boolean existsByUtilizadorIdAndEstadoIn(Long utilizadorId, Collection<EstadoReserva> estados);

    List<Reserva> findByUtilizadorIdOrderByCriadaEmDesc(Long utilizadorId);

    /** Última viagem concluída nesta viatura antes da reserva indicada (o "condutor anterior"). */
    Optional<Reserva> findFirstByViaturaOdooIdAndEstadoAndIdLessThanOrderByFimViagemDesc(
            Long viaturaOdooId, EstadoReserva estado, Long reservaId);
}
