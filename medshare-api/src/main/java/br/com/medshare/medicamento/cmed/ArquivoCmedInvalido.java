package br.com.medshare.medicamento.cmed;

/** O arquivo nao tem o formato da lista de precos publicada pela CMED. */
public class ArquivoCmedInvalido extends RuntimeException {

    public ArquivoCmedInvalido(String mensagem) {
        super(mensagem);
    }
}
