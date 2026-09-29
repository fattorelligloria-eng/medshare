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

import java.util.Set;

@Service
public class ServicoDeAutenticacao {

    private static final Set<Papel> PAPEIS_DE_AUTOCADASTRO = Set.of(Papel.DOADOR, Papel.BENEFICIARIO);
    private static final String CONTA_DESATIVADA =
            "Esta conta está desativada. Fale com a equipe do MedShare.";

    private final UsuarioRepository usuarios;
    private final MunicipioRepository municipios;
    private final PasswordEncoder codificador;
    private final AuthenticationManager autenticador;
    private final ServicoDeToken tokens;

    public ServicoDeAutenticacao(UsuarioRepository usuarios, MunicipioRepository municipios,
                                 PasswordEncoder codificador, AuthenticationManager autenticador,
                                 ServicoDeToken tokens) {
        this.usuarios = usuarios;
        this.municipios = municipios;
        this.codificador = codificador;
        this.autenticador = autenticador;
        this.tokens = tokens;
    }

    @Transactional
    public RespostaDeLogin cadastrar(PedidoDeCadastro pedido) {
        recusarPapelRestrito(pedido.papeis());
        recusarCadastroDuplicado(pedido);

        // RN09: se o municipio nao esta na tabela da Grande SP, nao existe cadastro.
        Municipio municipio = municipios.findById(pedido.municipioId())
                .orElseThrow(() -> new RegraDeNegocioViolada("RN09",
                        "O MedShare atende apenas os 39 municípios da Grande São Paulo"));

        Endereco endereco = new Endereco(pedido.cep(), pedido.logradouro(), pedido.numero(),
                pedido.complemento(), pedido.bairro(), municipio,
                pedido.latitude(), pedido.longitude());

        Usuario novo = new Usuario(pedido.nome(), pedido.cpf(), pedido.email(),
                codificador.encode(pedido.senha()), pedido.telefone(), endereco, pedido.papeis());

        return montarResposta(usuarios.save(novo));
    }

    public RespostaDeLogin entrar(PedidoDeLogin pedido) {
        try {
            autenticador.authenticate(
                    new UsernamePasswordAuthenticationToken(pedido.email(), pedido.senha()));
        } catch (BadCredentialsException e) {
            throw new RegraDeNegocioViolada("AUTENTICACAO", "E-mail ou senha incorretos");
        } catch (AccountStatusException e) {
            // Conta desativada: sem isso a excecao subia como erro 500.
            throw new RegraDeNegocioViolada("AUTENTICACAO", CONTA_DESATIVADA);
        }
        Usuario usuario = usuarios.findByEmail(pedido.email())
                .orElseThrow(() -> new RecursoNaoEncontrado("Usuario", pedido.email()));
        return montarResposta(usuario);
    }

    public RespostaDeLogin renovar(String tokenDeRenovacao) {
        String email = tokens.emailDoTokenDeRenovacao(tokenDeRenovacao)
                .orElseThrow(() -> new RegraDeNegocioViolada("AUTENTICACAO",
                        "Token de renovação inválido ou expirado. Faça login novamente."));
        Usuario usuario = usuarios.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontrado("Usuario", email));
        // O token de renovacao dura 14 dias; desativar a conta precisa valer antes disso.
        if (!usuario.isAtivo()) {
            throw new RegraDeNegocioViolada("AUTENTICACAO", CONTA_DESATIVADA);
        }
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
        if (usuarios.existsByEmail(pedido.email())) {
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
}
