package br.com.medshare.integracao;

/**
 * Confere a forma do NIS antes de gastar uma chamada de rede com ele.
 *
 * O NIS usa o mesmo cálculo de dígito verificador do PIS/PASEP: pesos de 3 a 2
 * da esquerda para a direita, módulo 11. Um número digitado errado é pego aqui,
 * de graça e na hora, em vez de voltar como "não encontrado" e deixar a pessoa
 * achando que não tem direito.
 */
public final class ValidadorDeNis {

    private static final int[] PESOS = {3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    private ValidadorDeNis() { }

    public static boolean temFormatoValido(String nis) {
        if (nis == null || !nis.matches("\\d{11}")) {
            return false;
        }
        int soma = 0;
        for (int i = 0; i < 10; i++) {
            soma += Character.getNumericValue(nis.charAt(i)) * PESOS[i];
        }
        int resto = soma % 11;
        int digitoEsperado = (resto < 2) ? 0 : 11 - resto;
        return Character.getNumericValue(nis.charAt(10)) == digitoEsperado;
    }
}
