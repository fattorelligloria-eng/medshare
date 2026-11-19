package br.com.medshare.usuario;

import br.com.medshare.comum.FabricaDeTestes;
import br.com.medshare.comum.RegraDeNegocioViolada;
import br.com.medshare.doacao.DoacaoRepository;
import br.com.medshare.integracao.RepositorioDeFotos;
import br.com.medshare.necessidade.VerificacaoCadUnico;
import br.com.medshare.necessidade.VerificacaoCadUnicoRepository;
import br.com.medshare.usuario.dto.MeusDados;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Foto de perfil e carteirinha digital.
 *
 * O que estes testes travam: a foto so pode ser uma das nossas e sai sempre
 * como link assinado; o numero da carteirinha e sorteado, nao e o id; e a
 * validade do CadUnico so aparece enquanto vale.
 */
@DisplayName("Foto de perfil e carteirinha")
class FotoECarteirinhaTest {

    @TempDir
    Path pastaDeFotos;

    private RepositorioDeFotos fotos;
    private UsuarioRepository usuarios;
    private VerificacaoCadUnicoRepository verificacoes;
    private ServicoDeUsuario servico;

    @BeforeEach
    void preparar() {
        fotos = new RepositorioDeFotos(pastaDeFotos.toString(), "http://localhost:8080/fotos-locais", "segredo-de-teste");
        usuarios = mock(UsuarioRepository.class);
        when(usuarios.save(any())).thenAnswer(chamada -> chamada.getArgument(0));
        verificacoes = mock(VerificacaoCadUnicoRepository.class);
        when(verificacoes.findFirstByUsuarioIdOrderByValidoAteDesc(any())).thenReturn(Optional.empty());
        servico = new ServicoDeUsuario(usuarios, mock(ServicoDeEndereco.class), mock(DoacaoRepository.class),
                mock(PasswordEncoder.class), fotos, verificacoes);
    }

    private String fotoNossa() throws Exception {
        String nome = UUID.randomUUID() + ".jpg";
        Files.write(pastaDeFotos.resolve(nome), new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF });
        return fotos.urlDe(nome);
    }

    @Test
    @DisplayName("toda conta nova nasce com numero de carteirinha de 12 digitos, que nao e o id")
    void numeroDaCarteirinha() {
        Usuario ana = FabricaDeTestes.usuario(7L, "Ana Souza", Papel.DOADOR);
        Usuario bia = FabricaDeTestes.usuario(8L, "Bia Lima", Papel.DOADOR);

        assertThat(ana.getNumeroCarteirinha()).matches("[0-9]{12}").isNotEqualTo("000000000007");
        assertThat(ana.getNumeroCarteirinha()).isNotEqualTo(bia.getNumeroCarteirinha());
    }

    @Test
    @DisplayName("o numero da carteirinha nao muda depois que a conta existe")
    void numeroNaoMuda() {
        Usuario ana = FabricaDeTestes.usuario(7L, "Ana Souza", Papel.DOADOR);

        assertThatThrownBy(ana::sortearOutroNumeroDeCarteirinha).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("recusa foto que nao veio do nosso envio de fotos")
    void recusaFotoDeFora() {
        Usuario ana = FabricaDeTestes.usuario(7L, "Ana Souza", Papel.DOADOR);

        assertThatThrownBy(() -> servico.trocarFoto(ana, "https://exemplo.com/rosto.jpg"))
                .isInstanceOf(RegraDeNegocioViolada.class);
        assertThat(ana.getFotoUrl()).isNull();
        verify(usuarios, never()).save(any());
    }

    @Test
    @DisplayName("aceita foto nossa e entrega so o link assinado, nunca o endereco gravado")
    void aceitaFotoNossa() throws Exception {
        Usuario ana = FabricaDeTestes.usuario(7L, "Ana Souza", Papel.DOADOR);
        String url = fotoNossa();

        servico.trocarFoto(ana, url);
        MeusDados dados = servico.meusDados(ana);

        assertThat(ana.getFotoUrl()).isEqualTo(url);
        assertThat(dados.fotoUrl()).startsWith(url + "?expira=").contains("&assinatura=");
    }

    @Test
    @DisplayName("remover a foto volta para o bonequinho")
    void removeFoto() throws Exception {
        Usuario ana = FabricaDeTestes.usuario(7L, "Ana Souza", Papel.DOADOR);
        servico.trocarFoto(ana, fotoNossa());

        servico.removerFoto(ana);

        assertThat(servico.meusDados(ana).fotoUrl()).isNull();
    }

    @Test
    @DisplayName("a validade do CadUnico aparece enquanto vale")
    void cadUnicoVigente() {
        Usuario ana = FabricaDeTestes.usuario(7L, "Ana Souza", Papel.DOADOR, Papel.BENEFICIARIO);
        var verificacao = new VerificacaoCadUnico(ana, "12345678901", true, 12, "TESTE");
        when(verificacoes.findFirstByUsuarioIdOrderByValidoAteDesc(7L)).thenReturn(Optional.of(verificacao));

        assertThat(servico.meusDados(ana).cadUnicoValidoAte()).isEqualTo(LocalDate.now().plusMonths(12));
    }

    @Test
    @DisplayName("CadUnico vencido ou nao confirmado nao vai para a carteirinha")
    void cadUnicoForaDoPrazo() {
        Usuario ana = FabricaDeTestes.usuario(7L, "Ana Souza", Papel.DOADOR, Papel.BENEFICIARIO);
        var vencida = new VerificacaoCadUnico(ana, "12345678901", true, -1, "TESTE");
        when(verificacoes.findFirstByUsuarioIdOrderByValidoAteDesc(7L)).thenReturn(Optional.of(vencida));
        assertThat(servico.meusDados(ana).cadUnicoValidoAte()).isNull();

        var naoConfirmada = new VerificacaoCadUnico(ana, "12345678901", false, 12, "TESTE");
        when(verificacoes.findFirstByUsuarioIdOrderByValidoAteDesc(7L)).thenReturn(Optional.of(naoConfirmada));
        assertThat(servico.meusDados(ana).cadUnicoValidoAte()).isNull();
    }
}
