package br.com.medshare.comum;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.StandardEnvironment;

import java.io.InputStream;
import java.util.Properties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Leitura do arquivo .env")
class CarregadorDoEnvTest {

    private final CarregadorDoEnv carregador = new CarregadorDoEnv();

    private Path arquivoCom(Path pasta, String conteudo) throws IOException {
        Path arquivo = pasta.resolve(".env");
        Files.writeString(arquivo, conteudo, StandardCharsets.UTF_8);
        return arquivo;
    }

    @Test
    @DisplayName("le pares simples")
    void paresSimples(@TempDir Path pasta) throws IOException {
        var valores = carregador.ler(arquivoCom(pasta, """
                MEDSHARE_PORTA=8081
                MEDSHARE_BANCO_SENHA=abc123
                """));

        assertThat(valores)
                .containsEntry("MEDSHARE_PORTA", "8081")
                .containsEntry("MEDSHARE_BANCO_SENHA", "abc123");
    }

    @Test
    @DisplayName("ignora comentario e linha em branco")
    void comentarios(@TempDir Path pasta) throws IOException {
        var valores = carregador.ler(arquivoCom(pasta, """
                # Isto e um comentario
                
                MEDSHARE_PORTA=8081
                """));

        assertThat(valores).hasSize(1).containsEntry("MEDSHARE_PORTA", "8081");
    }

    @Test
    @DisplayName("linha sem valor fica de fora, para o padrao do application.yml valer")
    void semValor(@TempDir Path pasta) throws IOException {
        // `MEDSHARE_GEMINI_CHAVE=` e exatamente como o .env.exemplo vem. Se
        // isso virasse string vazia, a aplicacao acharia que tem uma chave.
        var valores = carregador.ler(arquivoCom(pasta, """
                MEDSHARE_GEMINI_CHAVE=
                MEDSHARE_PORTA=8081
                """));

        assertThat(valores).doesNotContainKey("MEDSHARE_GEMINI_CHAVE");
        assertThat(valores).containsEntry("MEDSHARE_PORTA", "8081");
    }

    @Test
    @DisplayName("tira as aspas de valor entre aspas")
    void aspas(@TempDir Path pasta) throws IOException {
        var valores = carregador.ler(arquivoCom(pasta, """
                COM_ASPAS="com espaco no meio"
                COM_APOSTROFO='outro valor'
                """));

        assertThat(valores)
                .containsEntry("COM_ASPAS", "com espaco no meio")
                .containsEntry("COM_APOSTROFO", "outro valor");
    }

    @Test
    @DisplayName("valor com = no meio nao e cortado")
    void igualNoMeio(@TempDir Path pasta) throws IOException {
        // Segredo de JWT em base64 quase sempre termina em '='.
        var valores = carregador.ler(arquivoCom(pasta, """
                MEDSHARE_JWT_SEGREDO=dGVzdGU=
                MEDSHARE_BANCO_URL=jdbc:postgresql://localhost:5433/medshare?ssl=false
                """));

        assertThat(valores)
                .containsEntry("MEDSHARE_JWT_SEGREDO", "dGVzdGU=")
                .containsEntry("MEDSHARE_BANCO_URL",
                        "jdbc:postgresql://localhost:5433/medshare?ssl=false");
    }

    @Test
    @DisplayName("arquivo ilegivel nao derruba a aplicacao")
    void arquivoQueNaoExiste(@TempDir Path pasta) {
        assertThat(carregador.ler(pasta.resolve("nao-existe"))).isEmpty();
    }

    @Test
    @DisplayName("o .env vira propriedade da aplicacao")
    void entraNoAmbiente(@TempDir Path pasta) throws IOException {
        arquivoCom(pasta, "MEDSHARE_PORTA=8081\n");
        var ambiente = new StandardEnvironment();

        new CarregadorDoEnv(pasta).postProcessEnvironment(ambiente, null);

        assertThat(ambiente.getProperty("MEDSHARE_PORTA")).isEqualTo("8081");
    }

    @Test
    @DisplayName("variavel de ambiente de verdade ganha do .env")
    void ambienteGanhaDoArquivo(@TempDir Path pasta) throws IOException {
        // Esta e a regra que protege producao: um .env esquecido na pasta do
        // servidor nao pode sobrescrever a credencial que veio do ambiente.
        arquivoCom(pasta, "MEDSHARE_PORTA=8081\n");
        var ambiente = new StandardEnvironment();
        ambiente.getPropertySources().addFirst(
                new org.springframework.core.env.MapPropertySource(
                        "ambiente de verdade", java.util.Map.of("MEDSHARE_PORTA", "9090")));

        new CarregadorDoEnv(pasta).postProcessEnvironment(ambiente, null);

        assertThat(ambiente.getProperty("MEDSHARE_PORTA")).isEqualTo("9090");
    }

    @Test
    @DisplayName("o Spring realmente enxerga o carregador no spring.factories")
    void registradoNoSpringFactories() throws Exception {
        // Este teste existe porque o erro mais provavel aqui e silencioso: se
        // o META-INF/spring.factories tiver um nome de classe errado, ou nao
        // for parar no jar, a aplicacao sobe normalmente e o .env simplesmente
        // nao faz efeito. Ninguem descobre ate a chave "nao funcionar".
        var propriedades = new Properties();
        try (InputStream entrada = CarregadorDoEnv.class.getClassLoader()
                .getResourceAsStream("META-INF/spring.factories")) {

            assertThat(entrada)
                    .withFailMessage("META-INF/spring.factories nao esta no classpath")
                    .isNotNull();
            propriedades.load(entrada);
        }

        String registrados = propriedades.getProperty(EnvironmentPostProcessor.class.getName());

        assertThat(registrados)
                .withFailMessage("nenhum EnvironmentPostProcessor registrado no spring.factories")
                .isNotNull()
                .contains(CarregadorDoEnv.class.getName());

        // O nome esta escrito la; falta garantir que ele corresponde a uma
        // classe que existe de verdade e que o Spring consegue instanciar.
        assertThat(Class.forName(CarregadorDoEnv.class.getName()).getDeclaredConstructor())
                .isNotNull();
    }

    @Test
    @DisplayName("sem .env nenhum, nada e adicionado")
    void semArquivo(@TempDir Path pasta) {
        var ambiente = new StandardEnvironment();
        int antes = ambiente.getPropertySources().size();

        new CarregadorDoEnv(pasta).postProcessEnvironment(ambiente, null);

        assertThat(ambiente.getPropertySources().size()).isEqualTo(antes);
    }
}
