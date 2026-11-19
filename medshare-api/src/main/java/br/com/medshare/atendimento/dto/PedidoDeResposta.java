package br.com.medshare.atendimento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PedidoDeResposta(
        @NotBlank(message = "Escreva a resposta")
        @Size(max = 2000, message = "A resposta pode ter até 2000 letras")
        String texto
) { }
