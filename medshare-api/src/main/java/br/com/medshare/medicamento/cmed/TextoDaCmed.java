package br.com.medshare.medicamento.cmed;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A lista da CMED vem toda em maiusculas ("HUMIRA", "ADALIMUMABE", "100 MG/ML
 * SOL INJ CT 2 BL"). Gravado assim, o catalogo grita em todas as telas. Aqui
 * cada campo ganha a caixa que uma bula usaria: nome comercial com iniciais
 * maiusculas, principio ativo como substantivo comum, apresentacao em
 * minusculas com as siglas de unidade preservadas.
 *
 * A busca e o matching comparam sem caixa (LOWER / sem_acento), entao mudar a
 * caixa nao muda o que casa com o que.
 */
final class TextoDaCmed {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    /** Palavras que ficam minusculas no meio de um nome. */
    private static final Set<String> LIGACOES = Set.of("de", "da", "do", "das", "dos", "e", "com", "em", "para");

    /** Unidades que sao siglas: "ui" e "UI" (unidade internacional), nao uma palavra. */
    private static final Set<String> SIGLAS = Set.of("ui", "ut", "utr", "ufc", "mui", "ppm");

    private static final Pattern PALAVRA = Pattern.compile("\\p{L}+");

    /** Pega o comeco de cada componente de uma associacao: "x + y", "x;y". */
    private static final Pattern INICIO_DE_COMPONENTE = Pattern.compile("(^|[;+]\\s*)(\\p{L})");

    private TextoDaCmed() {
    }

    /** "HUMIRA" vira "Humira"; "ACIDO ZOLEDRONICO DE SODIO" vira "Acido Zoledronico de Sodio". */
    static String nome(String texto) {
        if (!emMaiusculas(texto)) {
            return texto;
        }
        Matcher m = PALAVRA.matcher(texto.toLowerCase(PT_BR));
        StringBuilder saida = new StringBuilder();
        boolean primeira = true;
        while (m.find()) {
            String palavra = m.group();
            boolean ligacao = !primeira && LIGACOES.contains(palavra);
            m.appendReplacement(saida, Matcher.quoteReplacement(ligacao ? palavra : inicialMaiuscula(palavra)));
            primeira = false;
        }
        m.appendTail(saida);
        return saida.toString();
    }

    /** "ADALIMUMABE" vira "Adalimumabe"; "LAMIVUDINA;ZIDOVUDINA" vira "Lamivudina;Zidovudina". */
    static String principio(String texto) {
        if (!emMaiusculas(texto)) {
            return texto;
        }
        Matcher m = INICIO_DE_COMPONENTE.matcher(texto.toLowerCase(PT_BR));
        StringBuilder saida = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(saida, Matcher.quoteReplacement(m.group(1) + m.group(2).toUpperCase(PT_BR)));
        }
        m.appendTail(saida);
        return saida.toString();
    }

    /** "100 MG/ML SOL INJ CT 2 BL" vira "100 mg/ml sol inj ct 2 bl"; "4000 UI" continua "4000 UI". */
    static String apresentacao(String texto) {
        if (!emMaiusculas(texto)) {
            return texto;
        }
        Matcher m = PALAVRA.matcher(texto.toLowerCase(PT_BR));
        StringBuilder saida = new StringBuilder();
        while (m.find()) {
            String palavra = m.group();
            m.appendReplacement(saida, Matcher.quoteReplacement(
                    SIGLAS.contains(palavra) ? palavra.toUpperCase(PT_BR) : palavra));
        }
        m.appendTail(saida);
        return saida.toString();
    }

    /** So mexe no que veio gritado; um texto que ja tem minusculas foi escrito por alguem. */
    private static boolean emMaiusculas(String texto) {
        return texto != null && texto.equals(texto.toUpperCase(PT_BR)) && !texto.equals(texto.toLowerCase(PT_BR));
    }

    private static String inicialMaiuscula(String palavra) {
        return palavra.substring(0, 1).toUpperCase(PT_BR) + palavra.substring(1);
    }
}
