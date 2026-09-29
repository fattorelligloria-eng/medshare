package br.com.medshare;

import br.com.medshare.comum.PropriedadesDoMedShare;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(PropriedadesDoMedShare.class)
@EnableScheduling
public class MedShareApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedShareApplication.class, args);
    }
}
