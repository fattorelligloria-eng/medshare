package br.com.medshare.necessidade;

import br.com.medshare.usuario.Papel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static br.com.medshare.comum.FabricaDeTestes.usuario;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Verificacao no CadUnico (RN08)")
class VerificacaoCadUnicoTest {

    private VerificacaoCadUnico verificacao(boolean confirmado, int meses) {
        return new VerificacaoCadUnico(usuario(1L, "Bruno", Papel.BENEFICIARIO),
                "12345678901", confirmado, meses, "TESTE");
    }

    @Test
    @DisplayName("confirmada e dentro do prazo esta vigente")
    void confirmadaEstaVigente() {
        assertThat(verificacao(true, 12).estaVigente()).isTrue();
    }

    @Test
    @DisplayName("nao confirmada nunca esta vigente, mesmo dentro do prazo")
    void naoConfirmadaNaoVale() {
        assertThat(verificacao(false, 12).estaVigente()).isFalse();
    }

    @Test
    @DisplayName("passados os 12 meses, precisa verificar de novo")
    void vencePassadosDozeMeses() {
        VerificacaoCadUnico verificacao = verificacao(true, 12);
        ReflectionTestUtils.setField(verificacao, "validoAte", LocalDate.now().minusDays(1));

        assertThat(verificacao.estaVigente()).isFalse();
    }

    @Test
    @DisplayName("a validade e de 12 meses a partir da consulta")
    void validadeDeDozeMeses() {
        assertThat(verificacao(true, 12).getValidoAte())
                .isEqualTo(LocalDate.now().plusMonths(12));
    }
}
