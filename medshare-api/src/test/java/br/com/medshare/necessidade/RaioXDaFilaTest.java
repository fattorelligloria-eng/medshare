package br.com.medshare.necessidade;

import br.com.medshare.comum.FabricaDeTestes;
import br.com.medshare.comum.PropriedadesDoMedShare;
import br.com.medshare.doacao.EventoHistoricoRepository;
import br.com.medshare.integracao.ConsultaDeCadUnico;
import br.com.medshare.medicamento.Medicamento;
import br.com.medshare.medicamento.MedicamentoRepository;
import br.com.medshare.necessidade.dto.RaioXDaFila;
import br.com.medshare.notificacao.ServicoDeNotificacao;
import br.com.medshare.reserva.ServicoDeOferta;
import br.com.medshare.usuario.Papel;
import br.com.medshare.usuario.ServicoDeUsuario;
import br.com.medshare.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * O raio-x da fila.
 *
 * O teste que mais importa aqui e o ultimo: o DTO nao tem posicao na fila, e
 * nao pode ganhar uma sem que alguem perceba. Neste sistema a ordem depende da
 * farmacia onde a caixa aparece, entao "voce e o 3o" seria um numero inventado.
 */
class RaioXDaFilaTest {

    private NecessidadeRepository necessidades;
    private EventoHistoricoRepository eventos;
    private ServicoDeNecessidade servico;

    private Necessidade pedido;

    @BeforeEach
    void preparar() {
        necessidades = mock(NecessidadeRepository.class);
        eventos = mock(EventoHistoricoRepository.class);

        servico = new ServicoDeNecessidade(
                necessidades, mock(ReceitaRepository.class),
                mock(VerificacaoCadUnicoRepository.class), mock(ProcuradorRepository.class),
                mock(MedicamentoRepository.class), mock(ConsultaDeCadUnico.class),
                mock(ServicoDeNotificacao.class), mock(ServicoDeOferta.class),
                new PropriedadesDoMedShare(
                        new PropriedadesDoMedShare.Jwt("segredo-de-teste-com-tamanho-suficiente", 120, 14),
                        new PropriedadesDoMedShare.Doacao(30, new BigDecimal("150.00")),
                        new PropriedadesDoMedShare.Reserva(120),
                        new PropriedadesDoMedShare.CadUnico(12)),
                mock(ServicoDeUsuario.class), eventos);

        Usuario beneficiaria = FabricaDeTestes.usuario(1L, "Joana Silva", Papel.BENEFICIARIO);
        Medicamento humira = FabricaDeTestes.medicamento(9L, "Humira", "4210.55");
        pedido = new Necessidade(beneficiaria, humira);
        // criadaEm e preenchido pelo banco; aqui ele vem por reflexao.
        ReflectionTestUtils.setField(pedido, "criadaEm", OffsetDateTime.now().minusDays(23));
    }

    @Test
    @DisplayName("Conta quem espera, quem chegou depois e ha quantos dias")
    void numerosDaEspera() {
        when(necessidades.contarNaFila(anyString())).thenReturn(12L);
        when(necessidades.contarEntrantesApos(anyString(), any())).thenReturn(8L);
        when(eventos.disponibilizacoesDesde(anyString(), any())).thenReturn(List.of(
                OffsetDateTime.now().minusDays(9),
                OffsetDateTime.now().minusDays(21)));

        RaioXDaFila raioX = servico.raioXDaFila(pedido);

        assertThat(raioX.pessoasNaFila()).isEqualTo(12);
        assertThat(raioX.entraramDepoisDeVoce()).isEqualTo(8);
        assertThat(raioX.diasEsperando()).isEqualTo(23);
        assertThat(raioX.caixasRecentes()).isEqualTo(2);
        assertThat(raioX.diasDesdeAUltima()).isEqualTo(9);
        assertThat(raioX.diasDaJanela()).isEqualTo(30);
    }

    @Test
    @DisplayName("Nenhuma caixa na janela: a tela precisa saber disso sem dar erro")
    void semCaixaNenhuma() {
        when(necessidades.contarNaFila(anyString())).thenReturn(4L);
        when(necessidades.contarEntrantesApos(anyString(), any())).thenReturn(0L);
        when(eventos.disponibilizacoesDesde(anyString(), any())).thenReturn(List.of());

        RaioXDaFila raioX = servico.raioXDaFila(pedido);

        assertThat(raioX.caixasRecentes()).isZero();
        assertThat(raioX.diasDesdeAUltima()).isNull();
    }

    @Test
    @DisplayName("A prioridade do UC07 A2 chega na tela")
    void prioridadeAparece() {
        when(necessidades.contarNaFila(anyString())).thenReturn(1L);
        when(eventos.disponibilizacoesDesde(anyString(), any())).thenReturn(List.of());
        pedido.priorizar();

        assertThat(servico.raioXDaFila(pedido).prioridade()).isTrue();
    }

    @Test
    @DisplayName("O raio-x nao tem posicao na fila, e nao pode ganhar uma por acidente")
    void naoExistePosicaoNaFila() {
        List<String> campos = java.util.Arrays.stream(RaioXDaFila.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .toList();

        assertThat(campos)
                .withFailMessage("""
                        Alguem acrescentou %s ao raio-x. Posicao na fila nao existe neste
                        sistema: a ordem depende da farmacia onde a caixa aparece, entao o
                        numero mudaria sozinho. Se a regra do matching mudou e a posicao
                        passou a ser estavel, apague este teste de proposito.""", campos)
                .noneMatch(nome -> nome.toLowerCase().contains("posicao")
                        || nome.toLowerCase().contains("lugar"));
    }

    @Test
    @DisplayName("Nada no raio-x identifica outra pessoa (RN05)")
    void nadaIdentificaNinguem() {
        for (Field campo : RaioXDaFila.class.getDeclaredFields()) {
            assertThat(campo.getType())
                    .withFailMessage("O campo %s e do tipo %s. O raio-x so pode levar"
                            + " numeros e datas — qualquer objeto abre porta para vazar"
                            + " nome de quem esta na fila.", campo.getName(), campo.getType())
                    .isIn(long.class, int.class, boolean.class, Long.class, Integer.class);
        }
    }
}
