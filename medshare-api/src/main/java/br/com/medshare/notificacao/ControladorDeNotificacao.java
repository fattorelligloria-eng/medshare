package br.com.medshare.notificacao;

import br.com.medshare.comum.RecursoNaoEncontrado;
import br.com.medshare.seguranca.UsuarioLogado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/notificacoes")
public class ControladorDeNotificacao {

    private final NotificacaoRepository notificacoes;
    private final UsuarioLogado usuarioLogado;

    public ControladorDeNotificacao(NotificacaoRepository notificacoes,
                                    UsuarioLogado usuarioLogado) {
        this.notificacoes = notificacoes;
        this.usuarioLogado = usuarioLogado;
    }

    public record NotificacaoResumida(Long id, String titulo, String corpo, String tipo,
                                      boolean lida, OffsetDateTime quando) { }

    @GetMapping
    public Page<NotificacaoResumida> minhas(Pageable pagina) {
        return notificacoes
                .findByUsuarioIdOrderByCriadoEmDesc(usuarioLogado.obrigatorio().getId(), pagina)
                .map(n -> new NotificacaoResumida(n.getId(), n.getTitulo(), n.getCorpo(),
                        n.getTipo().name(), n.isLida(), n.getCriadoEm()));
    }

    @GetMapping("/nao-lidas")
    public Map<String, Long> quantidadeNaoLida() {
        return Map.of("quantidade",
                notificacoes.countByUsuarioIdAndLidaFalse(usuarioLogado.obrigatorio().getId()));
    }

    @PostMapping("/{id}/leitura")
    @Transactional
    public void marcarComoLida(@PathVariable Long id) {
        Long donoDaSessao = usuarioLogado.obrigatorio().getId();
        Notificacao notificacao = notificacoes.findById(id)
                .filter(n -> n.getUsuario().getId().equals(donoDaSessao))
                .orElseThrow(() -> new RecursoNaoEncontrado("Notificacao", id));
        notificacao.marcarComoLida();
    }
}
