package br.com.medshare.medicamento.cmed;

import br.com.medshare.medicamento.Tarja;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Uma apresentacao da lista da CMED, ja limpa e pronta para gravar.
 *
 * A planilha traz 74 colunas; o MedShare usa nove. As colunas sao achadas pelo
 * nome do cabecalho, e nao pela posicao: a ANVISA ja acrescentou colunas no
 * meio da lista de um ano para outro, e o importador nao deveria quebrar por
 * isso.
 */
public record LinhaDaCmed(
        String codigoGgrem,
        String registro,
        String ean,
        String nomeComercial,
        String principioAtivo,
        String apresentacao,
        String laboratorio,
        BigDecimal pmc,
        Tarja tarja
) {

    static final String SUBSTANCIA = "SUBSTANCIA";
    static final String LABORATORIO = "LABORATORIO";
    static final String GGREM = "CODIGO GGREM";
    static final String REGISTRO = "REGISTRO";
    static final String EAN = "EAN 1";
    static final String PRODUTO = "PRODUTO";
    static final String APRESENTACAO = "APRESENTACAO";
    static final String TARJA = "TARJA";

    /** As posicoes das colunas usadas, lidas da linha de cabecalho. */
    public record Colunas(Map<String, Integer> indices, String colunaDoPmc) {

        public static Optional<Colunas> doCabecalho(List<String> celulas, String colunaDoPmc) {
            Map<String, Integer> indices = new HashMap<>();
            for (int i = 0; i < celulas.size(); i++) {
                indices.putIfAbsent(normalizar(celulas.get(i)), i);
            }
            if (!indices.containsKey(SUBSTANCIA)) {
                return Optional.empty();
            }
            String pmc = normalizar(colunaDoPmc);
            for (String obrigatoria : List.of(GGREM, REGISTRO, PRODUTO, APRESENTACAO, pmc)) {
                if (!indices.containsKey(obrigatoria)) {
                    throw new ArquivoCmedInvalido(
                            "A planilha não tem a coluna \"%s\". Confira se é a lista de preços da CMED."
                                    .formatted(obrigatoria));
                }
            }
            return Optional.of(new Colunas(indices, pmc));
        }

        String valor(List<String> celulas, String coluna) {
            Integer indice = indices.get(coluna);
            if (indice == null || indice >= celulas.size()) {
                return "";
            }
            String valor = celulas.get(indice);
            return valor == null ? "" : valor.strip();
        }
    }

    /**
     * Converte uma linha de dados. Vazio quando a apresentacao nao serve para
     * o catalogo: sem PMC (uso hospitalar, sem venda em farmacia) ou com
     * codigo GGREM fora do formato.
     */
    public static Optional<LinhaDaCmed> de(List<String> celulas, Colunas colunas) {
        String ggrem = colunas.valor(celulas, GGREM);
        String produto = colunas.valor(celulas, PRODUTO);
        Optional<BigDecimal> pmc = preco(colunas.valor(celulas, colunas.colunaDoPmc()));

        if (!ggrem.matches("\\d{15}") || produto.isEmpty() || pmc.isEmpty()) {
            return Optional.empty();
        }
        String registro = colunas.valor(celulas, REGISTRO).replaceAll("\\D", "");
        String ean = colunas.valor(celulas, EAN).replaceAll("\\D", "");

        return Optional.of(new LinhaDaCmed(
                ggrem,
                registro.isEmpty() ? "SEM REGISTRO" : registro,
                ean.length() >= 8 && ean.length() <= 14 ? ean : null,
                limitar(produto, 200),
                colunas.valor(celulas, SUBSTANCIA),
                limitar(colunas.valor(celulas, APRESENTACAO), 300),
                vazioViraNulo(limitar(colunas.valor(celulas, LABORATORIO), 200)),
                pmc.get(),
                tarja(colunas.valor(celulas, TARJA))));
    }

    /**
     * "11.336,31" (texto no formato brasileiro, como a ANVISA publica) ou
     * 11336.31 (celula numerica) viram 11336.31. Vazio, traco ou zero: sem preco.
     */
    static Optional<BigDecimal> preco(String texto) {
        String limpo = texto.strip();
        if (limpo.contains(",")) {
            limpo = limpo.replace(".", "").replace(",", ".");
        }
        if (limpo.isEmpty()) {
            return Optional.empty();
        }
        try {
            BigDecimal valor = new BigDecimal(limpo);
            return valor.signum() > 0 ? Optional.of(valor) : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    static Tarja tarja(String texto) {
        String t = normalizar(texto);
        if (t.contains("PRETA")) return Tarja.PRETA;
        if (t.contains("VERMELHA")) return Tarja.VERMELHA;     // inclui "sob restricao"
        if (t.contains("SEM TARJA")) return Tarja.SEM_TARJA;
        return Tarja.NAO_INFORMADA;                             // "- (*)"
    }

    /** "CÓDIGO  GGREM " vira "CODIGO GGREM", para achar a coluna pelo nome. */
    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.replace(' ', ' ').replaceAll("\\s+", " ").strip().toUpperCase();
    }

    private static String limitar(String texto, int tamanho) {
        return texto.length() <= tamanho ? texto : texto.substring(0, tamanho);
    }

    private static String vazioViraNulo(String texto) {
        return texto.isEmpty() ? null : texto;
    }
}
