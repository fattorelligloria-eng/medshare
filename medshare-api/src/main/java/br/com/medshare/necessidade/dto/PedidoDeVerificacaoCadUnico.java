package br.com.medshare.necessidade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PedidoDeVerificacaoCadUnico(
        @NotBlank @Pattern(regexp = "\\d{11}", message = "o NIS tem 11 dígitos") String nis
) { }
