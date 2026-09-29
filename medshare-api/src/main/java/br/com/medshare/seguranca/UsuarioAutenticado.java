package br.com.medshare.seguranca;

import br.com.medshare.usuario.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adaptador entre o nosso Usuario e o que o Spring Security espera.
 *
 * O Usuario do dominio nao implementa UserDetails de proposito: o dominio nao
 * precisa saber que existe um framework de seguranca. Quando trocarmos de
 * biblioteca, so esta classe muda.
 */
public class UsuarioAutenticado implements UserDetails {

    private final Usuario usuario;

    public UsuarioAutenticado(Usuario usuario) {
        this.usuario = usuario;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Long getId() {
        return usuario.getId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return usuario.getPapeis().stream()
                .map(papel -> new SimpleGrantedAuthority(papel.comoAutoridade()))
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    @Override
    public String getPassword() {
        return usuario.getSenhaHash();
    }

    @Override
    public String getUsername() {
        return usuario.getEmail();
    }

    @Override
    public boolean isEnabled() {
        return usuario.isAtivo();
    }

    @Override
    public boolean isAccountNonExpired() {
        return usuario.isAtivo();
    }

    @Override
    public boolean isAccountNonLocked() {
        return usuario.isAtivo();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    public static List<String> papeisDe(Usuario usuario) {
        return usuario.getPapeis().stream().map(Enum::name).sorted().toList();
    }
}
