package br.com.medshare.comum;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * Os numeros das regras de negocio ficam no application.yml, nao espalhados
 * como literais pelo codigo. Se a CMED mudar o piso de R$ 150, muda-se uma
 * linha de configuracao — nao se cacam "150" por dez arquivos.
 */
@ConfigurationProperties(prefix = "medshare")
public record PropriedadesDoMedShare(Jwt jwt, Doacao doacao, Reserva reserva, CadUnico cadunico) {

    public record Jwt(String segredo, int minutosDeValidade, int diasDeValidadeDoRefresh) { }

    /** RN02 e RN07. */
    public record Doacao(int diasMinimosDeValidade, BigDecimal valorMinimoPmc) { }

    public record Reserva(int horasParaRetirada) { }

    /** RN08. */
    public record CadUnico(int mesesDeValidade) { }
}
