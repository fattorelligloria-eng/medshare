package br.com.medshare.prevalidacao;

import br.com.medshare.comum.FabricaDeTestes;
import br.com.medshare.doacao.Doacao;
import br.com.medshare.usuario.Papel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static br.com.medshare.comum.FabricaDeTestes.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * RN10 - o teste mais importante do projeto.
 *
 * Cada caso aqui responde a mesma pergunta: "a IA pode decidir isto sozinha?".
 * A resposta so e sim no cenario em que tudo bate. Em qualquer outro, tem que
 * ir para um humano — e cada teste abaixo e uma forma diferente de "qualquer
 * outro".
 */
@DisplayName("Regra de decisao da pre-validacao (RN10)")
class RegraDeDecisaoDaPreValidacaoTest {

    private static final LocalDate VALIDADE = LocalDate.now().plusDays(180);
    private static final String LOTE = "ALC2026A";
    private static final String EAN = "7890000000001";

    private final RegraDeDecisaoDaPreValidacao regra = new RegraDeDecisaoDaPreValidacao();

    private Doacao doacao() {
        var medicamento = medicamento(1L, "Alecensa", "35000.00");
        org.springframework.test.util.ReflectionTestUtils.setField(medicamento, "ean", EAN);
        return Doacao.cadastrar("MS-000001",
                usuario(1L, "Ana", Papel.DOADOR), medicamento, LOTE, VALIDADE,
                "http://x/f.jpg", FabricaDeTestes.PISO_DE_PRECO, FabricaDeTestes.DIAS_MINIMOS);
    }

    private AnalisePreValidacao analise(ClasseEmbalagem classe, Certeza certeza,
                                        String ean, String lote, LocalDate validade) {
        return new AnalisePreValidacao(doacao(), ean, lote, validade, classe, certeza,
                "motivo de teste", "teste");
    }

    @Test
    @DisplayName("segue sozinha so quando tudo bate: lacrada, certeza alta e sem divergencia")
    void casoFelizSegueSemHumano() {
        var analise = analise(ClasseEmbalagem.LACRADA, Certeza.ALTA, EAN, LOTE, VALIDADE);

        assertThat(regra.decidirSobre(analise)).isEqualTo(DecisaoDaPreValidacao.SEGUIR);
    }

    @Test
    @DisplayName("certeza media manda para a central mesmo com tudo batendo")
    void certezaMediaChamaHumano() {
        var analise = analise(ClasseEmbalagem.LACRADA, Certeza.MEDIA, EAN, LOTE, VALIDADE);

        assertThat(regra.decidirSobre(analise))
                .isEqualTo(DecisaoDaPreValidacao.ENVIAR_PARA_CENTRAL);
        assertThat(regra.explicarEncaminhamento(analise)).contains("certeza média");
    }

    @Test
    @DisplayName("caixa violada nunca passa automaticamente")
    void violadaChamaHumano() {
        var analise = analise(ClasseEmbalagem.VIOLADA, Certeza.ALTA, EAN, LOTE, VALIDADE);

        assertThat(regra.decidirSobre(analise))
                .isEqualTo(DecisaoDaPreValidacao.ENVIAR_PARA_CENTRAL);
        assertThat(regra.explicarEncaminhamento(analise)).contains("violada");
    }

    @Test
    @DisplayName("campo ilegivel chama humano - nao ler nao e o mesmo que estar certo")
    void campoIlegivelChamaHumano() {
        var analise = analise(ClasseEmbalagem.LACRADA, Certeza.ALTA, EAN, null, VALIDADE);

        assertThat(analise.leuTodosOsCampos()).isFalse();
        assertThat(regra.decidirSobre(analise))
                .isEqualTo(DecisaoDaPreValidacao.ENVIAR_PARA_CENTRAL);
    }

    @Test
    @DisplayName("lote divergente chama humano e diz exatamente qual foi a diferenca")
    void loteDivergenteChamaHumano() {
        var analise = analise(ClasseEmbalagem.LACRADA, Certeza.ALTA, EAN, "OUTRO_LOTE", VALIDADE);

        assertThat(analise.temDivergencia()).isTrue();
        assertThat(analise.getDivergencias().get(0)).contains("OUTRO_LOTE").contains(LOTE);
        assertThat(regra.decidirSobre(analise))
                .isEqualTo(DecisaoDaPreValidacao.ENVIAR_PARA_CENTRAL);
    }

    @Test
    @DisplayName("validade divergente chama humano")
    void validadeDivergenteChamaHumano() {
        var analise = analise(ClasseEmbalagem.LACRADA, Certeza.ALTA, EAN, LOTE,
                VALIDADE.plusMonths(6));

        assertThat(regra.decidirSobre(analise))
                .isEqualTo(DecisaoDaPreValidacao.ENVIAR_PARA_CENTRAL);
    }

    @Test
    @DisplayName("caixa com so mes e ano de validade confere com o dia que o doador digitou")
    void validadeSoMesEAnoConfere() {
        var ultimoDiaDoMes = java.time.YearMonth.from(VALIDADE).atEndOfMonth();
        var analise = analise(ClasseEmbalagem.LACRADA, Certeza.ALTA, EAN, LOTE, ultimoDiaDoMes);

        assertThat(analise.temDivergencia()).isFalse();
        assertThat(regra.decidirSobre(analise)).isEqualTo(DecisaoDaPreValidacao.SEGUIR);
    }

    @Test
    @DisplayName("espaco, traco e minuscula no lote nao sao divergencia")
    void loteComFormatacaoDiferenteConfere() {
        var analise = analise(ClasseEmbalagem.LACRADA, Certeza.ALTA, EAN, "alc-2026 a", VALIDADE);

        assertThat(analise.temDivergencia()).isFalse();
    }

    @Test
    @DisplayName("codigo de barras de outro medicamento chama humano")
    void eanDivergenteChamaHumano() {
        var analise = analise(ClasseEmbalagem.LACRADA, Certeza.ALTA, "7899999999999",
                LOTE, VALIDADE);

        assertThat(regra.decidirSobre(analise))
                .isEqualTo(DecisaoDaPreValidacao.ENVIAR_PARA_CENTRAL);
    }

    @Test
    @DisplayName("a IA nunca tem 'recusar' como opcao - so seguir ou chamar alguem")
    void naoExisteRecusaAutomatica() {
        assertThat(DecisaoDaPreValidacao.values())
                .containsExactlyInAnyOrder(DecisaoDaPreValidacao.SEGUIR,
                        DecisaoDaPreValidacao.ENVIAR_PARA_CENTRAL);
    }
}
