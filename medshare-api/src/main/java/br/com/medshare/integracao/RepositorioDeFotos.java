package br.com.medshare.integracao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Onde as fotos ficam e como reconhecer uma foto nossa.
 *
 * A URL da foto chega do cliente junto com a doacao e a receita. Aceitar
 * qualquer endereco deixaria alguem apontar para uma imagem de outra caixa, ou
 * fazer o servidor buscar enderecos da rede interna quando o Gemini fosse ler a
 * foto. Por isso so vale a URL que o proprio /api/fotos devolveu: endereco
 * publico configurado + nome sorteado (UUID) + extensao de imagem. E a leitura
 * para a IA acontece direto do disco, nunca por HTTP.
 */
@Component
public class RepositorioDeFotos {

    static final Map<String, String> EXTENSAO_POR_TIPO = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    private static final Map<String, String> TIPO_POR_EXTENSAO = Map.of(
            "jpg", "image/jpeg",
            "png", "image/png",
            "webp", "image/webp");

    private static final Pattern NOME_VALIDO = Pattern.compile(
            "([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})\\.(jpg|png|webp)");

    private final Path diretorio;
    private final String enderecoPublico;

    public RepositorioDeFotos(@Value("${medshare.fotos.diretorio}") String diretorio,
                              @Value("${medshare.fotos.endereco-publico}") String enderecoPublico) {
        this.diretorio = Path.of(diretorio).toAbsolutePath().normalize();
        this.enderecoPublico = enderecoPublico.replaceAll("/+$", "");
        try {
            Files.createDirectories(this.diretorio);
        } catch (IOException e) {
            throw new UncheckedIOException("Não consegui criar a pasta de fotos " + this.diretorio, e);
        }
    }

    public Path diretorio() {
        return diretorio;
    }

    public String enderecoPublico() {
        return enderecoPublico;
    }

    /** Nome novo e imprevisivel para uma foto do tipo informado. */
    public String novoNome(String extensao) {
        return UUID.randomUUID() + extensao;
    }

    public String urlDe(String nome) {
        return enderecoPublico + "/" + nome;
    }

    /** A URL foi emitida por este servidor e o arquivo existe em disco? */
    public boolean ehFotoNossa(String url) {
        return arquivoDa(url).isPresent();
    }

    /** Barra URLs que nao vieram do nosso envio de fotos. */
    public void exigirFotoNossa(String url) {
        if (!ehFotoNossa(url)) {
            throw new br.com.medshare.comum.RegraDeNegocioViolada("FOTO",
                    "Envie a foto pelo aplicativo antes de continuar");
        }
    }

    /** O arquivo em disco correspondente a uma URL nossa, ou vazio. */
    public Optional<Path> arquivoDa(String url) {
        return nomeDa(url)
                .map(nome -> diretorio.resolve(nome.group(0)).normalize())
                .filter(caminho -> caminho.startsWith(diretorio) && Files.isRegularFile(caminho));
    }

    /** O tipo MIME pela extensao do nome, que nos mesmos escolhemos no envio. */
    public String tipoDa(String url) {
        return nomeDa(url)
                .map(nome -> TIPO_POR_EXTENSAO.get(nome.group(2)))
                .orElse("image/jpeg");
    }

    private Optional<Matcher> nomeDa(String url) {
        String prefixo = enderecoPublico + "/";
        if (url == null || !url.startsWith(prefixo)) {
            return Optional.empty();
        }
        Matcher nome = NOME_VALIDO.matcher(url.substring(prefixo.length()));
        return nome.matches() ? Optional.of(nome) : Optional.empty();
    }
}
