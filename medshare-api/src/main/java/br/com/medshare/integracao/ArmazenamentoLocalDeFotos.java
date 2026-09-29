package br.com.medshare.integracao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Implementacao de desenvolvimento: devolve uma URL local, sem assinatura.
 * Ativa quando nao ha credenciais do R2 configuradas, para o projeto rodar
 * completo na maquina de quem esta desenvolvendo.
 */
@Component
@ConditionalOnExpression("'${medshare.r2.endpoint:}'.isEmpty()")
public class ArmazenamentoLocalDeFotos implements ArmazenamentoDeFotos {

    private static final Logger log = LoggerFactory.getLogger(ArmazenamentoLocalDeFotos.class);

    public ArmazenamentoLocalDeFotos() {
        log.warn("Sem credenciais do Cloudflare R2: as fotos usam URLs locais de desenvolvimento.");
    }

    @Override
    public UrlAssinada urlParaEnvio(String caminho, Duration validade) {
        return new UrlAssinada("http://localhost:8080/fotos-locais/" + caminho, validade);
    }

    @Override
    public UrlAssinada urlParaLeitura(String caminho, Duration validade) {
        return new UrlAssinada("http://localhost:8080/fotos-locais/" + caminho, validade);
    }
}
