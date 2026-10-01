package br.com.medshare.usuario.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Os dados da propria conta")
class MeusDadosTest {

    @Test
    @DisplayName("o CPF sai mascarado, nunca inteiro")
    void cpfMascarado() {
        // A pessoa ja sabe o CPF dela; a tela so precisa confirmar que o
        // cadastro e o certo. Mandar os 11 digitos a cada abertura de tela
        // seria risco sem ganho.
        assertThat(MeusDados.mascarar("12345678901")).isEqualTo("•••.•••.789-01");
    }

    @Test
    @DisplayName("CPF ausente ou estranho nao vira string meia-boca")
    void semCpf() {
        assertThat(MeusDados.mascarar(null)).isNull();
        assertThat(MeusDados.mascarar("123")).isNull();
        assertThat(MeusDados.mascarar("")).isNull();
    }
}
