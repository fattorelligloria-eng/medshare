package br.com.medshare.integracao;

import br.com.medshare.comum.RegraDeNegocioViolada;
import br.com.medshare.seguranca.UsuarioLogado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Recebe a foto da embalagem e da receita enviadas pelo aplicativo.
 *
 * O arquivo e gravado com um nome sorteado, nunca com o nome que veio do
 * aparelho: nome de arquivo e conteudo controlado por quem envia, e aceita-lo
 * abriria caminho para gravar fora do diretorio previsto.
 *
 * Em producao com Cloudflare R2 configurado, o aplicativo envia direto para o
 * R2 com URL assinada e este endpoint deixa de ser usado. Ele existe para que o
 * sistema funcione inteiro sem nenhuma credencial externa.
 */
@RestController
@RequestMapping("/api/fotos")
public class ControladorDeFotos {

    private static final Logger log = LoggerFactory.getLogger(ControladorDeFotos.class);

    private static final long TAMANHO_MAXIMO = 8L * 1024 * 1024;   // 8 MB
    private static final Map<String, String> EXTENSAO_POR_TIPO = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    private final Path diretorio;
    private final String enderecoPublico;
    private final UsuarioLogado usuarioLogado;

    public ControladorDeFotos(@Value("${medshare.fotos.diretorio}") String diretorio,
                              @Value("${medshare.fotos.endereco-publico}") String enderecoPublico,
                              UsuarioLogado usuarioLogado) throws IOException {
        this.diretorio = Path.of(diretorio).toAbsolutePath().normalize();
        this.enderecoPublico = enderecoPublico.replaceAll("/+$", "");
        this.usuarioLogado = usuarioLogado;
        Files.createDirectories(this.diretorio);
        log.info("Fotos gravadas em {} e servidas em {}", this.diretorio, this.enderecoPublico);
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<Map<String, String>> enviar(@RequestPart("arquivo") MultipartFile arquivo)
            throws IOException {

        usuarioLogado.obrigatorio();   // so quem esta autenticado envia foto
        String extensao = validar(arquivo);

        String nome = UUID.randomUUID() + extensao;
        Path destino = diretorio.resolve(nome);
        arquivo.transferTo(destino);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("url", enderecoPublico + "/" + nome, "nome", nome));
    }

    private String validar(MultipartFile arquivo) {
        if (arquivo.isEmpty()) {
            throw new RegraDeNegocioViolada("FOTO", "O arquivo enviado está vazio");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw new RegraDeNegocioViolada("FOTO",
                    "A foto tem %d MB. O limite é 8 MB — tire a foto em resolução menor."
                            .formatted(arquivo.getSize() / (1024 * 1024)));
        }
        String tipo = arquivo.getContentType();
        String extensao = tipo == null ? null : EXTENSAO_POR_TIPO.get(tipo.toLowerCase());
        if (extensao == null) {
            throw new RegraDeNegocioViolada("FOTO",
                    "Envie uma imagem JPEG, PNG ou WebP. Recebemos: %s".formatted(tipo));
        }
        return extensao;
    }

    /** Usado pelo app para saber de antemao o que o servidor aceita. */
    @GetMapping("/limites")
    public Map<String, Object> limites() {
        return Map.of(
                "tamanhoMaximoEmBytes", TAMANHO_MAXIMO,
                "tiposAceitos", List.copyOf(EXTENSAO_POR_TIPO.keySet()));
    }
}
