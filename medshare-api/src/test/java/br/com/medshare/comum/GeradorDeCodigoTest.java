package br.com.medshare.comum;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Gerador de codigo")
class GeradorDeCodigoTest {

    private final GeradorDeCodigo gerador = new GeradorDeCodigo();

    @Test
    @DisplayName("nao usa caracteres que se confundem ao ditar no balcao")
    void semCaracteresAmbiguos() {
        for (int i = 0; i < 500; i++) {
            assertThat(gerador.paraRetirada()).doesNotContain("O", "0", "I", "1");
        }
    }

    @Test
    @DisplayName("o codigo da doacao segue o padrao MS-XXXXXX")
    void padraoDoCodigoDeDoacao() {
        assertThat(gerador.paraDoacao()).matches("MS-[A-Z2-9]{6}");
    }

    @Test
    @DisplayName("nao repete codigo em 10 mil sorteios")
    void naoRepete() {
        Set<String> sorteados = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            sorteados.add(gerador.paraRetirada());
        }
        assertThat(sorteados).hasSize(10_000);
    }
}
