package br.com.medshare.reserva.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** UC07 A1 - por que a entrega foi negada (vai para o historico e para o beneficiario). */
public record PedidoDeNegativa(
        @NotBlank(message = "explique por que a entrega foi negada")
        @Size(max = 280) String motivo
) { }
