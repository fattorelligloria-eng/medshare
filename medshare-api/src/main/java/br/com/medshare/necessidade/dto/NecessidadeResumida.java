package br.com.medshare.necessidade.dto;

import br.com.medshare.necessidade.Necessidade;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * @param emRevisao     UC07 A1 - a receita nao bateu no balcao; precisa de outra
 * @param prioridade    UC07 A2 - perdeu uma caixa por validade e esta na frente da fila
 */
public record NecessidadeResumida(
        Long id,
        String medicamento,
        String principioAtivo,
        boolean ativa,
        boolean temReceitaValida,
        LocalDate validadeDaReceita,
        boolean emRevisao,
        String motivoRevisao,
        boolean prioridade,
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
                necessidade.isEmRevisao(),
                necessidade.getMotivoRevisao(),
                necessidade.isPrioridade(),
                necessidade.getCriadaEm());
    }
}
