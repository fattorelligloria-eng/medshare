package br.com.medshare.usuario;

import br.com.medshare.comum.FabricaDeTestes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UC04 — o doador que tambem passa a receber.
 *
 * O ponto delicado aqui e de permissao: este caminho acrescenta um papel ao
 * proprio usuario. Os testes travam que ele acrescenta um papel so, nunca os
 * que dao acesso ao balcao e a central.
 */
class TornarBeneficiarioTest {

    private UsuarioRepository usuarios;
    private ServicoDeUsuario servico;

    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepository.class);
        servico = new ServicoDeUsuario(usuarios, mock(ServicoDeEndereco.class),
                mock(br.com.medshare.doacao.DoacaoRepository.class),
                mock(org.springframework.security.crypto.password.PasswordEncoder.class));
    }

    @Test
    @DisplayName("O doador ganha o papel de beneficiario e continua doador")
    void doadorPassaAReceberSemDeixarDeDoar() {
        Usuario doador = FabricaDeTestes.usuario(1L, "Gloria Fattorelli", Papel.DOADOR);
        when(usuarios.findById(1L)).thenReturn(Optional.of(doador));

        boolean mudou = servico.tornarBeneficiario(doador);

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(salvo.capture());
        assertThat(salvo.getValue().getPapeis())
                .containsExactlyInAnyOrder(Papel.DOADOR, Papel.BENEFICIARIO);
        assertThat(mudou).isTrue();
    }

    @Test
    @DisplayName("Nunca concede FARMACEUTICO nem ADMIN")
    void naoConcedePapelRestrito() {
        Usuario doador = FabricaDeTestes.usuario(2L, "Carlos Lima", Papel.DOADOR);
        when(usuarios.findById(2L)).thenReturn(Optional.of(doador));

        servico.tornarBeneficiario(doador);

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(salvo.capture());
        assertThat(salvo.getValue().getPapeis())
                .doesNotContain(Papel.FARMACEUTICO, Papel.ADMIN);
    }

    @Test
    @DisplayName("Chamar duas vezes nao muda nada nem grava de novo")
    void ehIdempotente() {
        Usuario jaBeneficiario =
                FabricaDeTestes.usuario(3L, "Joana Silva", Papel.DOADOR, Papel.BENEFICIARIO);

        boolean mudou = servico.tornarBeneficiario(jaBeneficiario);

        assertThat(mudou).isFalse();
        verify(usuarios, never()).save(any());
    }
}
