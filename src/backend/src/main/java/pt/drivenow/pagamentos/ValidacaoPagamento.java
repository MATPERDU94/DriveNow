package pt.drivenow.pagamentos;

import java.math.BigInteger;
import java.time.YearMonth;

/** Validações de cartão (algoritmo de Luhn, validade, CVV) e de IBAN (módulo 97). */
final class ValidacaoPagamento {

    /** Cartão de teste que recusa sempre (como nos ambientes de teste dos bancos). */
    static final String CARTAO_RECUSA = "4000000000000002";

    private ValidacaoPagamento() {
    }

    static String soDigitos(String s) {
        return s == null ? "" : s.replaceAll("[\\s-]", "");
    }

    static boolean luhn(String numero) {
        if (!numero.matches("\\d{13,19}")) {
            return false;
        }
        int soma = 0;
        boolean duplicar = false;
        for (int i = numero.length() - 1; i >= 0; i--) {
            int d = numero.charAt(i) - '0';
            if (duplicar) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            soma += d;
            duplicar = !duplicar;
        }
        return soma % 10 == 0;
    }

    /** "MM/AA" válido e não expirado. */
    static boolean validadeOk(String validade) {
        if (validade == null || !validade.matches("(0[1-9]|1[0-2])/\\d{2}")) {
            return false;
        }
        String[] p = validade.split("/");
        YearMonth fim = YearMonth.of(2000 + Integer.parseInt(p[1]), Integer.parseInt(p[0]));
        return !YearMonth.now().isAfter(fim);
    }

    static String marca(String numero) {
        if (numero.startsWith("4")) {
            return "Visa";
        }
        if (numero.matches("5[1-5].*") || numero.matches("2[2-7].*")) {
            return "Mastercard";
        }
        if (numero.matches("3[47].*")) {
            return "American Express";
        }
        return "Cartão";
    }

    /** IBAN português: PT + 23 dígitos, com dígitos de controlo corretos (módulo 97). */
    static boolean ibanOk(String iban) {
        if (!iban.matches("PT\\d{23}")) {
            return false;
        }
        String rearranjado = iban.substring(4) + iban.substring(0, 4);
        StringBuilder numerico = new StringBuilder();
        for (char c : rearranjado.toCharArray()) {
            numerico.append(Character.isLetter(c) ? String.valueOf(c - 'A' + 10) : String.valueOf(c));
        }
        return new BigInteger(numerico.toString()).mod(BigInteger.valueOf(97)).intValue() == 1;
    }
}
