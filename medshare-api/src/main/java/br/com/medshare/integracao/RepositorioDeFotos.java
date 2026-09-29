package br.com.medshare.integracao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
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

    /** Quanto tempo um link de foto vale depois de entregue pela API. */
    public static final Duration VALIDADE_DO_LINK = Duration.ofMinutes(15);

    private final Path diretorio;
    private final String enderecoPublico;
    private final SecretKeySpec chaveDeAssinatura;

    public RepositorioDeFotos(@Value("${medshare.fotos.diretorio}") String diretorio,
                              @Value("${medshare.fotos.endereco-publico}") String enderecoPublico,
                              @Value("${medshare.jwt.segredo}") String segredo) {
        this.diretorio = Path.of(diretorio).toAbsolutePath().normalize();
        this.enderecoPublico = enderecoPublico.replaceAll("/+$", "");
        // Chave propria para fotos, derivada do segredo do servidor: quem nao
        // tem o segredo nao consegue fabricar um link valido.
        this.chaveDeAssinatura = new SecretKeySpec(
                hmac(("fotos:" + segredo).getBytes(StandardCharsets.UTF_8), "medshare".getBytes(StandardCharsets.UTF_8)),
                "HmacSHA256");
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

    // --- links assinados --------------------------------------------------------
    //
    // A foto da receita e dado de saude: um link permanente, que passa de mao
    // em mao, seria um vazamento esperando acontecer. Por isso a URL gravada no
    // banco nunca e entregue como esta: a API devolve um link com prazo e
    // assinatura (HMAC), e o endpoint de fotos so serve o arquivo com os dois
    // conferindo. E o mesmo modelo das URLs assinadas do S3/R2.

    /** Link com prazo para exibir uma foto nossa; qualquer outra URL volta como veio. */
    public String urlAssinada(String urlCanonica) {
        if (urlCanonica == null) {
            return null;
        }
        return nomeDa(urlCanonica).map(nome -> {
            long expira = Instant.now().plus(VALIDADE_DO_LINK).getEpochSecond();
            return "%s?expira=%d&assinatura=%s".formatted(urlCanonica, expira, assinar(nome.group(0), expira));
        }).orElse(urlCanonica);
    }

    /** O arquivo de um link assinado, se o nome, o prazo e a assinatura conferirem. */
    public Optional<Path> arquivoDoLink(String nome, long expira, String assinatura) {
        if (!NOME_VALIDO.matcher(nome).matches()
                || Instant.now().getEpochSecond() > expira
                || assinatura == null
                || !MessageDigest.isEqual(assinar(nome, expira).getBytes(StandardCharsets.US_ASCII),
                                          assinatura.getBytes(StandardCharsets.US_ASCII))) {
            return Optional.empty();
        }
        Path caminho = diretorio.resolve(nome).normalize();
        return caminho.startsWith(diretorio) && Files.isRegularFile(caminho)
                ? Optional.of(caminho) : Optional.empty();
    }

    public String tipoDoNome(String nome) {
        Matcher m = NOME_VALIDO.matcher(nome);
        return m.matches() ? TIPO_POR_EXTENSAO.get(m.group(2)) : "application/octet-stream";
    }

    private String assinar(String nome, long expira) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(chaveDeAssinatura);
            byte[] assinatura = mac.doFinal((nome + ":" + expira).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(assinatura);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 indisponível", e);
        }
    }

    private static byte[] hmac(byte[] chave, byte[] dados) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(chave, "HmacSHA256"));
            return mac.doFinal(dados);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 indisponível", e);
        }
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
