package pt.drivenow.pagamentos;

/**
 * Dados enviados pela app ao associar um meio de pagamento.
 *
 * Cartão: {"tipo": "CARTAO", "titular": "ANA SILVA", "numero": "4242 4242 4242 4242", "validade": "08/28", "cvv": "123"}
 * Conta:  {"tipo": "CONTA_BANCARIA", "titular": "Ana Silva", "iban": "PT50 0002 0123 1234 5678 9015 4"}
 *
 * Cartões de teste: 4242 4242 4242 4242 aprova; 4000 0000 0000 0002 recusa sempre.
 */
public record PedidoMeioPagamento(
        TipoMeioPagamento tipo,
        String titular,
        String numero,
        String validade,
        String cvv,
        String iban) {
}
