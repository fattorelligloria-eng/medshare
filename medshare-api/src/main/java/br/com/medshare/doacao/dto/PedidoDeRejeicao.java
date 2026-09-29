package br.com.medshare.doacao.dto;

import jakarta.validation.constraints.NotBlank;

public record PedidoDeRejeicao(
        boolean lacreIntegro,
        boolean dadosConferem,
        @NotBlank(message = "descreva o motivo da rejeição") String motivo
) { }
