package br.com.medshare.reserva;

import br.com.medshare.comum.RegraDeNegocioViolada;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Reserva")
class ReservaTest {

    private Reserva reserva(int horas) {
        return new Reserva("ABC12345", null, null, horas);
    }

    @Test
    @DisplayName("nasce ativa com o prazo de retirada contado a partir de agora")
    void nasceAtiva() {
        Reserva reserva = reserva(72);

        assertThat(reserva.getStatus()).isEqualTo(StatusReserva.ATIVA);
        assertThat(reserva.getExpiraEm()).isAfter(OffsetDateTime.now().plusHours(71));
        assertThat(reserva.venceu()).isFalse();
    }

    @Test
    @DisplayName("passado o prazo, esta vencida")
    void venceDepoisDoPrazo() {
        Reserva reserva = reserva(72);
        ReflectionTestUtils.setField(reserva, "expiraEm", OffsetDateTime.now().minusHours(1));

        assertThat(reserva.venceu()).isTrue();
    }

    @Test
    @DisplayName("nao da para concluir duas vezes a mesma reserva")
    void naoConcluiDuasVezes() {
        Reserva reserva = reserva(72);
        reserva.concluir();

        assertThatThrownBy(reserva::concluir)
                .isInstanceOf(RegraDeNegocioViolada.class)
                .hasMessageContaining("CONCLUIDA");
    }

    @Test
    @DisplayName("reserva ja expirada nao pode ser cancelada")
    void expiradaNaoCancela() {
        Reserva reserva = reserva(72);
        reserva.expirar();

        assertThatThrownBy(reserva::cancelar).isInstanceOf(RegraDeNegocioViolada.class);
    }
}
