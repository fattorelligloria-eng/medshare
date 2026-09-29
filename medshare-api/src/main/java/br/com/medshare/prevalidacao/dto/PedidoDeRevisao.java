package br.com.medshare.prevalidacao.dto;

import br.com.medshare.prevalidacao.RevisaoCentral;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** A justificativa vai para o historico, que guarda ate 500 caracteres por evento. */
public record PedidoDeRevisao(
        @NotNull RevisaoCentral.Decisao decisao,
        @NotBlank(message = "explique a decisão para ficar registrado")
        @Size(max = 400, message = "resuma a justificativa em até 400 caracteres") String justificativa
) { }
