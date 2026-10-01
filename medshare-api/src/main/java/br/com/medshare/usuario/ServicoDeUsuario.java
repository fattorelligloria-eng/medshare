package br.com.medshare.usuario;

import br.com.medshare.comum.RegraDeNegocioViolada;
import br.com.medshare.doacao.DoacaoRepository;
import br.com.medshare.doacao.StatusDoacao;
import br.com.medshare.usuario.dto.MeuImpacto;
import br.com.medshare.usuario.dto.PedidoDeAtualizacaoDeCadastro;
import br.com.medshare.usuario.dto.PedidoDeNovaSenha;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A conta da propria pessoa: ver, corrigir e trocar a senha. */
@Service
public class ServicoDeUsuario {

    private final UsuarioRepository usuarios;
    private final ServicoDeEndereco enderecos;
    private final DoacaoRepository doacoes;
    private final PasswordEncoder codificador;

    public ServicoDeUsuario(UsuarioRepository usuarios, ServicoDeEndereco enderecos,
                            DoacaoRepository doacoes, PasswordEncoder codificador) {
        this.usuarios = usuarios;
        this.enderecos = enderecos;
        this.doacoes = doacoes;
        this.codificador = codificador;
    }

    /**
     * RN09 vale aqui tambem. Mudar de endereco e sair da Grande Sao Paulo tem
     * que esbarrar na mesma regra que o cadastro — senao a pessoa entra pela
     * regra e escapa dela pela edicao.
     */
    @Transactional
    public Usuario atualizar(Usuario usuario, PedidoDeAtualizacaoDeCadastro pedido) {
        Endereco endereco = enderecos.montar(pedido.cep(), pedido.logradouro(), pedido.numero(),
                pedido.complemento(), pedido.bairro(), pedido.municipioId(),
                pedido.latitude(), pedido.longitude());

        usuario.atualizarCadastro(pedido.nome(), pedido.telefone(), endereco);
        return usuarios.save(usuario);
    }

    @Transactional
    public void trocarSenha(Usuario usuario, PedidoDeNovaSenha pedido) {
        // Sem conferir a senha atual, quem pegasse o celular destravado trocaria
        // a senha e tomaria a conta.
        if (!codificador.matches(pedido.senhaAtual(), usuario.getSenhaHash())) {
            throw new RegraDeNegocioViolada("AUTENTICACAO", "A senha atual não confere.");
        }
        // Decisao da Gloria: a nova nao pode ser a mesma de antes.
        if (codificador.matches(pedido.novaSenha(), usuario.getSenhaHash())) {
            throw new RegraDeNegocioViolada("SENHA",
                    "A nova senha precisa ser diferente da anterior.");
        }

        usuario.trocarSenha(codificador.encode(pedido.novaSenha()));
        usuarios.save(usuario);
    }

    @Transactional(readOnly = true)
    public MeuImpacto impactoDe(Usuario doador) {
        long total = doacoes.countByDoadorId(doador.getId());
        long entregues = doacoes.countByDoadorIdAndStatus(doador.getId(), StatusDoacao.ENTREGUE);
        var valor = doacoes.valorEntreguePeloDoador(doador.getId());
        return new MeuImpacto(total, entregues, entregues == 0 ? null : valor);
    }
}
