package br.com.medshare.doacao.dto;

import br.com.medshare.comum.FabricaDeTestes;
import br.com.medshare.usuario.Papel;
import br.com.medshare.usuario.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O rotulo do responsavel e o ponto onde a RN05 pode vazar sem ninguem notar:
 * o resto da tela pode estar certo e, ainda assim, o nome de quem recebeu
 * aparecer numa linha do historico. Estes testes existem para travar isso.
 */
class EventoResumidoTest {

    private final Usuario doador = FabricaDeTestes.usuario(1L, "Gloria Fattorelli", Papel.DOADOR);

    @Test
    @DisplayName("RN05: o nome de quem recebe nunca aparece no historico")
    void nomeDoBeneficiarioNaoVaza() {
        Usuario beneficiaria = FabricaDeTestes.usuario(2L, "Joana Silva", Papel.BENEFICIARIO);

        String rotulo = EventoResumido.rotuloDe(beneficiaria, doador);

        assertThat(rotulo).isEqualTo("Beneficiário");
        assertThat(rotulo).doesNotContain("Joana");
    }

    @Test
    @DisplayName("Quem acumula beneficiario e doador continua sem ter o nome exposto")
    void acumularPapeisNaoAbreExcecao() {
        Usuario osDois = FabricaDeTestes.usuario(3L, "Carlos Lima", Papel.BENEFICIARIO, Papel.DOADOR);

        assertThat(EventoResumido.rotuloDe(osDois, doador)).doesNotContain("Carlos");
    }

    @Test
    @DisplayName("O farmaceutico aparece pelo nome: ele responde pela conferencia")
    void farmaceuticoApareceNomeado() {
        Usuario farmaceutica = FabricaDeTestes.usuario(4L, "Ana Paula Souza", Papel.FARMACEUTICO);

        assertThat(EventoResumido.rotuloDe(farmaceutica, doador))
                .isEqualTo("Farmacêutico(a) Ana");
    }

    @Test
    @DisplayName("A central aparece como equipe, nao como pessoa")
    void centralApareceComoEquipe() {
        Usuario analista = FabricaDeTestes.usuario(5L, "Pedro Alves", Papel.ADMIN);

        assertThat(EventoResumido.rotuloDe(analista, doador)).isEqualTo("Equipe MedShare");
    }

    @Test
    @DisplayName("Quem olha o proprio passo le 'Você'")
    void oProprioUsuarioSeReconhece() {
        assertThat(EventoResumido.rotuloDe(doador, doador)).isEqualTo("Você");
    }

    @Test
    @DisplayName("Evento sem responsavel e automatico: a tela escreve 'Sistema'")
    void eventoAutomaticoNaoTemResponsavel() {
        assertThat(EventoResumido.rotuloDe(null, doador)).isNull();
    }

    @Test
    @DisplayName("Um farmaceutico olhando o proprio registro tambem le 'Você'")
    void oProprioFarmaceuticoSeReconhece() {
        Usuario farmaceutica = FabricaDeTestes.usuario(6L, "Ana Paula Souza", Papel.FARMACEUTICO);

        assertThat(EventoResumido.rotuloDe(farmaceutica, farmaceutica)).isEqualTo("Você");
    }
}
