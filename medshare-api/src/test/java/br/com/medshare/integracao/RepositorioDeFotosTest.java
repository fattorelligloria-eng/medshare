package br.com.medshare.integracao;

import br.com.medshare.comum.RegraDeNegocioViolada;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Repositorio de fotos: so aceita fotos enviadas pelo proprio MedShare")
class RepositorioDeFotosTest {

    private static final String ENDERECO = "http://192.168.0.10:8080/fotos-locais";

    @TempDir
    Path pasta;

    private RepositorioDeFotos fotos;
    private String nomeGravado;

    @BeforeEach
    void preparar() throws Exception {
        fotos = new RepositorioDeFotos(pasta.toString(), ENDERECO + "/");
        nomeGravado = fotos.novoNome(".png");
        Files.write(pasta.resolve(nomeGravado), new byte[] {1, 2, 3});
    }

    @Test
    @DisplayName("aceita a URL que o envio devolveu, e acha o arquivo em disco")
    void aceitaFotoNossa() {
        String url = fotos.urlDe(nomeGravado);

        assertThat(fotos.ehFotoNossa(url)).isTrue();
        assertThat(fotos.arquivoDa(url)).contains(pasta.resolve(nomeGravado).toAbsolutePath().normalize());
        assertThat(fotos.tipoDa(url)).isEqualTo("image/png");
    }

    @Test
    @DisplayName("recusa endereco de outro servidor, mesmo com o mesmo nome de arquivo")
    void recusaOutroServidor() {
        assertThat(fotos.ehFotoNossa("http://169.254.169.254/latest/meta-data/" + nomeGravado)).isFalse();
        assertThat(fotos.ehFotoNossa("http://outro.site/fotos-locais/" + nomeGravado)).isFalse();
    }

    @Test
    @DisplayName("recusa tentativa de sair da pasta de fotos")
    void recusaCaminhoRelativo() {
        assertThat(fotos.ehFotoNossa(ENDERECO + "/../application.yml")).isFalse();
        assertThat(fotos.ehFotoNossa(ENDERECO + "/..%2F..%2Fsegredo.jpg")).isFalse();
    }

    @Test
    @DisplayName("recusa nome valido que nao existe em disco")
    void recusaArquivoInexistente() {
        assertThat(fotos.ehFotoNossa(fotos.urlDe(fotos.novoNome(".jpg")))).isFalse();
    }

    @Test
    @DisplayName("exigirFotoNossa barra com regra FOTO")
    void exigirBarraComRegra() {
        assertThatThrownBy(() -> fotos.exigirFotoNossa("http://x/y.jpg"))
                .isInstanceOf(RegraDeNegocioViolada.class)
                .extracting("regra").isEqualTo("FOTO");
    }
}
