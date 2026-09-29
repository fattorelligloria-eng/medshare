package br.com.medshare.integracao;

import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Limites de tempo das APIs publicas (ViaCEP, Nominatim). Sem eles, uma API
 * lenta prenderia a tela de cadastro; com eles, a falha vira "sem dado" e o
 * cadastro segue.
 */
final class Tempos {

    private Tempos() { }

    static JdkClientHttpRequestFactory curtos() {
        var fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build());
        fabrica.setReadTimeout(Duration.ofSeconds(6));
        return fabrica;
    }
}
