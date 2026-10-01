package br.com.medshare.medicamento.cmed;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Texto da CMED: caixa legivel para o catalogo")
class TextoDaCmedTest {

    @Test
    @DisplayName("nome comercial ganha iniciais maiusculas, ligacoes ficam minusculas")
    void nome() {
        assertThat(TextoDaCmed.nome("HUMIRA")).isEqualTo("Humira");
        assertThat(TextoDaCmed.nome("ÁCIDO ZOLEDRÔNICO DE SÓDIO")).isEqualTo("Ácido Zoledrônico de Sódio");
        assertThat(TextoDaCmed.nome("DE NOL")).isEqualTo("De Nol");
    }

    @Test
    @DisplayName("principio ativo vira substantivo comum, cada componente da associacao com inicial maiuscula")
    void principio() {
        assertThat(TextoDaCmed.principio("ADALIMUMABE")).isEqualTo("Adalimumabe");
        assertThat(TextoDaCmed.principio("LAMIVUDINA;ZIDOVUDINA")).isEqualTo("Lamivudina;Zidovudina");
        assertThat(TextoDaCmed.principio("SOFOSBUVIR + VELPATASVIR")).isEqualTo("Sofosbuvir + Velpatasvir");
    }

    @Test
    @DisplayName("apresentacao fica minuscula, com as siglas de unidade preservadas")
    void apresentacao() {
        assertThat(TextoDaCmed.apresentacao("100 MG/ML SOL INJ CT 2 BL X SER PREENC VD TRANS X 0,2 ML"))
                .isEqualTo("100 mg/ml sol inj ct 2 bl x ser preenc vd trans x 0,2 ml");
        assertThat(TextoDaCmed.apresentacao("4000 UI PO LIOF CT FA")).isEqualTo("4000 UI po liof ct fa");
    }

    @Test
    @DisplayName("texto que ja tem minusculas fica como veio")
    void naoMexeNoQueJaEstaEscrito() {
        assertThat(TextoDaCmed.nome("Alecensa")).isEqualTo("Alecensa");
        assertThat(TextoDaCmed.principio("")).isEqualTo("");
        assertThat(TextoDaCmed.apresentacao("150 mg")).isEqualTo("150 mg");
    }
}
