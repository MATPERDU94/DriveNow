package pt.drivenow.common;

import java.net.URI;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import pt.drivenow.moloni.MoloniException;
import pt.drivenow.odoo.OdooException;

/** Converte as exceções em respostas JSON (formato ProblemDetail). */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(NaoEncontradoException.class)
    public ProblemDetail naoEncontrado(NaoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "Não encontrado", e.getMessage());
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ProblemDetail regraNegocio(RegraNegocioException e) {
        return problema(HttpStatus.CONFLICT, "Pedido inválido", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacao(MethodArgumentNotValidException e) {
        String detalhe = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return problema(HttpStatus.BAD_REQUEST, "Dados inválidos", detalhe);
    }

    @ExceptionHandler(OdooException.class)
    public ProblemDetail odoo(OdooException e) {
        log.error("Erro na integração com o Odoo", e);
        return problema(HttpStatus.BAD_GATEWAY, "Erro no Odoo", e.getMessage());
    }

    @ExceptionHandler(MoloniException.class)
    public ProblemDetail moloni(MoloniException e) {
        log.error("Erro na integração com o Moloni", e);
        return problema(HttpStatus.BAD_GATEWAY, "Erro no Moloni", e.getMessage());
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detalhe);
        p.setTitle(titulo);
        p.setType(URI.create("about:blank"));
        return p;
    }
}
