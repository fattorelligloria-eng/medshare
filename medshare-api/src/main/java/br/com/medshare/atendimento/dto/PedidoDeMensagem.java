package br.com.medshare.atendimento.dto;

import br.com.medshare.atendimento.AssuntoDaMensagem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PedidoDeMensagem(
        @NotNull(message = "Escolha o assunto")
        AssuntoDaMensagem assunto,

        @NotBlank(message = "Escreva a mensagem")
        @Size(max = 2000, message = "A mensagem pode ter até 2000 letras")
        String texto
) { }
