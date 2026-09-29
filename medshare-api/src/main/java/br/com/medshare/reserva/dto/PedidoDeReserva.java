package br.com.medshare.reserva.dto;

import jakarta.validation.constraints.NotNull;

public record PedidoDeReserva(@NotNull Long necessidadeId) { }
