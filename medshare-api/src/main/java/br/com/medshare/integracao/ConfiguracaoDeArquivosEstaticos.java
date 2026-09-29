package br.com.medshare.integracao;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Serve as fotos gravadas localmente.
 *
 * Nao ha listagem de diretorio nem caminho previsivel: o nome de cada arquivo e
 * um UUID sorteado, entao so chega na foto quem recebeu a URL pela API.
 */
@Configuration
public class ConfiguracaoDeArquivosEstaticos implements WebMvcConfigurer {

    private final RepositorioDeFotos fotos;

    public ConfiguracaoDeArquivosEstaticos(RepositorioDeFotos fotos) {
        this.fotos = fotos;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registro) {
        registro.addResourceHandler("/fotos-locais/**")
                .addResourceLocations(fotos.diretorio().toUri().toString())
                .setCachePeriod(3600);
    }
}
