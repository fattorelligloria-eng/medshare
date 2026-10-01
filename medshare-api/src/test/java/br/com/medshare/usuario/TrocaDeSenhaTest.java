package br.com.medshare.usuario;

import br.com.medshare.comum.RegraDeNegocioViolada;
import br.com.medshare.usuario.dto.PedidoDeNovaSenha;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Troca de senha da propria conta")
class TrocaDeSenhaTest {

    // Fator 4 em vez de 12: o teste nao precisa da lentidao que protege o banco.
    private final PasswordEncoder codificador = new BCryptPasswordEncoder(4);
    private final UsuarioRepository usuarios = Mockito.mock(UsuarioRepository.class);

    private final ServicoDeUsuario servico = new ServicoDeUsuario(
            usuarios, Mockito.mock(ServicoDeEndereco.class),
            Mockito.mock(br.com.medshare.doacao.DoacaoRepository.class), codificador);

    private Usuario comSenha(String senha) {
        return new Usuario("Ana Doadora", "11122233344", "ana@medshare.test",
                codificador.encode(senha), "11999990000", null,
                EnumSet.of(Papel.DOADOR));
    }

    @Test
    @DisplayName("troca quando a senha atual confere e a nova e diferente")
    void trocaFeliz() {
        Usuario ana = comSenha("senhaAntiga1");

        servico.trocarSenha(ana, new PedidoDeNovaSenha("senhaAntiga1", "senhaNova12345"));

        assertThat(codificador.matches("senhaNova12345", ana.getSenhaHash())).isTrue();
        assertThat(codificador.matches("senhaAntiga1", ana.getSenhaHash())).isFalse();
    }

    @Test
    @DisplayName("recusa quando a senha atual esta errada")
    void senhaAtualErrada() {
        Usuario ana = comSenha("senhaAntiga1");

        assertThatThrownBy(() ->
                servico.trocarSenha(ana, new PedidoDeNovaSenha("chutei", "senhaNova12345")))
                .isInstanceOf(RegraDeNegocioViolada.class)
                .hasMessageContaining("não confere");

        // A senha nao pode ter mudado por uma tentativa errada.
        assertThat(codificador.matches("senhaAntiga1", ana.getSenhaHash())).isTrue();
    }

    @Test
    @DisplayName("recusa quando a nova senha e igual a anterior — decisao da Gloria")
    void novaIgualAAnterior() {
        Usuario ana = comSenha("senhaAntiga1");

        assertThatThrownBy(() ->
                servico.trocarSenha(ana, new PedidoDeNovaSenha("senhaAntiga1", "senhaAntiga1")))
                .isInstanceOf(RegraDeNegocioViolada.class)
                .hasMessageContaining("diferente da anterior");
    }
}
