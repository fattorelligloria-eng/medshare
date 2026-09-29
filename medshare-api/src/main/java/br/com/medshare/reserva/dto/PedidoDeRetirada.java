package br.com.medshare.reserva.dto;

/** RN03 - o farmaceutico declara que conferiu receita e documento. */
public record PedidoDeRetirada(boolean receitaConferida, boolean documentoConferido) { }
