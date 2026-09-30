package br.com.medshare.seguranca;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O CORS ja custou uma tarde.
 *
 * A lista de origens era literal, com "http://localhost:5173" e mais nada.
 * Quem abria o painel por 127.0.0.1, ou testava o aplicativo no celular pelo
 * IP da rede, recebia "Invalid CORS request" na tela de login — e ia conferir
 * a senha, que estava certa. O erro aparecia longe da causa.
 *
 * Estes testes existem para o proximo endereco que alguem precisar usar nao
 * custar a mesma tarde.
 */
@DisplayName("Origens que podem chamar a API pelo navegador")
class OrigensPermitidasTest {

    private static final List<String> PADRAO = List.of(
            "http://localhost:*", "http://127.0.0.1:*",
            "http://192.168.*:*", "http://10.*:*", "http://172.1*:*");

    private CorsConfiguration configuracaoCom(List<String> origens) {
        var fonte = (UrlBasedCorsConfigurationSource)
                new ConfiguracaoDeSeguranca(null).origensPermitidas(origens);
        return fonte.getCorsConfigurations().get("/api/**");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost:5173",   // painel em desenvolvimento
            "http://localhost:3000",   // outra porta, sem precisar mexer em nada
            "http://127.0.0.1:5173",   // o mesmo computador, escrito de outro jeito
            "http://192.168.0.15:8080" // o celular alcancando o PC pela rede de casa
    })
    @DisplayName("o padrao cobre a maquina de quem desenvolve e a rede local")
    void padraoCobreDesenvolvimento(String origem) {
        assertThat(configuracaoCom(PADRAO).checkOrigin(origem))
                .withFailMessage("%s deveria ser aceita, mas o navegador veria 'Invalid CORS request'", origem)
                .isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://site-qualquer.com", "http://medshare.com.br.evil.net"})
    @DisplayName("endereco de fora nao entra pelo padrao")
    void padraoNaoAbreParaQualquerUm(String origem) {
        assertThat(configuracaoCom(PADRAO).checkOrigin(origem)).isNull();
    }

    @Test
    @DisplayName("em producao a lista configurada substitui o padrao inteiro")
    void producaoSubstituiOPadrao() {
        var producao = configuracaoCom(List.of("https://medshare.com.br"));

        assertThat(producao.checkOrigin("https://medshare.com.br")).isNotNull();
        // O ponto do teste: os padroes de desenvolvimento somem junto.
        assertThat(producao.checkOrigin("http://localhost:5173")).isNull();
        assertThat(producao.checkOrigin("http://192.168.0.15:8080")).isNull();
    }

    @Test
    @DisplayName("o preflight OPTIONS e os metodos que a API usa estao liberados")
    void metodos() {
        var c = configuracaoCom(PADRAO);
        assertThat(c.getAllowedMethods())
                .contains("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    }
}
