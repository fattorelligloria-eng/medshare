package br.com.medshare.doacao.dto;

import br.com.medshare.doacao.Doacao;
import br.com.medshare.doacao.EventoHistorico;

import java.util.List;

/** RN06 - a doacao com o historico completo, do cadastro ate agora. */
public record DoacaoDetalhada(
        DoacaoResumida doacao,
        List<EventoResumido> historico
) {

    public static DoacaoDetalhada de(Doacao doacao, List<EventoHistorico> eventos) {
        return new DoacaoDetalhada(
                DoacaoResumida.de(doacao),
                eventos.stream().map(EventoResumido::de).toList());
    }
}
