package br.com.medshare.necessidade;

import br.com.medshare.comum.FabricaDeTestes;
import br.com.medshare.comum.PropriedadesDoMedShare;
import br.com.medshare.comum.RegraDeNegocioViolada;
import br.com.medshare.integracao.ConsultaDeCadUnico;
import br.com.medshare.integracao.ConsultaDeCadUnico.ResultadoDaConsulta;
import br.com.medshare.medicamento.MedicamentoRepository;
import br.com.medshare.notificacao.ServicoDeNotificacao;
import br.com.medshare.reserva.ServicoDeOferta;
import br.com.medshare.usuario.Papel;
import br.com.medshare.usuario.ServicoDeUsuario;
import br.com.medshare.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * UC04 / RN08 — virar beneficiario é consequência de confirmar o NIS, nunca de
 * pedir.
 *
 * Decisao da Gloria: quem nao tem CadUnico nao ve Pedidos nem Reservas, nem
 * sabe que existem. Como as telas sao mostradas pelo papel, o papel e que
 * precisa nao nascer antes da hora — e e isto que estes testes travam. Se um
 * dia alguem conceder o papel mais cedo "para a tela abrir", eles quebram.
 */
class PapelPelaConfirmacaoDoNisTest {

    private ConsultaDeCadUnico consulta;
    private VerificacaoCadUnicoRepository verificacoes;
    private ServicoDeUsuario usuarios;
    private ServicoDeNecessidade servico;

    private final Usuario doador = FabricaDeTestes.usuario(1L, "Gloria Fattorelli", Papel.DOADOR);

    @BeforeEach
    void preparar() {
        consulta = mock(ConsultaDeCadUnico.class);
        verificacoes = mock(VerificacaoCadUnicoRepository.class);
        usuarios = mock(ServicoDeUsuario.class);
        when(consulta.fonte()).thenReturn("SIMULADO");

        servico = new ServicoDeNecessidade(
                mock(NecessidadeRepository.class), mock(ReceitaRepository.class),
                verificacoes, mock(ProcuradorRepository.class),
                mock(MedicamentoRepository.class), consulta,
                mock(ServicoDeNotificacao.class), mock(ServicoDeOferta.class),
                new PropriedadesDoMedShare(
                        new PropriedadesDoMedShare.Jwt("segredo-de-teste-com-tamanho-suficiente", 120, 14),
                        new PropriedadesDoMedShare.Doacao(30, new BigDecimal("150.00")),
                        new PropriedadesDoMedShare.Reserva(120),
                        new PropriedadesDoMedShare.CadUnico(12)),
                usuarios);
    }

    @Test
    @DisplayName("NIS confirmado: o doador vira beneficiario")
    void confirmadoConcedeOPapel() {
        when(consulta.consultar("12345678901"))
                .thenReturn(ResultadoDaConsulta.confirmado("Encontrado"));

        servico.verificarCadUnico("12345678901", doador);

        verify(usuarios).tornarBeneficiario(doador);
    }

    @Test
    @DisplayName("NIS nao encontrado: segue so doador, e as telas continuam invisiveis")
    void naoEncontradoNaoConcedeNada() {
        when(consulta.consultar("12345678901"))
                .thenReturn(ResultadoDaConsulta.naoEncontrado("Nao encontrado"));

        servico.verificarCadUnico("12345678901", doador);

        verify(usuarios, never()).tornarBeneficiario(any());
    }

    @Test
    @DisplayName("Portal fora do ar: fica pendente, sem papel")
    void portalIndisponivelNaoConcedeNada() {
        when(consulta.consultar("12345678901"))
                .thenReturn(ResultadoDaConsulta.indisponivel("Portal fora do ar"));

        servico.verificarCadUnico("12345678901", doador);

        verify(usuarios, never()).tornarBeneficiario(any());
    }

    @Test
    @DisplayName("NIS mal formado: recusa antes de qualquer coisa e nao grava nada")
    void formatoInvalidoNaoConcedeNada() {
        when(consulta.consultar("123"))
                .thenReturn(ResultadoDaConsulta.formatoInvalido("O NIS tem 11 digitos"));

        assertThatThrownBy(() -> servico.verificarCadUnico("123", doador))
                .isInstanceOf(RegraDeNegocioViolada.class);

        verify(usuarios, never()).tornarBeneficiario(any());
        verify(verificacoes, never()).save(any());
    }

    @Test
    @DisplayName("Nenhum caminho concede papel a quem so consultou, seja qual for a resposta")
    void nenhumaRespostaNegativaConcedePapel() {
        for (ResultadoDaConsulta r : new ResultadoDaConsulta[]{
                ResultadoDaConsulta.naoEncontrado("x"),
                ResultadoDaConsulta.indisponivel("y")}) {
            reset(usuarios);
            when(consulta.consultar(anyString())).thenReturn(r);

            servico.verificarCadUnico("12345678901", doador);

            verify(usuarios, never()).tornarBeneficiario(any());
        }
    }
}
