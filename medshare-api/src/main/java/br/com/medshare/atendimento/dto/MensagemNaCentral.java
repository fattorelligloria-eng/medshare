package br.com.medshare.atendimento.dto;

import br.com.medshare.atendimento.AssuntoDaMensagem;
import br.com.medshare.atendimento.MensagemDeAtendimento;

import java.time.OffsetDateTime;

/**
 * A mensagem na mesa da central.
 *
 * Vai o nome de quem escreveu, para a resposta nao sair impessoal, e mais
 * nada do cadastro: CPF e endereco nao ajudam a responder uma duvida.
 */
public record MensagemNaCentral(
        Long id,
        AssuntoDaMensagem assunto,
        String texto,
        OffsetDateTime criadoEm,
        String nome
) {
    public static MensagemNaCentral de(MensagemDeAtendimento m) {
        return new MensagemNaCentral(m.getId(), m.getAssunto(), m.getTexto(), m.getCriadoEm(),
                m.getUsuario().getNome());
    }
}
