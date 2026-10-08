package br.com.medshare.usuario;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tokens antigos caem quando a senha muda")
class TokenAposTrocaDeSenhaTest {

    private static Usuario ana() {
        return new Usuario("Ana Doadora", "11122233344", "ana@medshare.test", "hash",
                null, null, EnumSet.of(Papel.DOADOR));
    }

    @Test
    @DisplayName("quem nunca trocou a senha aceita qualquer token")
    void semTroca() {
        assertThat(ana().aceitaTokenEmitidoEm(Instant.now().minus(10, ChronoUnit.DAYS))).isTrue();
    }

    @Test
    @DisplayName("token de antes da troca deixa de valer")
    void tokenAntigo() {
        Usuario ana = ana();
        Instant ontem = Instant.now().minus(1, ChronoUnit.DAYS);
        ana.trocarSenha("hash-novo");
        assertThat(ana.aceitaTokenEmitidoEm(ontem)).isFalse();
    }

    @Test
    @DisplayName("token emitido logo depois da troca, no mesmo segundo, continua valendo")
    void tokenNovo() {
        Usuario ana = ana();
        ana.trocarSenha("hash-novo");
        // O "emitido em" do JWT e em segundos inteiros.
        Instant emitidoAgora = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        assertThat(ana.aceitaTokenEmitidoEm(emitidoAgora)).isTrue();
    }
}
