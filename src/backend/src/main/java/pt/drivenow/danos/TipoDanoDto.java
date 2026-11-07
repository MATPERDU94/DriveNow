package pt.drivenow.danos;

/** Um tipo de dano da tabela de custos do Odoo (módulo drivenow_danos). */
public record TipoDanoDto(
        Long id,
        String nome,
        String gravidade,
        Double custo,
        boolean imobilizaCarro,
        String descricao) {
}
