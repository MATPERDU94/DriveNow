package pt.drivenow.danos;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/danos")
public class DanoController {

    private final DanoService danos;

    public DanoController(DanoService danos) {
        this.danos = danos;
    }

    /** GET /api/danos/tipos: a lista que o cliente vê na app para escolher o tipo de dano. */
    @GetMapping("/tipos")
    public List<TipoDanoDto> tipos() {
        return danos.listarTipos();
    }
}
