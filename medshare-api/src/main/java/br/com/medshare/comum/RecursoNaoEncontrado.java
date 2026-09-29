package br.com.medshare.comum;

public class RecursoNaoEncontrado extends RuntimeException {

    public RecursoNaoEncontrado(String recurso, Object identificador) {
        super("Não encontramos %s: %s".formatted(recurso, identificador));
    }
}
