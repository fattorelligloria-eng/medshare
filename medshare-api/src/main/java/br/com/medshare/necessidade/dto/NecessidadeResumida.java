package br.com.medshare.necessidade.dto;

import br.com.medshare.necessidade.Necessidade;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record NecessidadeResumida(
        Long id,
        String medicamento,
        String principioAtivo,
        boolean ativa,
        boolean temReceitaValida,
        LocalDate validadeDaReceita,
        OffsetDateTime criadaEm
) {

    public static NecessidadeResumida de(Necessidade necessidade) {
        var receita = necessidade.getReceita();
        return new NecessidadeResumida(
                necessidade.getId(),
                necessidade.getMedicamento().getNomeComercial(),
                necessidade.getMedicamento().getPrincipioAtivo(),
                necessidade.isAtiva(),
                receita != null && receita.estaValida(),
                receita == null ? null : receita.getValidade(),
                necessidade.getCriadaEm());
    }
}
