package br.com.medshare.notificacao;

import br.com.medshare.usuario.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Avisos para o usuario.
 *
 * Grava no banco sempre (para o app listar) e, quando o FCM estiver
 * configurado, tambem dispara o push. Falhar em notificar nunca pode derrubar
 * a operacao que gerou o aviso — por isso o envio e best-effort e o registro
 * no banco e o que vale.
 */
@Service
public class ServicoDeNotificacao {

    private static final Logger log = LoggerFactory.getLogger(ServicoDeNotificacao.class);

    private final NotificacaoRepository notificacoes;

    public ServicoDeNotificacao(NotificacaoRepository notificacoes) {
        this.notificacoes = notificacoes;
    }

    @Transactional
    public void avisar(Usuario destinatario, TipoNotificacao tipo, String titulo, String corpo) {
        notificacoes.save(new Notificacao(destinatario, titulo, corpo, tipo));
        enviarPush(destinatario, titulo, corpo);
    }

    private void enviarPush(Usuario destinatario, String titulo, String corpo) {
        // Ponto de integracao do Firebase Cloud Messaging. Enquanto o app
        // Android nao registra o token do aparelho, o aviso fica so no banco.
        log.debug("Notificacao para o usuario {}: {} - {}", destinatario.getId(), titulo, corpo);
    }
}
