package br.com.medshare.doacao.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record PedidoDeDoacao(
        @NotNull Long medicamentoId,
        @NotBlank @Size(max = 30) String lote,
        @NotNull @Future(message = "a validade precisa ser uma data futura") LocalDate validade,
        @NotBlank String fotoUrl
) { }
