package pt.drivenow.frota;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pt.drivenow.common.NaoEncontradoException;
import pt.drivenow.common.RegraNegocioException;

@RestController
@RequestMapping("/api/viaturas")
public class FrotaController {

    private final FrotaService frota;

    public FrotaController(FrotaService frota) {
        this.frota = frota;
    }

    /**
     * GET /api/viaturas: toda a frota, com coordenadas.
     * GET /api/viaturas?lat=41.44&lng=-8.29&raio=2&disponiveis=true: viaturas livres
     * a menos de 2 km do cliente, da mais próxima para a mais afastada.
     */
    @GetMapping
    public List<ViaturaDto> listar(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double raio,
            @RequestParam(defaultValue = "false") boolean disponiveis) {
        if ((lat == null) != (lng == null)) {
            throw new RegraNegocioException("Indica lat e lng em conjunto");
        }
        if (raio != null && lat == null) {
            throw new RegraNegocioException("O raio exige lat e lng");
        }
        return frota.listar(lat, lng, raio, disponiveis);
    }

    @GetMapping("/{id}")
    public ViaturaDto obter(@PathVariable long id) {
        return frota.obter(id).orElseThrow(() -> new NaoEncontradoException("Viatura " + id + " não existe"));
    }
}
