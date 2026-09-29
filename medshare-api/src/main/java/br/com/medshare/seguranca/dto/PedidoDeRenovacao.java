package br.com.medshare.seguranca.dto;

import jakarta.validation.constraints.NotBlank;

public record PedidoDeRenovacao(@NotBlank String tokenDeRenovacao) { }
