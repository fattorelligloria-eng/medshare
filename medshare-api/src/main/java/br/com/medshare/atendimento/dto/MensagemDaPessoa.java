package br.com.medshare.atendimento.dto;

import br.com.medshare.atendimento.AssuntoDaMensagem;
import br.com.medshare.atendimento.MensagemDeAtendimento;

import java.time.OffsetDateTime;

/** Como a propria pessoa ve a mensagem que mandou: sem quem respondeu, so a resposta. */
public record MensagemDaPessoa(
        Long id,
        AssuntoDaMensagem assunto,
        String texto,
        OffsetDateTime criadoEm,
        String resposta,
        OffsetDateTime respondidaEm
) {
    public static MensagemDaPessoa de(MensagemDeAtendimento m) {
        return new MensagemDaPessoa(m.getId(), m.getAssunto(), m.getTexto(), m.getCriadoEm(),
                m.getResposta(), m.getRespondidaEm());
    }
}
