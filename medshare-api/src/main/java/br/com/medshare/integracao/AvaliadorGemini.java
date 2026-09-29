package br.com.medshare.integracao;

import br.com.medshare.prevalidacao.Certeza;
import br.com.medshare.prevalidacao.ClasseEmbalagem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Leitura da embalagem pela API do Gemini (plano gratuito do Google AI Studio).
 *
 * O trabalho aqui e de leitura, nao de decisao: o modelo diz o que conseguiu
 * ler (lote, validade, codigo de barras) e como a caixa parece estar. Quem
 * compara com o que o doador digitou e decide e a RegraDeDecisaoDaPreValidacao
 * (RN10), e a palavra final e sempre do farmaceutico no balcao (RN01).
 *
 * Tres cuidados tornam isso utilizavel:
 *
 *  1. responseMimeType = application/json com responseSchema: o proprio Gemini
 *     garante a forma da resposta, sem cacar JSON no meio de um texto.
 *  2. Temperatura baixa: a mesma foto deve produzir a mesma leitura.
 *  3. Qualquer erro, timeout ou limite do plano gratuito vira "indisponivel",
 *     o que manda a doacao para a analise humana. Falha de integracao nunca
 *     aprova nada sozinha.
 *
 * Privacidade: no plano gratuito o Google pode usar o conteudo enviado para
 * melhorar os produtos dele. Por isso so a foto da EMBALAGEM passa por aqui —
 * a foto da receita, que e dado de saude, nunca e enviada.
 */
@Component
@ConditionalOnExpression("!'${medshare.gemini.chave:}'.isEmpty()")
public class AvaliadorGemini implements AvaliadorDeEmbalagem {

    private static final Logger log = LoggerFactory.getLogger(AvaliadorGemini.class);

    static final String INSTRUCOES = """
            Voce e um farmaceutico conferindo uma caixa de medicamento doada.
            Sua tarefa e LER o que esta impresso e AVALIAR a embalagem. Nao invente
            nada: o que nao estiver legivel fica vazio.

            1. LOTE
               Procure o texto perto de "LOTE", "LOT", "L:" ou "L." — costuma estar
               gravado em relevo ou impresso a jato na aba ou na lateral da caixa.
               Copie exatamente os caracteres, sem o rotulo ("L: AB1234" -> "AB1234").
               Nao confunda com data de fabricacao ("FAB", "F:", "MFG") nem com o
               registro no Ministerio da Saude ("MS 1.0000.0000").

            2. VALIDADE
               Procure perto de "VAL", "VALIDADE", "V:", "VENC" ou "EXP". Nao confunda
               com a data de fabricacao. Responda no formato AAAA-MM-DD quando o dia
               estiver impresso, ou AAAA-MM quando so houver mes e ano (o mais comum:
               "VAL 03/2027" -> "2027-03"; "VAL MAR/27" -> "2027-03").

            3. CODIGO DE BARRAS
               Os 13 digitos impressos embaixo das barras (comecam com 789 nos
               produtos brasileiros). Vazio se nao estiver na foto.

            4. ESTADO DA EMBALAGEM — classifique em uma opcao:
               - lacrada: abas coladas de fabrica, selo/lacre intacto, conteudo nao aparece
               - violada: caixa aberta, abas descoladas, selo rompido ou colado com fita
               - danificada: fechada, mas amassada, rasgada ou molhada
               - invalida: nao e caixa de medicamento, ou a foto nao permite avaliar

            5. CERTEZA
               "alta" so se a foto estiver nitida e voce leu lote e validade sem duvida
               e o lacre esta claramente visivel e intacto. Qualquer duvida: "media" ou
               "baixa". Um caso em duvida vai para conferencia humana, e isso e o
               esperado. Dizer "lacrada" com certeza alta para uma caixa violada faz
               um medicamento adulterado chegar a um paciente.

            Em "motivo", explique em uma frase curta, em portugues, o que voce viu.
            """;

    private static final Map<String, Object> ESQUEMA_DA_RESPOSTA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "classe", Map.of("type", "STRING",
                            "enum", List.of("lacrada", "violada", "danificada", "invalida")),
                    "certeza", Map.of("type", "STRING",
                            "enum", List.of("alta", "media", "baixa")),
                    "motivo", Map.of("type", "STRING"),
                    "ean", Map.of("type", "STRING",
                            "description", "13 digitos do codigo de barras, ou vazio"),
                    "lote", Map.of("type", "STRING",
                            "description", "lote exatamente como impresso, sem o rotulo, ou vazio"),
                    "validade", Map.of("type", "STRING",
                            "description", "AAAA-MM-DD, ou AAAA-MM quando so ha mes e ano, ou vazio")),
            "required", List.of("classe", "certeza", "motivo", "ean", "lote", "validade"));

    private final RestClient gemini;
    private final RepositorioDeFotos fotos;
    private final ObjectMapper json;
    private final String modelo;
    private final String modeloReserva;

    public AvaliadorGemini(@Value("${medshare.gemini.chave}") String chave,
                           @Value("${medshare.gemini.modelo:gemini-3.8-flash}") String modelo,
                           @Value("${medshare.gemini.modelo-reserva:gemini-3.5-flash-lite}") String modeloReserva,
                           RepositorioDeFotos fotos, ObjectMapper json) {
        this.modelo = modelo;
        this.modeloReserva = modeloReserva == null || modeloReserva.isBlank() ? null : modeloReserva;
        this.fotos = fotos;
        this.json = json;

        // Sem limite de tempo, um Gemini lento prenderia a tela de doacao do
        // app (a leitura acontece dentro do cadastro). Com o modelo reserva,
        // o pior caso sao duas esperas destas.
        var cliente = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        cliente.setReadTimeout(Duration.ofSeconds(25));

        // A chave vai no cabecalho, e nao na URL: URL aparece em log e em
        // mensagem de erro, cabecalho nao.
        this.gemini = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .requestFactory(cliente)
                .defaultHeader("x-goog-api-key", chave)
                .build();
        log.info("Avaliador Gemini ativo, modelo {} (reserva: {})", modelo,
                this.modeloReserva == null ? "nenhum" : this.modeloReserva);
    }

    /**
     * Tenta o modelo principal e, se o Google estiver sobrecarregado (503),
     * com a cota gratuita esgotada (429) ou lento demais, repete no modelo
     * reserva. No plano gratuito o 503 e frequente nos modelos mais novos;
     * sem a reserva, quase toda doacao cairia na central por falta de leitura.
     */
    @Override
    public LeituraDaEmbalagem avaliar(String fotoUrl) {
        byte[] imagem;
        try {
            imagem = lerDoDisco(fotoUrl);
        } catch (Exception e) {
            log.warn("Falha ao ler a foto {}: {}", fotoUrl, e.getMessage());
            return LeituraDaEmbalagem.indisponivel("Não foi possível analisar a foto automaticamente");
        }

        List<String> tentativas = modeloReserva == null ? List.of(modelo) : List.of(modelo, modeloReserva);
        for (String modeloDaVez : tentativas) {
            try {
                JsonNode resposta = chamarGemini(modeloDaVez, imagem, fotos.tipoDa(fotoUrl));
                return interpretar(resposta, json).feitaPor(modeloDaVez);
            } catch (HttpStatusCodeException e) {
                if (!valeTentarDeNovo(e)) {
                    log.warn("Gemini ({}) respondeu {} ao avaliar {}", modeloDaVez, e.getStatusCode(), fotoUrl);
                    break;
                }
                log.warn("Gemini ({}) indisponível agora ({}){}", modeloDaVez, e.getStatusCode(),
                        modeloDaVez.equals(modelo) && modeloReserva != null ? "; tentando " + modeloReserva : "");
            } catch (org.springframework.web.client.ResourceAccessException e) {
                log.warn("Gemini ({}) não respondeu a tempo: {}", modeloDaVez, e.getMessage());
            } catch (Exception e) {
                log.warn("Falha ao interpretar a leitura do Gemini ({}): {}", modeloDaVez, e.getMessage());
                break;
            }
        }
        return LeituraDaEmbalagem.indisponivel("Não foi possível analisar a foto automaticamente");
    }

    /** 503 (sobrecarga), 429 (cota gratuita) e 500/504 sao passageiros; 400/403 nao. */
    private static boolean valeTentarDeNovo(HttpStatusCodeException e) {
        return e.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)
                || e.getStatusCode().is5xxServerError();
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
    private byte[] lerDoDisco(String fotoUrl) throws IOException {
        Path arquivo = fotos.arquivoDa(fotoUrl)
                .orElseThrow(() -> new IllegalStateException("foto fora do armazenamento do MedShare"));
        byte[] conteudo = Files.readAllBytes(arquivo);
        if (conteudo.length == 0) {
            throw new IllegalStateException("foto vazia");
        }
        return conteudo;
    }

    private JsonNode chamarGemini(String modeloDaVez, byte[] imagem, String tipo) {
        Map<String, Object> corpo = Map.of(
                "system_instruction", Map.of("parts", List.of(Map.of("text", INSTRUCOES))),
                "contents", List.of(Map.of("parts", List.of(
                        Map.of("inline_data", Map.of(
                                "mime_type", tipo,
                                "data", Base64.getEncoder().encodeToString(imagem)))))),
                "generationConfig", Map.of(
                        "temperature", 0.1,
                        "responseMimeType", "application/json",
                        "responseSchema", ESQUEMA_DA_RESPOSTA));

        return gemini.post()
                .uri("/models/{modelo}:generateContent", modeloDaVez)
                .body(corpo)
                .retrieve()
                .body(JsonNode.class);
    }

    /** Visivel ao pacote para o teste: converte a resposta do Gemini em leitura. */
    static LeituraDaEmbalagem interpretar(JsonNode resposta, ObjectMapper json) throws IOException {
        String texto = resposta == null ? null : resposta.path("candidates").path(0)
                .path("content").path("parts").path(0).path("text").asText(null);
        if (texto == null) {
            return LeituraDaEmbalagem.indisponivel("O serviço de visão não devolveu uma leitura");
        }

        JsonNode leitura = json.readTree(texto);
        // Os limites acompanham as colunas de analise_pre_validacao: um texto
        // longo demais vindo do modelo nao pode derrubar o cadastro da doacao.
        return new LeituraDaEmbalagem(
                apenasDigitosOuNulo(textoOuNulo(leitura, "ean")),
                limitar(textoOuNulo(leitura, "lote"), 30),
                validade(textoOuNulo(leitura, "validade")),
                classeDe(leitura.path("classe").asText("invalida")),
                certezaDe(leitura.path("certeza").asText("baixa")),
                limitar(leitura.path("motivo").asText(""), 500));
    }

    private static String limitar(String texto, int tamanho) {
        return texto == null || texto.length() <= tamanho ? texto : texto.substring(0, tamanho);
    }

    /**
     * "2027-03-15" vira o proprio dia; "2027-03" (so mes e ano, como vem na
     * maioria das caixas) vira o ultimo dia do mes — o remedio vale ate o fim
     * do mes impresso. Qualquer outra coisa: ilegivel.
     */
    static LocalDate validade(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            return texto.length() == 7
                    ? YearMonth.parse(texto).atEndOfMonth()
                    : LocalDate.parse(texto);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String textoOuNulo(JsonNode no, String campo) {
        String valor = no.path(campo).asText("");
        return valor.isBlank() ? null : valor.trim();
    }

    private static String apenasDigitosOuNulo(String texto) {
        if (texto == null) {
            return null;
        }
        String digitos = texto.replaceAll("\\D", "");
        return digitos.length() >= 8 && digitos.length() <= 14 ? digitos : null;
    }

    private static ClasseEmbalagem classeDe(String valor) {
        return switch (valor.toLowerCase()) {
            case "lacrada" -> ClasseEmbalagem.LACRADA;
            case "violada" -> ClasseEmbalagem.VIOLADA;
            case "danificada" -> ClasseEmbalagem.DANIFICADA;
            default -> ClasseEmbalagem.INVALIDA;
        };
    }

    private static Certeza certezaDe(String valor) {
        return switch (valor.toLowerCase()) {
            case "alta" -> Certeza.ALTA;
            case "media", "média" -> Certeza.MEDIA;
            default -> Certeza.BAIXA;
        };
    }
}
