package br.com.medshare.seguranca;

import br.com.medshare.comum.RecursoNaoEncontrado;
import br.com.medshare.comum.RegraDeNegocioViolada;
import br.com.medshare.seguranca.dto.PedidoDeCadastro;
import br.com.medshare.seguranca.dto.PedidoDeLogin;
import br.com.medshare.seguranca.dto.RespostaDeLogin;
import br.com.medshare.usuario.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoDeAutenticacao {

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
        return montarResposta(usuario);
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
