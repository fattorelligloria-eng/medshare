package br.com.medshare.seguranca;

import br.com.medshare.comum.RegraDeNegocioViolada;
import br.com.medshare.usuario.Usuario;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Quem esta fazendo a requisicao agora.
 *
 * Centralizado aqui para que nenhum service precise mexer em
 * SecurityContextHolder — se um dia a forma de identificar o usuario mudar,
 * muda so este arquivo.
 */
@Component
public class UsuarioLogado {

    public Usuario obrigatorio() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao == null || !(autenticacao.getPrincipal() instanceof UsuarioAutenticado dono)) {
            throw new RegraDeNegocioViolada("AUTENTICACAO",
                    "É preciso estar autenticado para esta operação");
        }
        return dono.getUsuario();
    }
}
