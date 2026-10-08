package br.com.medshare.seguranca;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
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
    public SecurityFilterChain corrente(
            HttpSecurity http,
            // @Qualifier e obrigatorio aqui: o proprio Spring MVC registra um
            // CorsConfigurationSource (o mvcHandlerMappingIntrospector), entao
            // sem dizer qual dos dois a aplicacao nem sobe.
            @Qualifier("origensPermitidas") CorsConfigurationSource origens)
            throws Exception {
        http
            // Sem CSRF porque a API nao usa cookie de sessao: o token vai no
            // cabecalho, que o navegador nao envia sozinho entre sites.
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(origens))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // Sem isto o Spring responde 403 a um token vencido, e o painel e o
            // app nao tinham como distinguir "sessao expirou" de "sem permissao".
            .exceptionHandling(e -> e.authenticationEntryPoint(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(rotas -> rotas
                    .requestMatchers("/api/autenticacao/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/municipios").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/enderecos/*").permitAll()
                    .requestMatchers(HttpMethod.GET, "/fotos-locais/**").permitAll()
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                    // A raiz so redireciona para a documentacao (ControladorDaRaiz).
                    .requestMatchers(HttpMethod.GET, "/").permitAll()
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
    /**
     * Quem pode chamar a API de dentro de um navegador.
     *
     * Antes esta lista era literal — "http://localhost:5173" e mais nada. O
     * efeito colateral aparecia longe da causa: abrir o painel por 127.0.0.1
     * em vez de localhost, ou testar o aplicativo no celular pelo IP da rede,
     * devolvia "Invalid CORS request" na tela de login. A pessoa lia aquilo e
     * ia conferir a senha, que estava certa o tempo todo.
     *
     * O padrao agora cobre a maquina de quem desenvolve e a rede local — que
     * so alcanca quem ja esta nessa mesma rede. Em producao,
     * MEDSHARE_CORS_ORIGENS substitui a lista inteira pelo dominio de verdade,
     * e nenhum desses padroes continua valendo.
     */
    public CorsConfigurationSource origensPermitidas(
            @Value("${medshare.cors.origens}") List<String> origens) {

        CorsConfiguration configuracao = new CorsConfiguration();
        // setAllowedOriginPatterns, e nao setAllowedOrigins, porque so ele
        // entende o "*" da porta e do final do IP.
        configuracao.setAllowedOriginPatterns(origens);
        configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracao.setAllowedHeaders(List.of("*"));
        configuracao.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/api/**", configuracao);
        return fonte;
    }
}
