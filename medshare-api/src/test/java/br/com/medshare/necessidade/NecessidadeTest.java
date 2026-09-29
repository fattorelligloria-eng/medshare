package br.com.medshare.necessidade;

import br.com.medshare.comum.RegraDeNegocioViolada;
import br.com.medshare.usuario.Papel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static br.com.medshare.comum.FabricaDeTestes.medicamento;
import static br.com.medshare.comum.FabricaDeTestes.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Necessidade e receita (RN03)")
class NecessidadeTest {

    private Necessidade necessidade() {
        return new Necessidade(usuario(1L, "Bruno", Papel.BENEFICIARIO),
                medicamento(1L, "Alecensa", "35000.00"));
    }

    private Receita receita(LocalDate validade) {
        return new Receita(necessidade(), "http://x/r.jpg",
                LocalDate.now().minusDays(5), validade, "123456", "SP");
    }

    @Test
    @DisplayName("sem receita anexada nao reserva")
    void semReceitaNaoReserva() {
        assertThatThrownBy(() -> necessidade().exigirReceitaValida())
                .isInstanceOf(RegraDeNegocioViolada.class)
                .extracting("regra").isEqualTo("RN03");
    }

    @Test
    @DisplayName("receita vencida nao vale")
    void receitaVencidaNaoVale() {
        Necessidade necessidade = necessidade();
        necessidade.anexarReceita(receita(LocalDate.now().minusDays(1)));

        assertThatThrownBy(necessidade::exigirReceitaValida)
                .isInstanceOf(RegraDeNegocioViolada.class)
                .hasMessageContaining("venceu");
    }

    @Test
    @DisplayName("receita que vence hoje ainda vale")
    void receitaDeHojeVale() {
        Necessidade necessidade = necessidade();
        necessidade.anexarReceita(receita(LocalDate.now()));

        assertThat(necessidade.getReceita().estaValida()).isTrue();
        necessidade.exigirReceitaValida();
    }

    @Test
    @DisplayName("encerrar marca a data e desativa o pedido")
    void encerrarDesativa() {
        Necessidade necessidade = necessidade();
        necessidade.encerrar();

        assertThat(necessidade.isAtiva()).isFalse();
        assertThat(necessidade.getEncerradaEm()).isNotNull();
    }
}
