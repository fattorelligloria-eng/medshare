package br.com.medshare.seguranca;

import br.com.medshare.comum.RecursoNaoEncontrado;
import br.com.medshare.comum.RegraDeNegocioViolada;
import br.com.medshare.seguranca.dto.PedidoDeCadastro;
import br.com.medshare.seguranca.dto.PedidoDeLogin;
import br.com.medshare.seguranca.dto.RespostaDeLogin;
import br.com.medshare.usuario.*;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;

@Service
public class ServicoDeAutenticacao {

    private static final Set<Papel> PAPEIS_DE_AUTOCADASTRO = Set.of(Papel.DOADOR, Papel.BENEFICIARIO);
    private static final String CONTA_DESATIVADA =
            "Esta conta está desativada. Fale com a equipe do MedShare.";
    private static final String TOKEN_DE_RENOVACAO_INVALIDO =
            "Token de renovação inválido ou expirado. Faça login novamente.";

    private final UsuarioRepository usuarios;
    private final ServicoDeEndereco enderecos;
    private final PasswordEncoder codificador;
    private final AuthenticationManager autenticador;
    private final ServicoDeToken tokens;

    public ServicoDeAutenticacao(UsuarioRepository usuarios, ServicoDeEndereco enderecos,
                                 PasswordEncoder codificador, AuthenticationManager autenticador,
                                 ServicoDeToken tokens) {
        this.usuarios = usuarios;
        this.enderecos = enderecos;
        this.codificador = codificador;
        this.autenticador = autenticador;
        this.tokens = tokens;
    }

    @Transactional
    public RespostaDeLogin cadastrar(PedidoDeCadastro pedido) {
        recusarPapelRestrito(pedido.papeis());
        recusarCadastroDuplicado(pedido);

        // RN09: municipio da Grande SP, conferido contra o CEP; e as coordenadas
        // que o matching usa para medir distancia.
        Endereco endereco = enderecos.montar(pedido.cep(), pedido.logradouro(), pedido.numero(),
                pedido.complemento(), pedido.bairro(), pedido.municipioId(),
                pedido.latitude(), pedido.longitude());

        // Toda conta nasce so doadora, mesmo quem marcou "quero receber": o papel
        // de beneficiario vem apenas da confirmacao do NIS (ServicoDeNecessidade).
        // Quem quer receber e levado a tela do CadUnico logo depois do cadastro.
        Usuario novo = new Usuario(pedido.nome(), pedido.cpf(), normalizar(pedido.email()),
                codificador.encode(pedido.senha()), pedido.telefone(), endereco,
                EnumSet.of(Papel.DOADOR));

        return montarResposta(usuarios.save(novo));
    }

    public RespostaDeLogin entrar(PedidoDeLogin pedido) {
        try {
            autenticador.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizar(pedido.email()), pedido.senha()));
        } catch (BadCredentialsException e) {
            throw new RegraDeNegocioViolada("AUTENTICACAO", "E-mail ou senha incorretos");
        } catch (AccountStatusException e) {
            // Conta desativada: sem isso a excecao subia como erro 500.
            throw new RegraDeNegocioViolada("AUTENTICACAO", CONTA_DESATIVADA);
        }
        Usuario usuario = usuarios.findByEmail(normalizar(pedido.email()))
                .orElseThrow(() -> new RecursoNaoEncontrado("Usuario", pedido.email()));
        return montarResposta(usuario);
    }

    public RespostaDeLogin renovar(String tokenDeRenovacao) {
        ServicoDeToken.TokenLido token = tokens.lerTokenDeRenovacao(tokenDeRenovacao)
                .orElseThrow(() -> new RegraDeNegocioViolada("AUTENTICACAO", TOKEN_DE_RENOVACAO_INVALIDO));
        Usuario usuario = usuarios.findByEmail(token.email())
                .orElseThrow(() -> new RecursoNaoEncontrado("Usuario", token.email()));
        // O token de renovacao dura 14 dias; desativar a conta precisa valer antes disso.
        if (!usuario.isAtivo()) {
            throw new RegraDeNegocioViolada("AUTENTICACAO", CONTA_DESATIVADA);
        }
        // E trocar a senha tambem: senao o outro aparelho renovaria a sessao.
        if (!usuario.aceitaTokenEmitidoEm(token.emitidoEm())) {
            throw new RegraDeNegocioViolada("AUTENTICACAO", TOKEN_DE_RENOVACAO_INVALIDO);
        }
        return montarResposta(usuario);
    }

    /** Tokens novos para quem acabou de trocar a senha, que continua conectado. */
    public RespostaDeLogin novaSessao(Usuario usuario) {
        return montarResposta(usuario);
    }

    /**
     * Farmaceutico e analista da central nao se cadastram sozinhos: esses
     * papeis dao acesso a conferencia de lacre e a decisao sobre doacoes, entao
     * so a equipe pode concede-los. Pelo app, a pessoa escolhe doar e/ou receber.
     */
    private void recusarPapelRestrito(Set<Papel> papeis) {
        if (!PAPEIS_DE_AUTOCADASTRO.containsAll(papeis)) {
            throw new RegraDeNegocioViolada("CADASTRO",
                    "O cadastro pelo aplicativo permite apenas doar e receber medicamentos");
        }
    }

    private void recusarCadastroDuplicado(PedidoDeCadastro pedido) {
        if (usuarios.existsByEmail(normalizar(pedido.email()))) {
            throw new RegraDeNegocioViolada("CADASTRO", "Já existe uma conta com este e-mail");
        }
        if (usuarios.existsByCpf(pedido.cpf())) {
            throw new RegraDeNegocioViolada("CADASTRO", "Já existe uma conta com este CPF");
        }
    }

    private RespostaDeLogin montarResposta(Usuario usuario) {
        return new RespostaDeLogin(
                tokens.emitirTokenDeAcesso(usuario),
                tokens.emitirTokenDeRenovacao(usuario),
                tokens.segundosDeValidadeDoAcesso(),
                usuario.getId(),
                usuario.getNome(),
                UsuarioAutenticado.papeisDe(usuario));
    }

    /**
     * O e-mail e gravado e procurado sempre em minusculas, sem espacos nas
     * pontas. Sem isso "Ana@..." nao entrava na conta de "ana@...", e o
     * cadastro aceitava as duas grafias como contas diferentes. O vinculo de
     * farmaceutico (ServicoDeAdministracao) ja procurava assim.
     */
    static String normalizar(String email) {
        return email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
