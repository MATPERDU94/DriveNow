package pt.drivenow.relatos;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record RelatoDto(
        Long id,
        Long reservaId,
        TipoRelato tipo,
        String observacoes,
        Instant criadoEm,
        List<DanoDto> danos,
        BigDecimal totalPenalizacoes) {

    public record DanoDto(
            Long tipoDanoId,
            String tipoDano,
            String descricao,
            BigDecimal penalizacao,
            Long odooRegistoId) {
    }

    public static RelatoDto de(Relato r) {
        List<DanoDto> danos = r.getDanos().stream()
                .map(d -> new DanoDto(d.getTipoDanoOdooId(), d.getNomeTipo(), d.getDescricao(),
                        d.getPenalizacao(), d.getOdooRegistoId()))
                .toList();
        BigDecimal total = danos.stream().map(DanoDto::penalizacao).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new RelatoDto(r.getId(), r.getReserva().getId(), r.getTipo(), r.getObservacoes(),
                r.getCriadoEm(), danos, total);
    }
}
