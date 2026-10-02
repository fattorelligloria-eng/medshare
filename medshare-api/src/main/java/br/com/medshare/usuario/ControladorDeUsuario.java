package br.com.medshare.usuario;

import br.com.medshare.seguranca.UsuarioLogado;
import br.com.medshare.usuario.dto.MeuImpacto;
import br.com.medshare.usuario.dto.MeusDados;
import br.com.medshare.usuario.dto.PedidoDeAtualizacaoDeCadastro;
import br.com.medshare.usuario.dto.PedidoDeNovaSenha;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * A conta de quem esta logado.
 *
 * Todo endpoint aqui age sobre o proprio usuario da sessao — nao existe
 * "/usuarios/{id}". Assim nao ha como errar e deixar uma pessoa ler ou editar
 * o cadastro de outra: o id nunca vem do cliente.
 */
@RestController
@RequestMapping("/api/usuarios")
public class ControladorDeUsuario {

    private final ServicoDeUsuario servico;
    private final UsuarioLogado usuarioLogado;

    public ControladorDeUsuario(ServicoDeUsuario servico, UsuarioLogado usuarioLogado) {
        this.servico = servico;
        this.usuarioLogado = usuarioLogado;
    }

    @GetMapping("/eu")
    public MeusDados meusDados() {
        return MeusDados.de(usuarioLogado.obrigatorio());
    }

    @PutMapping("/eu")
    public MeusDados atualizar(@Valid @RequestBody PedidoDeAtualizacaoDeCadastro pedido) {
        return MeusDados.de(servico.atualizar(usuarioLogado.obrigatorio(), pedido));
    }

    @PostMapping("/eu/senha")
    public ResponseEntity<Void> trocarSenha(@Valid @RequestBody PedidoDeNovaSenha pedido) {
        servico.trocarSenha(usuarioLogado.obrigatorio(), pedido);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/eu/impacto")
    @PreAuthorize("hasRole('DOADOR')")
    public MeuImpacto meuImpacto() {
        return servico.impactoDe(usuarioLogado.obrigatorio());
    }
}
