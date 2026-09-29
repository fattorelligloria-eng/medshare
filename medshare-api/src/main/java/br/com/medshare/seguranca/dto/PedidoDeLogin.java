package br.com.medshare.seguranca.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PedidoDeLogin(
        @NotBlank @Email String email,
        @NotBlank String senha
) { }
