package pt.drivenow.pagamentos;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MeioPagamentoRepository extends JpaRepository<MeioPagamento, Long> {

    Optional<MeioPagamento> findByUtilizadorId(Long utilizadorId);
}
