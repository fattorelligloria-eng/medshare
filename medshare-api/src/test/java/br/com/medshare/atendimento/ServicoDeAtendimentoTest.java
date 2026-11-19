package br.com.medshare.atendimento;

import br.com.medshare.comum.FabricaDeTestes;
import br.com.medshare.comum.RegraDeNegocioViolada;
import br.com.medshare.notificacao.ServicoDeNotificacao;
import br.com.medshare.notificacao.TipoNotificacao;
import br.com.medshare.usuario.Papel;
import br.com.medshare.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("Atendimento: mensagens para a central")
class ServicoDeAtendimentoTest {

    private MensagemDeAtendimentoRepository mensagens;
    private ServicoDeNotificacao notificacoes;
    private ServicoDeAtendimento servico;

    private final Usuario ana = FabricaDeTestes.usuario(7L, "Ana Souza", Papel.DOADOR);
    private final Usuario central = FabricaDeTestes.usuario(1L, "Bruno Teixeira Lopes", Papel.ADMIN);

    @BeforeEach
    void preparar() {
        mensagens = mock(MensagemDeAtendimentoRepository.class);
        when(mensagens.save(any())).thenAnswer(chamada -> chamada.getArgument(0));
        notificacoes = mock(ServicoDeNotificacao.class);
        servico = new ServicoDeAtendimento(mensagens, notificacoes);
    }

    @Test
    @DisplayName("guarda a mensagem sem os espacos sobrando")
    void envia() {
        MensagemDeAtendimento enviada = servico.enviar(ana, AssuntoDaMensagem.DUVIDA, "  Como doo insulina?  ");

        assertThat(enviada.getTexto()).isEqualTo("Como doo insulina?");
        assertThat(enviada.foiRespondida()).isFalse();
    }

    @Test
    @DisplayName("segura quem ja tem mensagens demais esperando resposta")
    void limiteSemResposta() {
        when(mensagens.countByUsuarioIdAndRespostaIsNull(7L)).thenReturn((long) ServicoDeAtendimento.LIMITE_SEM_RESPOSTA);

        assertThatThrownBy(() -> servico.enviar(ana, AssuntoDaMensagem.OUTRO, "Oi de novo"))
                .isInstanceOf(RegraDeNegocioViolada.class)
                .hasMessageContaining("esperando resposta");
        verify(mensagens, never()).save(any());
    }

    @Test
    @DisplayName("a resposta avisa a pessoa sem colocar o texto na notificacao")
    void respostaAvisaSemExpor() {
        MensagemDeAtendimento mensagem = new MensagemDeAtendimento(ana, AssuntoDaMensagem.PROBLEMA_COM_CAIXA, "Meu código não abriu");
        when(mensagens.findById(10L)).thenReturn(Optional.of(mensagem));

        servico.responder(10L, "Já liberamos um código novo para a sua receita de insulina.", central);

        assertThat(mensagem.getResposta()).startsWith("Já liberamos");
        ArgumentCaptor<String> corpo = ArgumentCaptor.forClass(String.class);
        verify(notificacoes).avisar(eq(ana), eq(TipoNotificacao.ATENDIMENTO_RESPONDIDO), any(), corpo.capture());
        assertThat(corpo.getValue()).doesNotContain("insulina");
    }

    @Test
    @DisplayName("nao responde duas vezes a mesma mensagem")
    void respostaUnica() {
        MensagemDeAtendimento mensagem = new MensagemDeAtendimento(ana, AssuntoDaMensagem.DUVIDA, "Pergunta");
        when(mensagens.findById(10L)).thenReturn(Optional.of(mensagem));
        servico.responder(10L, "Primeira resposta", central);

        assertThatThrownBy(() -> servico.responder(10L, "Segunda resposta", central))
                .isInstanceOf(RegraDeNegocioViolada.class);
        assertThat(mensagem.getResposta()).isEqualTo("Primeira resposta");
    }
}
