package br.com.medshare.doacao;

import br.com.medshare.comum.RegraDeNegocioViolada;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Validacao presencial (RN01)")
class ValidacaoTest {

    @Test
    @DisplayName("aprovar sempre grava lacre integro e dados conferidos")
    void aprovarExigeLacre() {
        Validacao validacao = Validacao.aprovar(null, null);

        assertThat(validacao.isAprovada()).isTrue();
        assertThat(validacao.isLacreIntegro()).isTrue();
        assertThat(validacao.isDadosConferem()).isTrue();
        assertThat(validacao.getMotivoRejeicao()).isNull();
    }

    @Test
    @DisplayName("rejeitar sem motivo nao e permitido")
    void rejeicaoExigeMotivo() {
        assertThatThrownBy(() -> Validacao.rejeitar(null, null, false, true, "  "))
                .isInstanceOf(RegraDeNegocioViolada.class)
                .extracting("regra").isEqualTo("RN01");
    }

    @Test
    @DisplayName("rejeicao guarda o que exatamente falhou na conferencia")
    void rejeicaoGuardaODetalhe() {
        Validacao validacao = Validacao.rejeitar(null, null, false, true, "selo rompido");

        assertThat(validacao.isAprovada()).isFalse();
        assertThat(validacao.isLacreIntegro()).isFalse();
        assertThat(validacao.getMotivoRejeicao()).isEqualTo("selo rompido");
    }
}
