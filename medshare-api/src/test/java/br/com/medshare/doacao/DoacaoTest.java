package br.com.medshare.doacao;

import br.com.medshare.comum.FabricaDeTestes;
import br.com.medshare.comum.RegraDeNegocioViolada;
import br.com.medshare.farmacia.PontoDeColeta;
import br.com.medshare.medicamento.Medicamento;
import br.com.medshare.usuario.Papel;
import br.com.medshare.usuario.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static br.com.medshare.comum.FabricaDeTestes.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Doacao")
class DoacaoTest {

    private final Usuario doador = usuario(1L, "Ana", Papel.DOADOR);
    private final Usuario farmaceutico = usuario(2L, "Carla", Papel.FARMACEUTICO);
    private final Medicamento altoCusto = medicamento(1L, "Alecensa", "35000.00");
    private final Medicamento baratinho = medicamento(2L, "Dipirona", "12.90");
    private final PontoDeColeta farmacia = pontoDeColeta(1L, "Farmacia Se", -23.5505, -46.6339);

    private Doacao doacaoValida() {
        return Doacao.cadastrar("MS-000001", doador, altoCusto, "L1",
                LocalDate.now().plusDays(180), "http://x/f.jpg", PISO_DE_PRECO, DIAS_MINIMOS);
    }

    @Nested
    @DisplayName("no cadastro")
    class NoCadastro {

        @Test
        @DisplayName("RN07: recusa medicamento com PMC abaixo do piso")
        void recusaMedicamentoBarato() {
            assertThatThrownBy(() -> Doacao.cadastrar("MS-000002", doador, baratinho, "L1",
                    LocalDate.now().plusDays(180), "http://x/f.jpg", PISO_DE_PRECO, DIAS_MINIMOS))
                    .isInstanceOf(RegraDeNegocioViolada.class)
                    .hasMessageContaining("150")
                    .extracting("regra").isEqualTo("RN07");
        }

        @Test
        @DisplayName("RN02: recusa validade abaixo de 30 dias")
        void recusaValidadeCurta() {
            assertThatThrownBy(() -> Doacao.cadastrar("MS-000003", doador, altoCusto, "L1",
                    LocalDate.now().plusDays(10), "http://x/f.jpg", PISO_DE_PRECO, DIAS_MINIMOS))
                    .isInstanceOf(RegraDeNegocioViolada.class)
                    .extracting("regra").isEqualTo("RN02");
        }

        @Test
        @DisplayName("RN02: aceita exatamente 30 dias de validade")
        void aceitaTrintaDiasExatos() {
            Doacao doacao = Doacao.cadastrar("MS-000004", doador, altoCusto, "L1",
                    LocalDate.now().plusDays(30), "http://x/f.jpg", PISO_DE_PRECO, DIAS_MINIMOS);

            assertThat(doacao.getStatus()).isEqualTo(StatusDoacao.CADASTRADA);
        }

        @Test
        @DisplayName("RN06: ja nasce com o evento de cadastro no historico")
        void nasceComHistorico() {
            Doacao doacao = doacaoValida();

            assertThat(doacao.getHistorico()).hasSize(1);
            assertThat(doacao.getHistorico().get(0).getTipo()).isEqualTo(TipoEvento.CADASTRO);
        }
    }

    @Nested
    @DisplayName("no ciclo de vida")
    class NoCicloDeVida {

        @Test
        @DisplayName("percorre o caminho feliz ate ENTREGUE")
        void caminhoFeliz() {
            Doacao doacao = doacaoValida();

            doacao.preValidar(doador);
            doacao.agendar(farmacia, OffsetDateTime.now().plusDays(2), doador);
            doacao.receber(farmaceutico);
            doacao.validar(farmaceutico);
            doacao.disponibilizar(farmaceutico);
            doacao.reservar("ABC12345", doador);
            doacao.entregar(farmaceutico);

            assertThat(doacao.getStatus()).isEqualTo(StatusDoacao.ENTREGUE);
        }

        @Test
        @DisplayName("nao deixa pular etapas do ciclo")
        void recusaPuloDeEtapa() {
            Doacao doacao = doacaoValida();

            assertThatThrownBy(() -> doacao.entregar(farmaceutico))
                    .isInstanceOf(RegraDeNegocioViolada.class)
                    .hasMessageContaining("CADASTRADA");
        }

        @Test
        @DisplayName("RN10: divergencia manda para analise humana, nao recusa sozinha")
        void divergenciaVaiParaCentral() {
            Doacao doacao = doacaoValida();

            doacao.enviarParaCentral("lote lido difere do informado", doador);

            assertThat(doacao.getStatus()).isEqualTo(StatusDoacao.EM_ANALISE_CENTRAL);
            assertThat(doacao.getStatus().proximosPossiveis())
                    .containsExactlyInAnyOrder(StatusDoacao.PRE_VALIDADA, StatusDoacao.RECUSADA);
        }

        @Test
        @DisplayName("reserva expirada devolve a caixa para o estoque")
        void reservaExpiradaVoltaParaEstoque() {
            Doacao doacao = doacaoValida();
            doacao.preValidar(doador);
            doacao.agendar(farmacia, OffsetDateTime.now().plusDays(2), doador);
            doacao.receber(farmaceutico);
            doacao.validar(farmaceutico);
            doacao.disponibilizar(farmaceutico);
            doacao.reservar("ABC12345", doador);

            doacao.liberarReservaExpirada();

            assertThat(doacao.getStatus()).isEqualTo(StatusDoacao.DISPONIVEL);
        }

        @Test
        @DisplayName("RN06: cada transicao deixa um evento, e o historico e so leitura")
        void historicoRegistraTudo() {
            Doacao doacao = doacaoValida();
            doacao.preValidar(doador);
            doacao.agendar(farmacia, OffsetDateTime.now().plusDays(2), doador);

            assertThat(doacao.getHistorico()).hasSize(3);
            assertThat(doacao.getHistorico())
                    .extracting(EventoHistorico::getTipo)
                    .containsExactly(TipoEvento.CADASTRO, TipoEvento.PRE_VALIDACAO,
                            TipoEvento.AGENDAMENTO);

            assertThatThrownBy(() -> doacao.getHistorico().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("estado final nao vai para lugar nenhum")
        void estadoFinalEFinal() {
            Doacao doacao = doacaoValida();
            doacao.recusar("PMC abaixo do piso", doador);

            assertThat(doacao.getStatus().ehFinal()).isTrue();
            assertThatThrownBy(() -> doacao.preValidar(doador))
                    .isInstanceOf(RegraDeNegocioViolada.class);
        }
    }

    @Nested
    @DisplayName("na prateleira")
    class NaPrateleira {

        @Test
        @DisplayName("RN02: caixa que nao alcanca mais os 30 dias e sinalizada")
        void avisaQuandoPerdeOPrazo() {
            Doacao doacao = Doacao.cadastrar("MS-000005", doador, altoCusto, "L1",
                    LocalDate.now().plusDays(40), "http://x/f.jpg", PISO_DE_PRECO, DIAS_MINIMOS);

            assertThat(doacao.aindaTemValidadeSuficiente(30)).isTrue();
            assertThat(doacao.aindaTemValidadeSuficiente(60)).isFalse();
        }
    }
}
