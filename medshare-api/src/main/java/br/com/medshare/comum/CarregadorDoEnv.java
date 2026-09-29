package br.com.medshare.comum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lê o arquivo {@code .env} da pasta do projeto antes da aplicação subir.
 *
 * <p>O Spring Boot não lê {@code .env} sozinho. A documentação mandava copiar
 * {@code .env.exemplo} para {@code .env} e preencher, mas nada carregava esse
 * arquivo — quem seguia a instrução ficava sem entender por que a chave não
 * fazia efeito. Esta classe fecha esse buraco sem trazer biblioteca nova.
 *
 * <p>Uma decisão importante: o {@code .env} entra com prioridade <b>baixa</b>.
 * Variável de ambiente de verdade e parâmetro de linha de comando continuam
 * ganhando dele. Em produção, onde as credenciais vêm do ambiente do servidor,
 * um {@code .env} esquecido na pasta não sobrescreve nada.
 *
 * <p>Registrada em {@code META-INF/spring.factories}, porque ela roda antes do
 * contexto existir — não dá para ser um {@code @Component}.
 */
public class CarregadorDoEnv implements EnvironmentPostProcessor {

    /**
     * Onde procurar, em ordem: a pasta atual, a pasta acima (rodando o módulo
     * direto pelo IntelliJ) e o módulo da API (rodando da raiz do repositório).
     */
    private static final List<String> CAMINHOS = List.of(
            ".env",
            "../.env",
            "medshare-api/.env");

    private final Path base;

    public CarregadorDoEnv() {
        this(Path.of("."));
    }

    /** O construtor com pasta existe para o teste; a aplicação usa o vazio. */
    CarregadorDoEnv(Path base) {
        this.base = base;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment ambiente, SpringApplication app) {
        for (String relativo : CAMINHOS) {
            Path caminho = base.resolve(relativo);
            if (!Files.isRegularFile(caminho)) continue;

            Map<String, Object> valores = ler(caminho);
            if (valores.isEmpty()) return;

            // addLast: quem já estava no ambiente continua na frente.
            ambiente.getPropertySources().addLast(new MapPropertySource("arquivo .env", valores));
            return;
        }
    }

    /** Visível ao pacote de propósito: é o que o teste exercita. */
    Map<String, Object> ler(Path caminho) {
        Map<String, Object> valores = new HashMap<>();
        try {
            for (String linha : Files.readAllLines(caminho, StandardCharsets.UTF_8)) {
                String limpa = linha.strip();
                if (limpa.isEmpty() || limpa.startsWith("#")) continue;

                int igual = limpa.indexOf('=');
                if (igual <= 0) continue;

                String nome = limpa.substring(0, igual).strip();
                String valor = desembrulhar(limpa.substring(igual + 1).strip());

                // Linha sem valor (`MEDSHARE_GEMINI_CHAVE=`) fica de fora: o
                // padrão do application.yml é melhor do que string vazia.
                if (!valor.isEmpty()) valores.put(nome, valor);
            }
        } catch (IOException e) {
            // Não derruba a aplicação por causa do .env: ela roda inteira sem
            // ele. Só avisa, porque o silêncio aqui é o que confunde.
            System.err.println("Não consegui ler " + caminho.toAbsolutePath() + ": " + e.getMessage());
        }
        return valores;
    }

    /** Tira as aspas de {@code SENHA="com espaço"}, se houver. */
    private String desembrulhar(String valor) {
        if (valor.length() >= 2
                && (valor.startsWith("\"") && valor.endsWith("\"")
                 || valor.startsWith("'") && valor.endsWith("'"))) {
            return valor.substring(1, valor.length() - 1);
        }
        return valor;
    }
}
