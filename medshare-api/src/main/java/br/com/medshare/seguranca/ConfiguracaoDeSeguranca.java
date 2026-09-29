package br.com.medshare.seguranca;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Quem pode acessar o que.
 *
 * A lista abaixo e curta de proposito: tudo que nao esta explicitamente aberto
 * exige autenticacao. Assim, um endpoint novo nasce protegido — o esquecimento
 * erra para o lado seguro.
 */
@Configuration
@EnableMethodSecurity
public class ConfiguracaoDeSeguranca {

    private final FiltroDeAutenticacaoJwt filtroJwt;

    public ConfiguracaoDeSeguranca(FiltroDeAutenticacaoJwt filtroJwt) {
        this.filtroJwt = filtroJwt;
    }

    @Bean
    public SecurityFilterChain corrente(HttpSecurity http) throws Exception {
        http
            // Sem CSRF porque a API nao usa cookie de sessao: o token vai no
            // cabecalho, que o navegador nao envia sozinho entre sites.
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(origensPermitidas()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(rotas -> rotas
                    .requestMatchers("/api/autenticacao/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/municipios").permitAll()
                    .requestMatchers(HttpMethod.GET, "/fotos-locais/**").permitAll()
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                    .anyRequest().authenticated())
            .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder codificadorDeSenha() {
        // BCrypt com fator 12: mais lento que o padrao 10 de proposito, para
        // encarecer tentativa de forca bruta caso o banco vaze.
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager gerenciadorDeAutenticacao(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource origensPermitidas() {
        CorsConfiguration configuracao = new CorsConfiguration();
        // O painel React em desenvolvimento. Em producao entra o dominio real.
        configuracao.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:3000"));
        configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracao.setAllowedHeaders(List.of("*"));
        configuracao.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/api/**", configuracao);
        return fonte;
    }
}
