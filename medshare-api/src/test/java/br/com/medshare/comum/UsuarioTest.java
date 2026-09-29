package br.com.medshare.comum;

import br.com.medshare.usuario.Papel;
import br.com.medshare.usuario.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Usuario")
class UsuarioTest {

    @Test
    @DisplayName("abrevia o nome para o painel nao expor a pessoa inteira")
    void abreviaONome() {
        assertThat(FabricaDeTestes.usuario(1L, "Maria Aparecida da Silva").nomeAbreviado())
                .isEqualTo("Maria A. S.");
    }

    @Test
    @DisplayName("nome de uma palavra so fica como esta")
    void nomeSimples() {
        assertThat(FabricaDeTestes.usuario(1L, "Ana").nomeAbreviado()).isEqualTo("Ana");
    }

    @Test
    @DisplayName("aceita acumular papeis: quem doa hoje pode precisar amanha")
    void acumulaPapeis() {
        Usuario usuario = FabricaDeTestes.usuario(1L, "Joana", Papel.DOADOR);
        usuario.adicionarPapel(Papel.BENEFICIARIO);

        assertThat(usuario.temPapel(Papel.DOADOR)).isTrue();
        assertThat(usuario.temPapel(Papel.BENEFICIARIO)).isTrue();
        assertThat(usuario.temPapel(Papel.ADMIN)).isFalse();
    }

    @Test
    @DisplayName("os papeis saem no formato que o Spring Security espera")
    void formatoDaAutoridade() {
        assertThat(Papel.FARMACEUTICO.comoAutoridade()).isEqualTo("ROLE_FARMACEUTICO");
    }

    @Test
    @DisplayName("a lista de papeis devolvida nao deixa ser alterada por fora")
    void papeisSaoImutaveis() {
        Usuario usuario = FabricaDeTestes.usuario(1L, "Joana", Papel.DOADOR);

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> usuario.getPapeis().add(Papel.ADMIN))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("aceita usuario sem papel definido sem estourar")
    void aceitaConjuntoVazio() {
        Usuario usuario = new Usuario("Sem Papel", "11122233344", "x@y.com", "hash",
                null, FabricaDeTestes.endereco(null, null), Set.of());

        assertThat(usuario.getPapeis()).isEmpty();
    }
}
