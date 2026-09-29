package br.com.medshare.integracao;

import br.com.medshare.prevalidacao.Certeza;
import br.com.medshare.prevalidacao.ClasseEmbalagem;

import java.time.LocalDate;

/**
 * O que a leitura automatica conseguiu extrair da foto.
 *
 * Campo nulo significa "nao consegui ler", e nao "nao existe" — a diferenca
 * importa: campo ilegivel manda o caso para a central (RN10).
 */
public record LeituraDaEmbalagem(
        String ean,
        String lote,
        LocalDate validade,
        ClasseEmbalagem classe,
        Certeza certeza,
        String motivo
) {

    /** Usada quando o servico de visao falha: na duvida, chama um humano. */
    public static LeituraDaEmbalagem indisponivel(String motivo) {
        return new LeituraDaEmbalagem(null, null, null,
                ClasseEmbalagem.INVALIDA, Certeza.BAIXA, motivo);
    }
}
