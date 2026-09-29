package br.com.medshare.seguranca;

import br.com.medshare.usuario.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ServicoDeUsuarioDetalhado implements UserDetailsService {

    private final UsuarioRepository usuarios;

    public ServicoDeUsuarioDetalhado(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return usuarios.findByEmail(email)
                .map(UsuarioAutenticado::new)
                // Mensagem generica de proposito: dizer "este e-mail nao existe"
                // entrega ao atacante quais contas estao cadastradas.
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));
    }
}
