package br.com.medshare.doacao.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record PedidoDeAgendamento(
        @NotNull Long pontoDeColetaId,
        @NotNull @Future OffsetDateTime dataHora
) { }
