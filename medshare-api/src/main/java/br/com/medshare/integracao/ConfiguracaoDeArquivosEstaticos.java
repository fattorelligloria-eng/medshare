package br.com.medshare.integracao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Serve as fotos gravadas localmente.
 *
 * Nao ha listagem de diretorio nem caminho previsivel: o nome de cada arquivo e
 * um UUID sorteado, entao so chega na foto quem recebeu a URL pela API. Com o
 * Cloudflare R2 configurado, as fotos saem daqui e passam a ter URL assinada
 * com prazo — o certo para a foto de receita, que e dado de saude.
 */
@Configuration
public class ConfiguracaoDeArquivosEstaticos implements WebMvcConfigurer {

    private final String diretorio;

    public ConfiguracaoDeArquivosEstaticos(@Value("${medshare.fotos.diretorio}") String diretorio) {
        this.diretorio = diretorio;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registro) {
        String caminho = Path.of(diretorio).toAbsolutePath().normalize().toUri().toString();
        registro.addResourceHandler("/fotos-locais/**")
                .addResourceLocations(caminho)
                .setCachePeriod(3600);
    }
}
