package br.com.medshare.doacao.dto;

import br.com.medshare.doacao.EventoHistorico;

import java.time.OffsetDateTime;

public record EventoResumido(
        OffsetDateTime quando,
        String tipo,
        String descricao,
        String statusAnterior,
        String statusNovo
) {

    public static EventoResumido de(EventoHistorico evento) {
        return new EventoResumido(
                evento.getOcorridoEm(),
                evento.getTipo().name(),
                evento.getDescricao(),
                evento.getStatusAnterior() == null ? null : evento.getStatusAnterior().name(),
                evento.getStatusNovo() == null ? null : evento.getStatusNovo().name());
    }
}
