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

    /**
     * UC04 — o doador diz que tambem quer receber.
     *
     * Nao recebe papel no corpo de proposito: o endpoint concede BENEFICIARIO e
     * nada mais. Se o papel viesse do cliente, bastaria trocar uma palavra no
     * JSON para virar FARMACEUTICO e ganhar o balcao.
     *
     * Depois disto o papel mudou no banco, mas o token em maos ainda e o antigo
     * — quem chama precisa renovar a sessao em /autenticacao/renovacao para o
     * novo papel valer de fato.
     */
    @PostMapping("/eu/papeis/beneficiario")
    public ResponseEntity<Void> tambemQueroReceber() {
        servico.tornarBeneficiario(usuarioLogado.obrigatorio());
        return ResponseEntity.noContent().build();
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
