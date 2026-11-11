package pt.drivenow.utilizadores;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UtilizadorRepository extends JpaRepository<Utilizador, Long> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<Utilizador> findByEmailIgnoreCase(String email);
}
