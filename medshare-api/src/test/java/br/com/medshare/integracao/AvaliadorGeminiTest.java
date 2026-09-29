package br.com.medshare.integracao;

import br.com.medshare.prevalidacao.Certeza;
import br.com.medshare.prevalidacao.ClasseEmbalagem;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Avaliador Gemini: interpretacao da resposta do modelo")
class AvaliadorGeminiTest {

    private final ObjectMapper json = new ObjectMapper();

    /** A resposta do generateContent traz o JSON pedido como texto dentro de candidates. */
    private LeituraDaEmbalagem ler(String leitura) throws Exception {
        String resposta = json.writeValueAsString(java.util.Map.of("candidates", java.util.List.of(
                java.util.Map.of("content", java.util.Map.of("parts", java.util.List.of(
                        java.util.Map.of("text", leitura)))))));
        return AvaliadorGemini.interpretar(json.readTree(resposta), json);
    }

    @Test
    @DisplayName("le lote, validade com dia e codigo de barras")
    void leituraCompleta() throws Exception {
        var leitura = ler("""
                {"classe":"lacrada","certeza":"alta","motivo":"caixa lacrada",
                 "ean":"7896226104058","lote":"ALC2026A","validade":"2027-03-15"}
                """);

        assertThat(leitura.classe()).isEqualTo(ClasseEmbalagem.LACRADA);
        assertThat(leitura.certeza()).isEqualTo(Certeza.ALTA);
        assertThat(leitura.lote()).isEqualTo("ALC2026A");
        assertThat(leitura.validade()).isEqualTo(LocalDate.of(2027, 3, 15));
        assertThat(leitura.ean()).isEqualTo("7896226104058");
    }

    @Test
    @DisplayName("validade so com mes e ano vale ate o ultimo dia do mes")
    void validadeMesEAno() {
        assertThat(AvaliadorGemini.validade("2027-02")).isEqualTo(LocalDate.of(2027, 2, 28));
        assertThat(AvaliadorGemini.validade("03/2027")).isNull();
        assertThat(AvaliadorGemini.validade(null)).isNull();
    }

    @Test
    @DisplayName("campo vazio e ilegivel, e nao texto em branco")
    void camposVazios() throws Exception {
        var leitura = ler("""
                {"classe":"invalida","certeza":"baixa","motivo":"foto desfocada",
                 "ean":"","lote":"  ","validade":""}
                """);

        assertThat(leitura.ean()).isNull();
        assertThat(leitura.lote()).isNull();
        assertThat(leitura.validade()).isNull();
    }

    @Test
    @DisplayName("lote longo demais e cortado para caber no banco, sem derrubar a doacao")
    void loteLongo() throws Exception {
        var leitura = ler("""
                {"classe":"lacrada","certeza":"media","motivo":"ok",
                 "ean":"789 6226 10405 8","lote":"%s","validade":"2027-03"}
                """.formatted("X".repeat(80)));

        assertThat(leitura.lote()).hasSize(30);
        assertThat(leitura.ean()).isEqualTo("7896226104058");
    }

    @Test
    @DisplayName("resposta sem candidatos (bloqueio, cota) vira indisponivel")
    void semCandidatos() throws Exception {
        var leitura = AvaliadorGemini.interpretar(json.readTree("{\"candidates\":[]}"), json);

        assertThat(leitura.certeza()).isEqualTo(Certeza.BAIXA);
        assertThat(leitura.classe()).isEqualTo(ClasseEmbalagem.INVALIDA);
    }
}
