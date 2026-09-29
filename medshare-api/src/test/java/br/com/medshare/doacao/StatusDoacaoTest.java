package br.com.medshare.doacao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Maquina de estados da doacao")
class StatusDoacaoTest {

    @Test
    @DisplayName("os estados finais sao exatamente os cinco previstos no diagrama")
    void estadosFinais() {
        assertThat(java.util.Arrays.stream(StatusDoacao.values())
                .filter(StatusDoacao::ehFinal)
                .toList())
                .containsExactlyInAnyOrder(
                        StatusDoacao.ENTREGUE, StatusDoacao.RECUSADA, StatusDoacao.CANCELADA,
                        StatusDoacao.REJEITADA, StatusDoacao.DESCARTADA);
    }

    @ParameterizedTest
    @EnumSource(StatusDoacao.class)
    @DisplayName("nenhum estado aponta para si mesmo")
    void semTransicaoCircular(StatusDoacao status) {
        assertThat(status.podeIrPara(status)).isFalse();
    }

    @Test
    @DisplayName("todo estado nao final leva a pelo menos um caminho")
    void todoEstadoIntermediarioTemSaida() {
        for (StatusDoacao status : StatusDoacao.values()) {
            if (!status.ehFinal()) {
                assertThat(status.proximosPossiveis())
                        .as("estado %s", status)
                        .isNotEmpty();
            }
        }
    }

    @Test
    @DisplayName("RN05: so ha custodia da farmacia depois do recebimento")
    void custodiaComecaNoRecebimento() {
        assertThat(StatusDoacao.CADASTRADA.estaSobCustodiaDaFarmacia()).isFalse();
        assertThat(StatusDoacao.AGENDADA.estaSobCustodiaDaFarmacia()).isFalse();
        assertThat(StatusDoacao.RECEBIDA.estaSobCustodiaDaFarmacia()).isTrue();
        assertThat(StatusDoacao.DISPONIVEL.estaSobCustodiaDaFarmacia()).isTrue();
        assertThat(StatusDoacao.ENTREGUE.estaSobCustodiaDaFarmacia()).isFalse();
    }
}
