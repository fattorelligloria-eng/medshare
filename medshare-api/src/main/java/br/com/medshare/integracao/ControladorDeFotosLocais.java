package br.com.medshare.integracao;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Serve as fotos por link assinado e com prazo (ver RepositorioDeFotos).
 *
 * O endereco e publico porque o link e aberto direto por uma tag de imagem,
 * que nao envia o token de login. Quem protege e a assinatura: sem ela, com
 * ela adulterada ou vencida, a resposta e 404 — nem confirma que a foto existe.
 */
@RestController
public class ControladorDeFotosLocais {

    private final RepositorioDeFotos fotos;

    public ControladorDeFotosLocais(RepositorioDeFotos fotos) {
        this.fotos = fotos;
    }

    @GetMapping("/fotos-locais/{nome}")
    public ResponseEntity<Resource> abrir(@PathVariable String nome,
                                          @RequestParam(defaultValue = "0") long expira,
                                          @RequestParam(required = false) String assinatura) {
        return fotos.arquivoDoLink(nome, expira, assinatura)
                .<ResponseEntity<Resource>>map(arquivo -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(fotos.tipoDoNome(nome)))
                        // Dado de saude: o navegador nao guarda em cache compartilhado.
                        .cacheControl(CacheControl.noStore())
                        .header("X-Content-Type-Options", "nosniff")
                        .body(new FileSystemResource(arquivo)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}
