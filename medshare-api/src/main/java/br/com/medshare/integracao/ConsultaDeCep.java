package br.com.medshare.integracao;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * ViaCEP - publica, gratuita e sem cadastro.
 *
 * Serve para o usuario nao digitar o endereco inteiro e, principalmente, para
 * conferirmos o municipio contra a lista da Grande Sao Paulo (RN09) antes de
 * deixar o cadastro seguir.
 */
@Component
public class ConsultaDeCep {

    private static final Logger log = LoggerFactory.getLogger(ConsultaDeCep.class);

    private final RestClient viaCep = RestClient.builder()
            .baseUrl("https://viacep.com.br/ws")
            .build();

    public Optional<EnderecoConsultado> porCep(String cep) {
        String apenasDigitos = cep.replaceAll("\\D", "");
        if (apenasDigitos.length() != 8) {
            return Optional.empty();
        }
        try {
            JsonNode resposta = viaCep.get()
                    .uri("/{cep}/json/", apenasDigitos)
                    .retrieve()
                    .body(JsonNode.class);

            if (resposta == null || resposta.path("erro").asBoolean(false)) {
                return Optional.empty();
            }
            return Optional.of(new EnderecoConsultado(
                    resposta.path("cep").asText(),
                    resposta.path("logradouro").asText(),
                    resposta.path("bairro").asText(),
                    resposta.path("localidade").asText(),
                    resposta.path("uf").asText()));
        } catch (Exception e) {
            log.warn("ViaCEP indisponivel para o CEP {}: {}", apenasDigitos, e.getMessage());
            return Optional.empty();
        }
    }
}
