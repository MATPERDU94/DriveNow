package pt.drivenow.utilizadores;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pt.drivenow.common.NaoEncontradoException;
import pt.drivenow.common.Nif;
import pt.drivenow.common.RegraNegocioException;

@RestController
@RequestMapping("/api/utilizadores")
public class UtilizadorController {

    public record CriarUtilizadorRequest(
            @NotBlank String nome,
            @NotBlank @Email String email,
            @Pattern(regexp = "\\d{9}", message = "deve ter 9 dígitos") String nif,
            String telefone) {
    }

    private final UtilizadorRepository utilizadores;

    public UtilizadorController(UtilizadorRepository utilizadores) {
        this.utilizadores = utilizadores;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Utilizador criar(@Valid @RequestBody CriarUtilizadorRequest pedido) {
        if (utilizadores.existsByEmailIgnoreCase(pedido.email())) {
            throw new RegraNegocioException("Já existe um utilizador com o email " + pedido.email());
        }
        if (pedido.nif() != null && !pedido.nif().isBlank() && !Nif.valido(pedido.nif())) {
            throw new RegraNegocioException("O NIF " + pedido.nif() + " não é um NIF português válido");
        }
        return utilizadores.save(new Utilizador(pedido.nome(), pedido.email(), pedido.nif(), pedido.telefone()));
    }

    @GetMapping
    public List<Utilizador> listar() {
        return utilizadores.findAll();
    }

    @GetMapping("/{id}")
    public Utilizador obter(@PathVariable long id) {
        return utilizadores.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Utilizador " + id + " não existe"));
    }
}
