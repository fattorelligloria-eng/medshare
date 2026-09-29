package br.com.medshare.reserva.dto;

import jakarta.validation.constraints.Pattern;

/**
 * RN03 - o farmaceutico declara que conferiu receita e documento.
 *
 * @param cpfDeQuemRetira UC07 A3 - vazio quando e o proprio titular; o CPF de
 *                        um procurador cadastrado quando e outra pessoa
 */
public record PedidoDeRetirada(
        boolean receitaConferida,
        boolean documentoConferido,
        @Pattern(regexp = "^$|\\d{11}", message = "informe os 11 dígitos do CPF") String cpfDeQuemRetira
) { }
