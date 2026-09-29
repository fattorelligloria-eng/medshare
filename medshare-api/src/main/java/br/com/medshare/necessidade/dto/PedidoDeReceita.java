package br.com.medshare.necessidade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PedidoDeReceita(
        @NotBlank @Size(max = 500) String fotoUrl,
        @NotNull LocalDate dataEmissao,
        @NotNull LocalDate validade,
        @NotBlank @Size(max = 20) String crmMedico,
        @NotBlank @Pattern(regexp = "[A-Za-z]{2}", message = "informe a UF com 2 letras") String ufCrm
) { }
