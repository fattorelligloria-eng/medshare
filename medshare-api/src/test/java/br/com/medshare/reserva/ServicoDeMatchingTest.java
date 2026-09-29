package br.com.medshare.reserva;

import br.com.medshare.comum.PropriedadesDoMedShare;
import br.com.medshare.doacao.Doacao;
import br.com.medshare.doacao.DoacaoRepository;
import br.com.medshare.farmacia.PontoDeColeta;
import br.com.medshare.medicamento.Medicamento;
import br.com.medshare.necessidade.Necessidade;
import br.com.medshare.usuario.Papel;
import br.com.medshare.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static br.com.medshare.comum.FabricaDeTestes.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * O criterio de escolha da caixa: primeiro a que vence antes, depois a mais
 * perto. Estes testes existem porque essa ordem e uma decisao de produto, nao
 * um detalhe — inverte-la significaria mandar gente atravessar a cidade ou
 * deixar medicamento vencer na prateleira.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Escolha da doacao para uma necessidade")
class ServicoDeMatchingTest {

    // Se (centro de SP) e Guarulhos: cerca de 15 km em linha reta.
    private static final double[] SE = {-23.5505, -46.6339};
    private static final double[] GUARULHOS = {-23.4543, -46.5337};

    @Mock
    private DoacaoRepository doacoes;

    private ServicoDeMatching matching;
    private Medicamento medicamento;
    private Usuario beneficiarioNoCentro;

    @BeforeEach
    void preparar() {
        var propriedades = new PropriedadesDoMedShare(
                new PropriedadesDoMedShare.Jwt("segredo", 120, 14),
                new PropriedadesDoMedShare.Doacao(30, new BigDecimal("150.00")),
                new PropriedadesDoMedShare.Reserva(72),
                new PropriedadesDoMedShare.CadUnico(12));
        matching = new ServicoDeMatching(doacoes, propriedades);
        medicamento = medicamento(1L, "Alecensa", "35000.00");
        beneficiarioNoCentro = usuario(1L, "Bruno", Papel.BENEFICIARIO);
    }

    private Doacao doacaoDisponivel(String codigo, int diasDeValidade, PontoDeColeta ponto) {
        Usuario doador = usuario(9L, "Ana", Papel.DOADOR);
        Doacao doacao = Doacao.cadastrar(codigo, doador, medicamento, "L1",
                LocalDate.now().plusDays(diasDeValidade), "http://x/f.jpg",
                PISO_DE_PRECO, DIAS_MINIMOS);
        doacao.preValidar(doador);
        doacao.agendar(ponto, OffsetDateTime.now().plusDays(1), doador);
        doacao.receber(doador);
        doacao.validar(doador);
        doacao.disponibilizar(doador);
        return doacao;
    }

    @Test
    @DisplayName("sem estoque, nao devolve nada")
    void semEstoque() {
        when(doacoes.disponiveisDoPrincipioAtivo(eq("principio de Alecensa"), any())).thenReturn(List.of());

        assertThat(matching.melhorDoacaoPara(new Necessidade(beneficiarioNoCentro, medicamento)))
                .isEmpty();
    }

    @Test
    @DisplayName("entre validades bem diferentes, escolhe a que vence antes")
    void venceAntesSaiAntes() {
        PontoDeColeta longe = pontoDeColeta(2L, "Guarulhos", GUARULHOS[0], GUARULHOS[1]);
        PontoDeColeta perto = pontoDeColeta(1L, "Se", SE[0], SE[1]);

        // O repositorio ja devolve ordenado por validade.
        when(doacoes.disponiveisDoPrincipioAtivo(eq("principio de Alecensa"), any())).thenReturn(List.of(
                doacaoDisponivel("MS-VENCE-ANTES", 40, longe),
                doacaoDisponivel("MS-VENCE-DEPOIS", 300, perto)));

        assertThat(matching.melhorDoacaoPara(new Necessidade(beneficiarioNoCentro, medicamento)))
                .get()
                .extracting(Doacao::getCodigo)
                .isEqualTo("MS-VENCE-ANTES");
    }

    @Test
    @DisplayName("com validades parecidas, escolhe a farmacia mais perto")
    void validadesParecidasDesempatamPelaDistancia() {
        PontoDeColeta longe = pontoDeColeta(2L, "Guarulhos", GUARULHOS[0], GUARULHOS[1]);
        PontoDeColeta perto = pontoDeColeta(1L, "Se", SE[0], SE[1]);

        when(doacoes.disponiveisDoPrincipioAtivo(eq("principio de Alecensa"), any())).thenReturn(List.of(
                doacaoDisponivel("MS-LONGE", 100, longe),
                doacaoDisponivel("MS-PERTO", 110, perto)));

        assertThat(matching.melhorDoacaoPara(new Necessidade(beneficiarioNoCentro, medicamento)))
                .get()
                .extracting(Doacao::getCodigo)
                .isEqualTo("MS-PERTO");
    }

    @Test
    @DisplayName("beneficiario sem coordenadas ainda recebe uma indicacao")
    void semCoordenadasAindaFunciona() {
        Usuario semCoordenadas = usuario(2L, "Sem GPS", Papel.BENEFICIARIO);
        ReflectionTestUtils.setField(semCoordenadas, "endereco", endereco(null, null));

        PontoDeColeta ponto = pontoDeColeta(1L, "Se", SE[0], SE[1]);
        when(doacoes.disponiveisDoPrincipioAtivo(eq("principio de Alecensa"), any()))
                .thenReturn(List.of(doacaoDisponivel("MS-UNICA", 100, ponto)));

        assertThat(matching.melhorDoacaoPara(new Necessidade(semCoordenadas, medicamento)))
                .isPresent();
    }

    // --- pedido para uma caixa (UC05) ------------------------------------------

    private Necessidade pedidoCom(Long id, Usuario quem, OffsetDateTime criadoEm) {
        Necessidade necessidade = new Necessidade(quem, medicamento);
        ReflectionTestUtils.setField(necessidade, "id", id);
        ReflectionTestUtils.setField(necessidade, "criadaEm", criadoEm);
        necessidade.anexarReceita(new br.com.medshare.necessidade.Receita(necessidade, "http://x/r.jpg",
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(60), "12345", "SP"));
        return necessidade;
    }

    private Usuario beneficiarioEm(Long id, double[] onde) {
        Usuario usuario = usuario(id, "Pessoa " + id, Papel.BENEFICIARIO);
        ReflectionTestUtils.setField(usuario, "endereco", endereco(onde[0], onde[1]));
        return usuario;
    }

    @Test
    @DisplayName("UC05: na mesma faixa de distancia, quem pediu antes recebe a oferta")
    void antiguidadeDentroDaFaixa() {
        var caixa = doacaoDisponivel("MS-1", 100, pontoDeColeta(1L, "Se", SE[0], SE[1]));
        var antigo = pedidoCom(10L, beneficiarioEm(10L, SE), OffsetDateTime.now().minusDays(30));
        var novo = pedidoCom(11L, beneficiarioEm(11L, SE), OffsetDateTime.now().minusDays(1));

        assertThat(matching.melhorNecessidadePara(caixa, List.of(novo, antigo), n -> true))
                .get().extracting(Necessidade::getId).isEqualTo(10L);
    }

    @Test
    @DisplayName("UC05: quem mora bem mais perto da farmacia passa na frente")
    void distanciaAntesDaAntiguidade() {
        var caixa = doacaoDisponivel("MS-1", 100, pontoDeColeta(1L, "Se", SE[0], SE[1]));
        var antigoLonge = pedidoCom(10L, beneficiarioEm(10L, GUARULHOS), OffsetDateTime.now().minusDays(30));
        var novoPerto = pedidoCom(11L, beneficiarioEm(11L, SE), OffsetDateTime.now().minusDays(1));

        assertThat(matching.melhorNecessidadePara(caixa, List.of(antigoLonge, novoPerto), n -> true))
                .get().extracting(Necessidade::getId).isEqualTo(11L);
    }

    @Test
    @DisplayName("UC07 A2: quem perdeu uma caixa por validade tem prioridade")
    void prioridadePrimeiro() {
        var caixa = doacaoDisponivel("MS-1", 100, pontoDeColeta(1L, "Se", SE[0], SE[1]));
        var perto = pedidoCom(10L, beneficiarioEm(10L, SE), OffsetDateTime.now().minusDays(30));
        var prioritarioLonge = pedidoCom(11L, beneficiarioEm(11L, GUARULHOS), OffsetDateTime.now());
        prioritarioLonge.priorizar();

        assertThat(matching.melhorNecessidadePara(caixa, List.of(perto, prioritarioLonge), n -> true))
                .get().extracting(Necessidade::getId).isEqualTo(11L);
    }

    @Test
    @DisplayName("pedido sem receita valida, em revisao ou sem CadUnico nao recebe oferta")
    void soAptosRecebem() {
        var caixa = doacaoDisponivel("MS-1", 100, pontoDeColeta(1L, "Se", SE[0], SE[1]));
        var semReceita = new Necessidade(beneficiarioEm(10L, SE), medicamento);
        var emRevisao = pedidoCom(11L, beneficiarioEm(11L, SE), OffsetDateTime.now());
        emRevisao.marcarParaRevisao("receita de outro principio ativo");
        var semCadUnico = pedidoCom(12L, beneficiarioEm(12L, SE), OffsetDateTime.now());

        assertThat(matching.melhorNecessidadePara(caixa, List.of(semReceita, emRevisao, semCadUnico),
                n -> !n.getId().equals(12L))).isEmpty();
    }
}
