package pt.drivenow.common;

/** Validação do NIF português (9 dígitos + dígito de controlo). */
public final class Nif {

    private Nif() {
    }

    public static boolean valido(String nif) {
        if (nif == null || !nif.matches("\\d{9}")) {
            return false;
        }
        int soma = 0;
        for (int i = 0; i < 8; i++) {
            soma += (nif.charAt(i) - '0') * (9 - i);
        }
        int resto = soma % 11;
        int controlo = resto < 2 ? 0 : 11 - resto;
        return controlo == nif.charAt(8) - '0';
    }
}
