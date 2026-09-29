package br.com.medshare.integracao;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Converte endereco em latitude e longitude pelo Nominatim (OpenStreetMap):
 * gratuito e sem cartao, ao contrario do Google Maps.
 *
 * E o que permite "farmacia mais proxima" (UC02) e "beneficiario mais perto
 * da caixa" (UC05). Sem coordenada o sistema continua funcionando — so perde
 * o criterio de distancia —, entao qualquer falha aqui devolve vazio.
 *
 * Politica de uso do Nominatim: identificar a aplicacao no User-Agent e no
 * maximo uma requisicao por segundo. O volume do MedShare (um cadastro por
 * vez) fica bem abaixo disso.
 */
@Component
public class Geocodificador {

    private static final Logger log = LoggerFactory.getLogger(Geocodificador.class);

    public record Coordenadas(double latitude, double longitude) { }

    private final RestClient nominatim;
    private final boolean habilitado;

    public Geocodificador(@Value("${medshare.geocodificacao.habilitada:true}") boolean habilitado,
                          @Value("${medshare.geocodificacao.contato:https://github.com/fattorelligloria-eng/medshare}")
                          String contato) {
        this.habilitado = habilitado;
        this.nominatim = RestClient.builder()
                .baseUrl("https://nominatim.openstreetmap.org")
                .requestFactory(Tempos.curtos())
                .defaultHeader("User-Agent", "MedShare/1.0 (" + contato + ")")
                .defaultHeader("Accept-Language", "pt-BR")
                .build();
    }

    /** Tenta o endereco completo; se nao achar, o CEP com a cidade (centro aproximado). */
    public Optional<Coordenadas> localizar(String logradouro, String numero, String municipio, String cep) {
        if (!habilitado) {
            return Optional.empty();
        }
        return buscar("%s %s".formatted(numero == null ? "" : numero, logradouro).strip(), municipio, null)
                .or(() -> buscar(null, municipio, formatarCep(cep)));
    }

    private Optional<Coordenadas> buscar(String rua, String cidade, String cep) {
        try {
            JsonNode resposta = nominatim.get()
                    .uri(uri -> {
                        uri.path("/search").queryParam("format", "json").queryParam("limit", 1)
                                .queryParam("countrycodes", "br").queryParam("state", "São Paulo");
                        if (rua != null && !rua.isBlank()) uri.queryParam("street", rua);
                        if (cidade != null && !cidade.isBlank()) uri.queryParam("city", cidade);
                        if (cep != null) uri.queryParam("postalcode", cep);
                        return uri.build();
                    })
                    .retrieve()
                    .body(JsonNode.class);
            if (resposta == null || !resposta.isArray() || resposta.isEmpty()) {
                return Optional.empty();
            }
            JsonNode lugar = resposta.get(0);
            return Optional.of(new Coordenadas(
                    Double.parseDouble(lugar.path("lat").asText()),
                    Double.parseDouble(lugar.path("lon").asText())));
        } catch (Exception e) {
            log.warn("Nominatim indisponível para '{}, {}': {}", rua, cidade, e.getMessage());
            return Optional.empty();
        }
    }

    private static String formatarCep(String cep) {
        String digitos = cep == null ? "" : cep.replaceAll("\\D", "");
        return digitos.length() == 8 ? digitos.substring(0, 5) + "-" + digitos.substring(5) : null;
    }
}
