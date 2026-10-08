package br.com.medshare.integracao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Conteudo da foto confere com o tipo declarado")
class ConteudoDaFotoTest {

    private static MockMultipartFile arquivo(String tipo, byte[] bytes) {
        return new MockMultipartFile("arquivo", "foto", tipo, bytes);
    }

    @Test
    @DisplayName("JPEG, PNG e WebP de verdade passam")
    void imagensDeVerdade() {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
        byte[] webp = {'R', 'I', 'F', 'F', 1, 2, 3, 4, 'W', 'E', 'B', 'P'};

        assertThat(ControladorDeFotos.conteudoEDoTipo(arquivo("image/jpeg", jpeg), ".jpg")).isTrue();
        assertThat(ControladorDeFotos.conteudoEDoTipo(arquivo("image/png", png), ".png")).isTrue();
        assertThat(ControladorDeFotos.conteudoEDoTipo(arquivo("image/webp", webp), ".webp")).isTrue();
    }

    @Test
    @DisplayName("HTML chamado de image/jpeg e recusado")
    void htmlDisfarcado() {
        byte[] html = "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8);
        assertThat(ControladorDeFotos.conteudoEDoTipo(arquivo("image/jpeg", html), ".jpg")).isFalse();
    }

    @Test
    @DisplayName("PNG declarado como JPEG e recusado")
    void tipoTrocado() {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        assertThat(ControladorDeFotos.conteudoEDoTipo(arquivo("image/jpeg", png), ".jpg")).isFalse();
    }

    @Test
    @DisplayName("arquivo curto demais e recusado")
    void curtoDemais() {
        assertThat(ControladorDeFotos.conteudoEDoTipo(arquivo("image/webp", new byte[]{'R', 'I'}), ".webp")).isFalse();
    }
}
