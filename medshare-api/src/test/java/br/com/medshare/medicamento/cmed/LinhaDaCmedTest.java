package br.com.medshare.medicamento.cmed;

import br.com.medshare.medicamento.Tarja;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Linha da CMED: conversao de uma apresentacao da lista oficial")
class LinhaDaCmedTest {

    /** O cabecalho real da lista, com acento, espaco duplo e coluna a mais no meio. */
    private static final List<String> CABECALHO = List.of(
            "SUBSTÂNCIA", "CNPJ", "LABORATÓRIO", "CÓDIGO GGREM", "REGISTRO", "EAN 1",
            "PRODUTO", "APRESENTAÇÃO", "PMC 12 %", "PMC 18 %", "PMC 18 %  ALC", "TARJA");

    private final LinhaDaCmed.Colunas colunas =
            LinhaDaCmed.Colunas.doCabecalho(CABECALHO, "PMC 18 %").orElseThrow();

    @Test
    @DisplayName("converte uma apresentacao com PMC, no formato brasileiro de numero")
    void converteLinhaCompleta() {
        var linha = LinhaDaCmed.de(List.of(
                "ABATACEPTE", "56.998.982/0001-07", "BRISTOL-MYERS SQUIBB", "505113100020505",
                "1018003900078", "7896016808197", "ORENCIA", "125 MG/ML SOL INJ SC CT 4 SER",
                "10563,39", "11.336,31", "11233,15", "Tarja Vermelha"), colunas).orElseThrow();

        assertThat(linha.codigoGgrem()).isEqualTo("505113100020505");
        assertThat(linha.nomeComercial()).isEqualTo("Orencia");
        assertThat(linha.principioAtivo()).isEqualTo("Abatacepte");
        assertThat(linha.ean()).isEqualTo("7896016808197");
        assertThat(linha.pmc()).isEqualByComparingTo("11336.31");   // coluna de SP, nao a de 12%
        assertThat(linha.tarja()).isEqualTo(Tarja.VERMELHA);
    }

    @Test
    @DisplayName("ignora apresentacao sem PMC: e de uso hospitalar, nao passa por farmacia")
    void ignoraSemPmc() {
        var linha = LinhaDaCmed.de(List.of(
                "ABATACEPTE", "", "BMS", "505107701157215", "1018003900019", "7896016806469",
                "ORENCIA", "250 MG PO LIOF", "", "", "", "Tarja Vermelha"), colunas);

        assertThat(linha).isEmpty();
    }

    @Test
    @DisplayName("EAN em branco (\"    -     \") vira nulo, e nao texto lixo")
    void eanEmBranco() {
        var linha = LinhaDaCmed.de(List.of(
                "DIPIRONA", "", "LAB", "538912020009303", "1705600230032", "    -     ",
                "PRODUTO X", "500 MG", "", "12,90", "", "- (*) "), colunas).orElseThrow();

        assertThat(linha.ean()).isNull();
        assertThat(linha.tarja()).isEqualTo(Tarja.NAO_INFORMADA);
    }

    @Test
    @DisplayName("le as quatro formas de tarja que a lista usa")
    void tarjas() {
        assertThat(LinhaDaCmed.tarja("Tarja Preta")).isEqualTo(Tarja.PRETA);
        assertThat(LinhaDaCmed.tarja("Tarja Vermelha sob restrição")).isEqualTo(Tarja.VERMELHA);
        assertThat(LinhaDaCmed.tarja("Tarja Sem Tarja")).isEqualTo(Tarja.SEM_TARJA);
        assertThat(LinhaDaCmed.tarja("- (*) ")).isEqualTo(Tarja.NAO_INFORMADA);
    }

    @Test
    @DisplayName("aceita preco em texto brasileiro e em celula numerica")
    void precos() {
        assertThat(LinhaDaCmed.preco("1.234,56")).contains(new BigDecimal("1234.56"));
        assertThat(LinhaDaCmed.preco("1234.56")).contains(new BigDecimal("1234.56"));
        assertThat(LinhaDaCmed.preco("0,00")).isEmpty();
        assertThat(LinhaDaCmed.preco("-")).isEmpty();
    }

    @Test
    @DisplayName("linha que nao e o cabecalho nao vira cabecalho")
    void naoConfundeTextoComCabecalho() {
        assertThat(LinhaDaCmed.Colunas.doCabecalho(List.of("Publicada em 09/09/2026"), "PMC 18 %"))
                .isEmpty();
    }

    @Test
    @DisplayName("planilha sem a coluna de PMC escolhida e recusada com mensagem clara")
    void recusaSemColunaDePmc() {
        assertThatThrownBy(() -> LinhaDaCmed.Colunas.doCabecalho(CABECALHO, "PMC 99 %"))
                .isInstanceOf(ArquivoCmedInvalido.class)
                .hasMessageContaining("PMC 99 %");
    }
}
