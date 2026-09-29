package br.com.medshare.prevalidacao.dto;

import br.com.medshare.prevalidacao.RevisaoCentral;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PedidoDeRevisao(
        @NotNull RevisaoCentral.Decisao decisao,
        @NotBlank(message = "explique a decisão para ficar registrado") String justificativa
) { }
