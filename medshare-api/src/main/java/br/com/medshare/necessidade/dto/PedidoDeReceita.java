package br.com.medshare.necessidade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PedidoDeReceita(
        @NotBlank String fotoUrl,
        @NotNull LocalDate dataEmissao,
        @NotNull LocalDate validade,
        @NotBlank String crmMedico,
        @NotBlank String ufCrm
) { }
