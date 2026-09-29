package br.com.medshare.integracao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Verificacao do formato do NIS")
class ConsultaDeCadUnicoSimuladaTest {

    private final ConsultaDeCadUnicoSimulada consulta = new ConsultaDeCadUnicoSimulada();

    @Test
    @DisplayName("NIS bem formado passa no formato, mas nao confirma sozinho")
    void nisValidoNaoConfirmaSozinho() {
        // 1234567890 + digito calculado pelos pesos 3,2,9,8,7,6,5,4,3,2 mod 11
        var resultado = consulta.consultar("12345678900");

        assertThat(resultado.formatoValido()).isTrue();
        assertThat(resultado.confirmado()).isFalse();
        // Sem consulta oficial ninguem e aprovado automaticamente: o caso vai
        // para conferencia humana, que e o caminho seguro.
        assertThat(resultado.precisaDeAnaliseHumana()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678901", "00000000001", "98765432100"})
    @DisplayName("digito verificador errado e recusado na hora, sem ir para a central")
    void digitoErrado(String nis) {
        var resultado = consulta.consultar(nis);

        assertThat(resultado.formatoValido()).isFalse();
        assertThat(resultado.precisaDeAnaliseHumana()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "123456789012", "1234567890a", ""})
    @DisplayName("formato errado e recusado")
    void formatoErrado(String nis) {
        assertThat(consulta.consultar(nis).formatoValido()).isFalse();
    }

    @Test
    @DisplayName("deixa claro na observacao que a consulta oficial nao aconteceu")
    void avisaQueEhSimulado() {
        assertThat(consulta.consultar("12345678900").observacao()).contains("simulado");
        assertThat(consulta.fonte()).isEqualTo("SIMULADO");
    }
}
