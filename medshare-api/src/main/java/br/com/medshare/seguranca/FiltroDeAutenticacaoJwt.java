package br.com.medshare.seguranca;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Le o token do cabecalho Authorization e, se ele for valido, coloca o usuario
 * no contexto de seguranca da requisicao.
 *
 * Quando o token falta ou nao presta, o filtro nao devolve erro: ele so deixa
 * a requisicao passar sem autenticacao. Quem decide se aquele endereco exigia
 * login e a configuracao de seguranca — assim a regra de "o que e publico"
 * fica em um lugar so.
 */
@Component
public class FiltroDeAutenticacaoJwt extends OncePerRequestFilter {

    private static final String CABECALHO = "Authorization";
    private static final String PREFIXO = "Bearer ";

    private final ServicoDeToken tokens;
    private final ServicoDeUsuarioDetalhado usuarios;

    public FiltroDeAutenticacaoJwt(ServicoDeToken tokens, ServicoDeUsuarioDetalhado usuarios) {
        this.tokens = tokens;
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest requisicao,
                                    @NonNull HttpServletResponse resposta,
                                    @NonNull FilterChain corrente)
            throws ServletException, IOException {

        extrairToken(requisicao)
                .flatMap(tokens::emailDoTokenDeAcesso)
                .ifPresent(email -> autenticar(email, requisicao));

        corrente.doFilter(requisicao, resposta);
    }

    private java.util.Optional<String> extrairToken(HttpServletRequest requisicao) {
        String cabecalho = requisicao.getHeader(CABECALHO);
        if (cabecalho == null || !cabecalho.startsWith(PREFIXO)) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(cabecalho.substring(PREFIXO.length()).trim());
    }

    private void autenticar(String email, HttpServletRequest requisicao) {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }
        try {
            UserDetails detalhes = usuarios.loadUserByUsername(email);
            var autenticacao = new UsernamePasswordAuthenticationToken(
                    detalhes, null, detalhes.getAuthorities());
            autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(requisicao));
            SecurityContextHolder.getContext().setAuthentication(autenticacao);
        } catch (UsernameNotFoundException e) {
            // Token valido de uma conta que nao existe mais: segue sem autenticar.
            SecurityContextHolder.clearContext();
        }
    }
}
