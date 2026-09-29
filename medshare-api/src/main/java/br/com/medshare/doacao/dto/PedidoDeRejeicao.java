package br.com.medshare.doacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** O motivo vai para o historico e para a notificacao, que guardam ate 500 caracteres. */
public record PedidoDeRejeicao(
        boolean lacreIntegro,
        boolean dadosConferem,
        @NotBlank(message = "descreva o motivo da rejeição")
        @Size(max = 400, message = "resuma o motivo em até 400 caracteres") String motivo
) { }
