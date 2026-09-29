package br.com.medshare.prevalidacao;

public enum Certeza {
    ALTA,
    MEDIA,
    BAIXA;

    /** RN10 - qualquer coisa abaixo de ALTA obriga revisao humana. */
    public boolean suficienteParaDecidirSozinha() {
        return this == ALTA;
    }

    /** Como a certeza aparece para quem le a explicacao na tela. */
    public String porExtenso() {
        return switch (this) {
            case ALTA -> "alta";
            case MEDIA -> "média";
            case BAIXA -> "baixa";
        };
    }
}
