package br.com.medshare.integracao;

import br.com.medshare.prevalidacao.Certeza;
import br.com.medshare.prevalidacao.ClasseEmbalagem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Map;

/**
 * Leitura da embalagem pela API do Gemini.
 *
 * Como a resposta de um modelo de linguagem e texto livre por natureza, dois
 * cuidados tornam isso utilizavel em producao:
 *
 *  1. responseMimeType = application/json com responseSchema. O proprio Gemini
 *     garante a forma da resposta, entao nao precisamos ficar caçando JSON no
 *     meio de um texto.
 *  2. temperatura 0.1. A tarefa aqui e conferir uma caixa, nao escrever — a
 *     mesma foto deve produzir a mesma leitura.
 *
 * E o terceiro cuidado esta fora desta classe: qualquer erro, timeout ou
 * resposta estranha vira "indisponivel", o que empurra a doacao para a analise
 * humana. Falha de integracao nunca aprova nada sozinha (RN10).
 */
@Component
@ConditionalOnExpression("!'${medshare.gemini.chave:}'.isEmpty()")
public class AvaliadorGemini implements AvaliadorDeEmbalagem {

    private static final Logger log = LoggerFactory.getLogger(AvaliadorGemini.class);

    private static final String INSTRUCOES = """
            Voce e um farmaceutico conferindo uma caixa de medicamento doada.

            Olhe a foto e responda sobre o ESTADO DA EMBALAGEM:
            - as abas da caixa estao coladas de fabrica?
            - o selo ou lacre esta intacto?
            - da para ver o blister ou o frasco pela abertura?
            - a caixa esta amassada, rasgada ou molhada?
            - isso e mesmo uma caixa de medicamento?

            Classifique em uma destas quatro opcoes:
            - lacrada: abas coladas, selo intacto, conteudo nao aparece
            - violada: caixa aberta, abas descoladas ou selo rompido
            - danificada: fechada, mas amassada, rasgada ou molhada
            - invalida: nao e caixa de medicamento, ou a foto nao permite avaliar

            Leia tambem, se estiverem legiveis: codigo de barras (EAN),
            numero do lote e data de validade.

            REGRA DE OURO: se voce nao tiver certeza, NAO responda "lacrada".
            Use certeza "media" ou "baixa". Um caso em duvida vai para conferencia
            humana, e isso e o esperado. Errar dizendo que esta lacrada quando nao
            esta faz um medicamento adulterado chegar a um paciente.
            """;

    private static final Map<String, Object> ESQUEMA_DA_RESPOSTA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "classe", Map.of("type", "STRING",
                            "enum", java.util.List.of("lacrada", "violada", "danificada", "invalida")),
                    "certeza", Map.of("type", "STRING",
                            "enum", java.util.List.of("alta", "media", "baixa")),
                    "motivo", Map.of("type", "STRING"),
                    "ean", Map.of("type", "STRING"),
                    "lote", Map.of("type", "STRING"),
                    "validade", Map.of("type", "STRING",
                            "description", "data no formato AAAA-MM-DD, ou vazio se ilegível")),
            "required", java.util.List.of("classe", "certeza", "motivo"));

    private final RestClient gemini;
    private final RepositorioDeFotos fotos;
    private final ObjectMapper json;
    private final String modelo;

    public AvaliadorGemini(@Value("${medshare.gemini.chave}") String chave,
                           @Value("${medshare.gemini.modelo:gemini-3.5-flash}") String modelo,
                           RepositorioDeFotos fotos, ObjectMapper json) {
        this.modelo = modelo;
        this.fotos = fotos;
        this.json = json;
        // A chave vai no cabecalho, e nao na URL: URL aparece em log e em
        // mensagem de erro, cabecalho nao.
        this.gemini = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .defaultHeader("x-goog-api-key", chave)
                .build();
        log.info("Avaliador Gemini ativo, modelo {}", modelo);
    }

    @Override
    public LeituraDaEmbalagem avaliar(String fotoUrl) {
        try {
            byte[] imagem = lerDoDisco(fotoUrl);
            JsonNode resposta = chamarGemini(imagem, fotos.tipoDa(fotoUrl));
            return interpretar(resposta);
        } catch (Exception e) {
            log.warn("Falha ao avaliar a foto {}: {}", fotoUrl, e.getMessage());
            return LeituraDaEmbalagem.indisponivel(
                    "Não foi possível analisar a foto automaticamente");
        }
    }

    @Override
    public String nome() {
        return modelo;
    }

    /**
     * So le fotos gravadas pelo proprio /api/fotos, direto do disco. Nunca
     * baixa uma URL vinda do cliente: isso deixaria alguem usar o servidor
     * para acessar enderecos internos da rede.
     */
    private byte[] lerDoDisco(String fotoUrl) throws java.io.IOException {
        java.nio.file.Path arquivo = fotos.arquivoDa(fotoUrl)
                .orElseThrow(() -> new IllegalStateException("foto fora do armazenamento do MedShare"));
        byte[] conteudo = java.nio.file.Files.readAllBytes(arquivo);
        if (conteudo.length == 0) {
            throw new IllegalStateException("foto vazia");
        }
        return conteudo;
    }

    private JsonNode chamarGemini(byte[] imagem, String tipo) {
        Map<String, Object> corpo = Map.of(
                "system_instruction", Map.of("parts", java.util.List.of(Map.of("text", INSTRUCOES))),
                "contents", java.util.List.of(Map.of("parts", java.util.List.of(
                        Map.of("inline_data", Map.of(
                                "mime_type", tipo,
                                "data", Base64.getEncoder().encodeToString(imagem)))))),
                "generationConfig", Map.of(
                        "temperature", 0.1,
                        "responseMimeType", "application/json",
                        "responseSchema", ESQUEMA_DA_RESPOSTA));

        return gemini.post()
                .uri("/models/{modelo}:generateContent", modelo)
                .body(corpo)
                .retrieve()
                .body(JsonNode.class);
    }

    private LeituraDaEmbalagem interpretar(JsonNode resposta) throws Exception {
        String texto = resposta.path("candidates").path(0)
                .path("content").path("parts").path(0).path("text").asText(null);
        if (texto == null) {
            return LeituraDaEmbalagem.indisponivel("O serviço de visão não devolveu uma leitura");
        }

        JsonNode leitura = json.readTree(texto);
        return new LeituraDaEmbalagem(
                textoOuNulo(leitura, "ean"),
                textoOuNulo(leitura, "lote"),
                dataOuNulo(textoOuNulo(leitura, "validade")),
                classeDe(leitura.path("classe").asText("invalida")),
                certezaDe(leitura.path("certeza").asText("baixa")),
                leitura.path("motivo").asText(""));
    }

    private String textoOuNulo(JsonNode no, String campo) {
        String valor = no.path(campo).asText("");
        return valor.isBlank() ? null : valor.trim();
    }

    private LocalDate dataOuNulo(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            return LocalDate.parse(texto);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private ClasseEmbalagem classeDe(String valor) {
        return switch (valor.toLowerCase()) {
            case "lacrada" -> ClasseEmbalagem.LACRADA;
            case "violada" -> ClasseEmbalagem.VIOLADA;
            case "danificada" -> ClasseEmbalagem.DANIFICADA;
            default -> ClasseEmbalagem.INVALIDA;
        };
    }

    private Certeza certezaDe(String valor) {
        return switch (valor.toLowerCase()) {
            case "alta" -> Certeza.ALTA;
            case "media", "média" -> Certeza.MEDIA;
            default -> Certeza.BAIXA;
        };
    }
}
