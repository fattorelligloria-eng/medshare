package br.com.medshare.prevalidacao;

/** As quatro classes que o modelo de visao devolve para a foto da caixa. */
public enum ClasseEmbalagem {
    /** Abas coladas, selo intacto, blister nao aparece. */
    LACRADA,
    /** Caixa aberta ou selo rompido. */
    VIOLADA,
    /** Amassada, rasgada, molhada. */
    DANIFICADA,
    /** Nao e uma caixa de medicamento, ou a foto nao permite avaliar. */
    INVALIDA;

    /** Como a classe aparece para quem le a explicacao na tela. */
    public String porExtenso() {
        return switch (this) {
            case LACRADA -> "lacrada";
            case VIOLADA -> "violada";
            case DANIFICADA -> "danificada";
            case INVALIDA -> "inválida";
        };
    }
}
