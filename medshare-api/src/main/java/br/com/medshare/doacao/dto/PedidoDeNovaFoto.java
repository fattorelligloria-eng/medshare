package br.com.medshare.doacao.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** UC10 A2 - a foto nova que a central pediu. */
public record PedidoDeNovaFoto(@NotBlank @Size(max = 500) String fotoUrl) { }
