package br.com.medshare.integracao;

import java.time.Duration;

/**
 * Guarda as fotos (embalagem e receita) no Cloudflare R2.
 *
 * O aplicativo nunca manda a imagem pela nossa API: ele pede uma URL assinada,
 * envia direto para o R2 e devolve so o endereco. Isso poupa banda do servidor
 * e mantem a foto fora dos nossos logs.
 *
 * Toda URL de leitura tem prazo curto porque a foto da receita e dado sensivel
 * de saude — um link permanente seria um vazamento esperando acontecer.
 */
public interface ArmazenamentoDeFotos {

    /** URL temporaria para o aplicativo enviar o arquivo. */
    UrlAssinada urlParaEnvio(String caminho, Duration validade);

    /** URL temporaria para exibir um arquivo ja enviado. */
    UrlAssinada urlParaLeitura(String caminho, Duration validade);

    record UrlAssinada(String url, Duration validade) { }
}
