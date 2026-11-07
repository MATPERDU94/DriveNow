package pt.drivenow.localizacao;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.drivenow.common.RegraNegocioException;

@Service
public class LocalizacaoService {

    private static final double RAIO_TERRA_KM = 6371.0;

    private final PosicaoViaturaRepository posicoes;

    public LocalizacaoService(PosicaoViaturaRepository posicoes) {
        this.posicoes = posicoes;
    }

    @Transactional(readOnly = true)
    public Map<Long, PosicaoViatura> todas() {
        return posicoes.findAll().stream()
                .collect(Collectors.toMap(PosicaoViatura::getViaturaOdooId, Function.identity()));
    }

    @Transactional(readOnly = true)
    public Optional<PosicaoViatura> daViatura(long viaturaId) {
        return posicoes.findById(viaturaId);
    }

    /** Guarda a nova posição da viatura (cria-a se ainda não existir). */
    @Transactional
    public void atualizar(long viaturaId, double latitude, double longitude) {
        validar(latitude, longitude);
        posicoes.findById(viaturaId).ifPresentOrElse(
                p -> p.mover(latitude, longitude),
                () -> posicoes.save(new PosicaoViatura(viaturaId, latitude, longitude)));
    }

    public static void validar(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new RegraNegocioException("Coordenadas inválidas: " + latitude + ", " + longitude);
        }
    }

    /** Distância em km entre dois pontos (fórmula de Haversine). */
    public static double distanciaKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * RAIO_TERRA_KM * Math.asin(Math.sqrt(a));
    }
}
