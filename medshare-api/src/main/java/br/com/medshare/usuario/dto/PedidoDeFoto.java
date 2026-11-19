package br.com.medshare.usuario.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** A URL que o /api/fotos devolveu para a foto escolhida. */
public record PedidoDeFoto(
        @NotBlank(message = "Envie a foto antes de escolher")
        @Size(max = 300)
        String url
) { }
