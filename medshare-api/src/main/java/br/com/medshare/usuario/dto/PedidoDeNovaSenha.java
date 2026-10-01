package br.com.medshare.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PedidoDeNovaSenha(
        @NotBlank String senhaAtual,
        @NotBlank @Size(min = 8, max = 72, message = "a senha precisa ter entre 8 e 72 caracteres")
        String novaSenha
) { }
