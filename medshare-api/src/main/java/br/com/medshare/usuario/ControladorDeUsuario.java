package br.com.medshare.usuario;

import br.com.medshare.seguranca.ServicoDeAutenticacao;
import br.com.medshare.seguranca.UsuarioLogado;
import br.com.medshare.seguranca.dto.RespostaDeLogin;
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
    private final ServicoDeAutenticacao autenticacao;
    private final UsuarioLogado usuarioLogado;

    public ControladorDeUsuario(ServicoDeUsuario servico, ServicoDeAutenticacao autenticacao,
                                UsuarioLogado usuarioLogado) {
        this.servico = servico;
        this.autenticacao = autenticacao;
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
     * Troca a senha e devolve tokens novos. Os antigos param de valer em todo
     * aparelho (ver Usuario.aceitaTokenEmitidoEm); quem trocou continua
     * conectado com os que vem nesta resposta.
     */
    @PostMapping("/eu/senha")
    public RespostaDeLogin trocarSenha(@Valid @RequestBody PedidoDeNovaSenha pedido) {
        Usuario usuario = usuarioLogado.obrigatorio();
        servico.trocarSenha(usuario, pedido);
        return autenticacao.novaSessao(usuario);
    }

    @GetMapping("/eu/impacto")
    @PreAuthorize("hasRole('DOADOR')")
    public MeuImpacto meuImpacto() {
        return servico.impactoDe(usuarioLogado.obrigatorio());
    }
}
