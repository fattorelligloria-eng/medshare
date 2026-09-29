package br.com.medshare.reserva;

import br.com.medshare.comum.RegraDeNegocioViolada;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Entrega (RN03)")
class EntregaTest {

    @Test
    @DisplayName("sem conferir a receita, a entrega nem chega a existir")
    void semReceitaNaoEntrega() {
        assertThatThrownBy(() -> new Entrega(null, null, false, true))
                .isInstanceOf(RegraDeNegocioViolada.class)
                .hasMessageContaining("receita")
                .extracting("regra").isEqualTo("RN03");
    }

    @Test
    @DisplayName("sem conferir o documento, a entrega nem chega a existir")
    void semDocumentoNaoEntrega() {
        assertThatThrownBy(() -> new Entrega(null, null, true, false))
                .isInstanceOf(RegraDeNegocioViolada.class)
                .hasMessageContaining("documento");
    }

    @Test
    @DisplayName("com as duas conferencias, a entrega e registrada")
    void comAsDuasConferencias() {
        Entrega entrega = new Entrega(null, null, true, true);

        assertThat(entrega.isReceitaConferida()).isTrue();
        assertThat(entrega.isDocumentoConferido()).isTrue();
    }
}
