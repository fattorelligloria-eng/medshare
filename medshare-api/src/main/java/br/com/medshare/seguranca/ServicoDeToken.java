package br.com.medshare.seguranca;

import br.com.medshare.comum.PropriedadesDoMedShare;
import br.com.medshare.usuario.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Emissao e leitura dos tokens.
 *
 * O segredo vem do ambiente (MEDSHARE_JWT_SEGREDO) e nunca do codigo. Sao dois
 * tokens: o de acesso, curto, que vai em toda requisicao, e o de renovacao,
 * longo, guardado so no aparelho. Se o de acesso vazar, ele expira em duas horas.
 */
@Service
public class ServicoDeToken {

    private static final Logger log = LoggerFactory.getLogger(ServicoDeToken.class);
    private static final String PREFIXO_DO_SEGREDO_DE_DESENVOLVIMENTO = "desenvolvimento-local-apenas";
    private static final String EMISSOR = "medshare-api";
    private static final String CAMPO_PAPEIS = "papeis";
    private static final String CAMPO_TIPO = "tipo";
    private static final String TIPO_ACESSO = "acesso";
    private static final String TIPO_RENOVACAO = "renovacao";

    private final SecretKey chave;
    private final Duration validadeDoAcesso;
    private final Duration validadeDaRenovacao;

    public ServicoDeToken(PropriedadesDoMedShare propriedades) {
        if (propriedades.jwt().segredo().startsWith(PREFIXO_DO_SEGREDO_DE_DESENVOLVIMENTO)) {
            // Com a chave publica do repositorio, qualquer um assina um token de
            // ADMIN. Aceitavel na maquina de quem desenvolve; nunca num servidor.
            log.warn("MEDSHARE_JWT_SEGREDO nao foi definido: usando a chave de desenvolvimento. "
                    + "Defina uma chave propria antes de publicar a API.");
        }
        this.chave = Keys.hmacShaKeyFor(
                propriedades.jwt().segredo().getBytes(StandardCharsets.UTF_8));
        this.validadeDoAcesso = Duration.ofMinutes(propriedades.jwt().minutosDeValidade());
        this.validadeDaRenovacao = Duration.ofDays(propriedades.jwt().diasDeValidadeDoRefresh());
    }

    public String emitirTokenDeAcesso(Usuario usuario) {
        return construir(usuario, TIPO_ACESSO, validadeDoAcesso);
    }

    public String emitirTokenDeRenovacao(Usuario usuario) {
        return construir(usuario, TIPO_RENOVACAO, validadeDaRenovacao);
    }

    public long segundosDeValidadeDoAcesso() {
        return validadeDoAcesso.toSeconds();
    }

    /** Devolve o e-mail do dono do token, ou vazio se o token nao presta. */
    public Optional<String> emailDoTokenDeAcesso(String token) {
        return ler(token).filter(c -> TIPO_ACESSO.equals(c.get(CAMPO_TIPO)))
                .map(Claims::getSubject);
    }

    public Optional<String> emailDoTokenDeRenovacao(String token) {
        return ler(token).filter(c -> TIPO_RENOVACAO.equals(c.get(CAMPO_TIPO)))
                .map(Claims::getSubject);
    }

    private String construir(Usuario usuario, String tipo, Duration validade) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .issuer(EMISSOR)
                .subject(usuario.getEmail())
                .claim(CAMPO_TIPO, tipo)
                .claim(CAMPO_PAPEIS, papeisDe(usuario))
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(validade)))
                .signWith(chave)
                .compact();
    }

    private List<String> papeisDe(Usuario usuario) {
        return usuario.getPapeis().stream().map(Enum::name).toList();
    }

    private Optional<Claims> ler(String token) {
        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(chave)
                    .requireIssuer(EMISSOR)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            // Token expirado, adulterado ou mal formado dao no mesmo para quem chama.
            return Optional.empty();
        }
    }
}
