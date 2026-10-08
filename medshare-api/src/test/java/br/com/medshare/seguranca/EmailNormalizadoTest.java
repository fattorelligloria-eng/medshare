package br.com.medshare.seguranca;

import br.com.medshare.comum.RegraDeNegocioViolada;
import br.com.medshare.usuario.ServicoDeEndereco;
import br.com.medshare.usuario.UsuarioRepository;
import br.com.medshare.seguranca.dto.PedidoDeCadastro;
import br.com.medshare.seguranca.dto.PedidoDeLogin;
import br.com.medshare.usuario.Papel;
import br.com.medshare.usuario.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("E-mail sem diferenca de maiusculas")
class EmailNormalizadoTest {

    @Test
    @DisplayName("normaliza para minusculas e tira espacos das pontas")
    void normaliza() {
        assertThat(ServicoDeAutenticacao.normalizar("  Ana@MedShare.Test ")).isEqualTo("ana@medshare.test");
        assertThat(ServicoDeAutenticacao.normalizar(null)).isNull();
    }

    @Test
    @DisplayName("login com maiusculas procura a conta em minusculas")
    void loginComMaiusculas() {
        AuthenticationManager autenticador = Mockito.mock(AuthenticationManager.class);
        when(autenticador.authenticate(any())).thenThrow(new BadCredentialsException("x"));
        ServicoDeAutenticacao servico = new ServicoDeAutenticacao(
                Mockito.mock(UsuarioRepository.class), Mockito.mock(ServicoDeEndereco.class),
                Mockito.mock(PasswordEncoder.class), autenticador, Mockito.mock(ServicoDeToken.class));

        assertThatThrownBy(() -> servico.entrar(new PedidoDeLogin("Ana@MedShare.test", "senha-qualquer")))
                .isInstanceOf(RegraDeNegocioViolada.class);

        ArgumentCaptor<Authentication> enviado = ArgumentCaptor.forClass(Authentication.class);
        verify(autenticador).authenticate(enviado.capture());
        assertThat(((UsernamePasswordAuthenticationToken) enviado.getValue()).getPrincipal())
                .isEqualTo("ana@medshare.test");
    }

    @Test
    @DisplayName("cadastro grava o e-mail em minusculas e a conta nasce so doadora, mesmo pedindo para receber")
    void cadastroSoDoador() {
        UsuarioRepository usuarios = Mockito.mock(UsuarioRepository.class);
        when(usuarios.save(any())).thenAnswer(chamada -> chamada.getArgument(0));
        ServicoDeAutenticacao servico = new ServicoDeAutenticacao(
                usuarios, Mockito.mock(ServicoDeEndereco.class),
                Mockito.mock(PasswordEncoder.class), Mockito.mock(AuthenticationManager.class),
                Mockito.mock(ServicoDeToken.class));

        servico.cadastrar(new PedidoDeCadastro("Joana", "55566677788", "Joana@Exemplo.com", "senha-forte",
                null, "01310100", "Av. Paulista", "1000", null, "Bela Vista", (short) 1, null, null,
                Set.of(Papel.DOADOR, Papel.BENEFICIARIO)));

        ArgumentCaptor<Usuario> gravado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(gravado.capture());
        assertThat(gravado.getValue().getEmail()).isEqualTo("joana@exemplo.com");
        assertThat(gravado.getValue().getPapeis()).containsExactly(Papel.DOADOR);
    }

    @Test
    @DisplayName("cadastro pedindo papel da equipe continua recusado")
    void cadastroComPapelRestrito() {
        ServicoDeAutenticacao servico = new ServicoDeAutenticacao(
                Mockito.mock(UsuarioRepository.class), Mockito.mock(ServicoDeEndereco.class),
                Mockito.mock(PasswordEncoder.class), Mockito.mock(AuthenticationManager.class),
                Mockito.mock(ServicoDeToken.class));

        assertThatThrownBy(() -> servico.cadastrar(new PedidoDeCadastro("Joana", "55566677788",
                "joana@exemplo.com", "senha-forte", null, "01310100", "Av. Paulista", "1000", null,
                "Bela Vista", (short) 1, null, null, Set.of(Papel.ADMIN))))
                .isInstanceOf(RegraDeNegocioViolada.class);
    }
}
