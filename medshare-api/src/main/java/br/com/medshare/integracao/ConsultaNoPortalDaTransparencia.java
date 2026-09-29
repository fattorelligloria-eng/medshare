package br.com.medshare.integracao;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * RN08 pela fonte oficial: a API do Portal da Transparência (CGU).
 *
 * A consulta pergunta se aquele NIS aparece como beneficiário do Bolsa Família
 * em algum dos últimos meses. A API é gratuita — basta cadastrar um e-mail em
 * portaldatransparencia.gov.br/api-de-dados e usar o token recebido no
 * cabeçalho chave-api-dados.
 *
 * Três cuidados que fazem diferença aqui:
 *
 *  1. Olha alguns meses para trás, não só o mês corrente. A folha de pagamento
 *     do mês atual pode ainda não ter sido publicada, e quem recebe seria
 *     tratado como se não recebesse.
 *  2. Falha de rede NUNCA vira recusa. Se a API estiver fora do ar, o caso vai
 *     para análise humana — negar benefício por causa de um timeout seria
 *     punir a pessoa por um problema nosso.
 *  3. O NIS vai na consulta, mas nada além dele: nenhum nome, nenhum CPF,
 *     nenhum dado do pedido.
 */
@Component
@ConditionalOnExpression("!'${medshare.portal-transparencia.chave:}'.isEmpty()")
public class ConsultaNoPortalDaTransparencia implements ConsultaDeCadUnico {

    private static final Logger log = LoggerFactory.getLogger(ConsultaNoPortalDaTransparencia.class);
    private static final DateTimeFormatter ANO_E_MES = DateTimeFormatter.ofPattern("yyyyMM");
    private static final int MESES_CONSULTADOS = 4;

    private final RestClient portal;

    public ConsultaNoPortalDaTransparencia(
            @Value("${medshare.portal-transparencia.chave}") String chave) {
        this.portal = RestClient.builder()
                .baseUrl("https://api.portaldatransparencia.gov.br/api-de-dados")
                .defaultHeader("chave-api-dados", chave)
                .defaultHeader("Accept", "application/json")
                .build();
        log.info("Consulta do CadUnico ligada no Portal da Transparencia");
    }

    @Override
    public ResultadoDaConsulta consultar(String nis) {
        if (!ValidadorDeNis.temFormatoValido(nis)) {
            return ResultadoDaConsulta.formatoInvalido(
                    "NIS inválido: o dígito verificador não confere");
        }

        LocalDate mes = LocalDate.now();
        for (int i = 0; i < MESES_CONSULTADOS; i++) {
            Boolean encontrado = apareceNoMes(nis, mes.format(ANO_E_MES));
            if (encontrado == null) {
                // A API não respondeu. Não é culpa de quem está pedindo.
                return ResultadoDaConsulta.naoEncontrado(
                        "Não conseguimos falar com o Portal da Transparência agora; "
                                + "o caso foi para conferência da nossa equipe");
            }
            if (encontrado) {
                return ResultadoDaConsulta.confirmado(
                        "NIS confirmado como beneficiário no Portal da Transparência");
            }
            mes = mes.minusMonths(1);
        }

        return ResultadoDaConsulta.naoEncontrado(
                "NIS não apareceu como beneficiário de programa social nos últimos "
                        + MESES_CONSULTADOS + " meses");
    }

    @Override
    public String fonte() {
        return "PORTAL_TRANSPARENCIA";
    }

    /** true, false, ou null quando a consulta não pôde ser feita. */
    private Boolean apareceNoMes(String nis, String anoMes) {
        try {
            JsonNode resposta = portal.get()
                    .uri(uri -> uri.path("/novo-bolsa-familia-sacado-por-nis")
                            .queryParam("nis", nis)
                            .queryParam("anoMesCompetencia", anoMes)
                            .queryParam("pagina", 1)
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            return resposta != null && resposta.isArray() && !resposta.isEmpty();
        } catch (Exception e) {
            log.warn("Portal da Transparencia indisponivel para a competencia {}: {}",
                    anoMes, e.getMessage());
            return null;
        }
    }
}
