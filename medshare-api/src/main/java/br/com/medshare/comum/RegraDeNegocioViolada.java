package br.com.medshare.comum;

/**
 * Uma regra do documento de modelagem foi desrespeitada.
 * A regra que falhou vai junto para que a resposta da API diga exatamente
 * qual foi — "RN02" e mais util para quem esta depurando do que "400".
 */
public class RegraDeNegocioViolada extends RuntimeException {

    private final String regra;

    public RegraDeNegocioViolada(String regra, String mensagem) {
        super(mensagem);
        this.regra = regra;
    }

    public String getRegra() {
        return regra;
    }
}
