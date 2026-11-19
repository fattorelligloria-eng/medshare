package br.com.medshare.atendimento;

import br.com.medshare.atendimento.dto.MensagemDaPessoa;
import br.com.medshare.atendimento.dto.MensagemNaCentral;
import br.com.medshare.atendimento.dto.PedidoDeMensagem;
import br.com.medshare.atendimento.dto.PedidoDeResposta;
import br.com.medshare.seguranca.UsuarioLogado;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Atendimento: o lado de quem escreve (/api/atendimento) e o da central
 * (/api/central/atendimento).
 *
 * Do lado da pessoa, tudo age sobre as mensagens dela mesma — o id do autor
 * nunca vem do cliente.
 */
@RestController
public class ControladorDeAtendimento {

    private final ServicoDeAtendimento servico;
    private final UsuarioLogado usuarioLogado;

    public ControladorDeAtendimento(ServicoDeAtendimento servico, UsuarioLogado usuarioLogado) {
        this.servico = servico;
        this.usuarioLogado = usuarioLogado;
    }

    @PostMapping("/api/atendimento/mensagens")
    @ResponseStatus(HttpStatus.CREATED)
    public MensagemDaPessoa enviar(@Valid @RequestBody PedidoDeMensagem pedido) {
        return MensagemDaPessoa.de(servico.enviar(usuarioLogado.obrigatorio(), pedido.assunto(), pedido.texto()));
    }

    @GetMapping("/api/atendimento/mensagens")
    public List<MensagemDaPessoa> minhas() {
        return servico.minhas(usuarioLogado.obrigatorio()).stream().map(MensagemDaPessoa::de).toList();
    }

    @GetMapping("/api/central/atendimento")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Page<MensagemNaCentral> semResposta(Pageable pagina) {
        return servico.semResposta(pagina).map(MensagemNaCentral::de);
    }

    @PostMapping("/api/central/atendimento/{id}/resposta")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void responder(@PathVariable Long id, @Valid @RequestBody PedidoDeResposta pedido) {
        servico.responder(id, pedido.texto(), usuarioLogado.obrigatorio());
    }
}
