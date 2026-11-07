package pt.drivenow.utilizadores;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UtilizadorRepository extends JpaRepository<Utilizador, Long> {

    boolean existsByEmailIgnoreCase(String email);
}
