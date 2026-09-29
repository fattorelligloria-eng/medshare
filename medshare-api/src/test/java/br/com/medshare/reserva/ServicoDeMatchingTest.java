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
        when(doacoes.disponiveisDoMedicamento(eq(1L), any())).thenReturn(List.of());

        assertThat(matching.melhorDoacaoPara(new Necessidade(beneficiarioNoCentro, medicamento)))
                .isEmpty();
    }

    @Test
    @DisplayName("entre validades bem diferentes, escolhe a que vence antes")
    void venceAntesSaiAntes() {
        PontoDeColeta longe = pontoDeColeta(2L, "Guarulhos", GUARULHOS[0], GUARULHOS[1]);
        PontoDeColeta perto = pontoDeColeta(1L, "Se", SE[0], SE[1]);

        // O repositorio ja devolve ordenado por validade.
        when(doacoes.disponiveisDoMedicamento(eq(1L), any())).thenReturn(List.of(
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

        when(doacoes.disponiveisDoMedicamento(eq(1L), any())).thenReturn(List.of(
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
        when(doacoes.disponiveisDoMedicamento(eq(1L), any()))
                .thenReturn(List.of(doacaoDisponivel("MS-UNICA", 100, ponto)));

        assertThat(matching.melhorDoacaoPara(new Necessidade(semCoordenadas, medicamento)))
                .isPresent();
    }
}
