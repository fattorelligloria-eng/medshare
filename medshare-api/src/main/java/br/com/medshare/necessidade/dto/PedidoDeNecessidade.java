package br.com.medshare.necessidade.dto;

import jakarta.validation.constraints.NotNull;

public record PedidoDeNecessidade(@NotNull Long medicamentoId) { }
