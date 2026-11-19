package br.com.medshare.atendimento;

import br.com.medshare.comum.RecursoNaoEncontrado;
import br.com.medshare.comum.RegraDeNegocioViolada;
import br.com.medshare.notificacao.ServicoDeNotificacao;
import br.com.medshare.notificacao.TipoNotificacao;
import br.com.medshare.usuario.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Mensagens para a central: a pessoa escreve, a central responde, a resposta
 * chega como notificacao.
 */
@Service
public class ServicoDeAtendimento {

    /**
     * Quantas mensagens sem resposta uma pessoa pode ter ao mesmo tempo.
     * Segura quem aperta "enviar" varias vezes, sem impedir ninguem de falar:
     * a quarta mensagem entra assim que a central responder uma.
     */
    static final int LIMITE_SEM_RESPOSTA = 3;

    private final MensagemDeAtendimentoRepository mensagens;
    private final ServicoDeNotificacao notificacoes;

    public ServicoDeAtendimento(MensagemDeAtendimentoRepository mensagens, ServicoDeNotificacao notificacoes) {
        this.mensagens = mensagens;
        this.notificacoes = notificacoes;
    }

    @Transactional
    public MensagemDeAtendimento enviar(Usuario autor, AssuntoDaMensagem assunto, String texto) {
        if (mensagens.countByUsuarioIdAndRespostaIsNull(autor.getId()) >= LIMITE_SEM_RESPOSTA) {
            throw new RegraDeNegocioViolada("ATENDIMENTO",
                    "Você já tem %d mensagens esperando resposta. Assim que a central responder, dá para mandar outra."
                            .formatted(LIMITE_SEM_RESPOSTA));
        }
        return mensagens.save(new MensagemDeAtendimento(autor, assunto, texto));
    }

    @Transactional(readOnly = true)
    public List<MensagemDeAtendimento> minhas(Usuario autor) {
        return mensagens.findTop20ByUsuarioIdOrderByCriadoEmDesc(autor.getId());
    }

    @Transactional(readOnly = true)
    public Page<MensagemDeAtendimento> semResposta(Pageable pagina) {
        return mensagens.findByRespostaIsNullOrderByCriadoEmAsc(pagina);
    }

    /**
     * A notificacao diz so que chegou resposta, nao o texto: aviso de celular
     * aparece na tela bloqueada, e a resposta pode falar de remedio e receita.
     */
    @Transactional
    public MensagemDeAtendimento responder(Long id, String resposta, Usuario quemResponde) {
        MensagemDeAtendimento mensagem = mensagens.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontrado("Mensagem", id));
        mensagem.responder(resposta, quemResponde);
        mensagens.save(mensagem);
        notificacoes.avisar(mensagem.getUsuario(), TipoNotificacao.ATENDIMENTO_RESPONDIDO,
                "A central respondeu sua mensagem",
                "Abra o Atendimento para ler a resposta.");
        return mensagem;
    }
}
