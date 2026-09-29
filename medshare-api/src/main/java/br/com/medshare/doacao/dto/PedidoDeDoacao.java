package br.com.medshare.doacao.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * UC01 - cadastro de doacao.
 *
 * @param quantidade      caixas iguais, do mesmo lote (1 a 10); cada uma vira
 *                        uma doacao com codigo e historico proprios (RN06)
 * @param lacreDeclarado  RN01 - o doador confirma que a embalagem nunca foi aberta
 */
public record PedidoDeDoacao(
        @NotNull Long medicamentoId,
        @NotBlank @Size(max = 30) String lote,
        @NotNull @Future(message = "a validade precisa ser uma data futura") LocalDate validade,
        @NotBlank @Size(max = 500) String fotoUrl,
        @Min(1) @Max(10) Integer quantidade,
        boolean lacreDeclarado
) {

    public int quantidadeOuUm() {
        return quantidade == null ? 1 : quantidade;
    }
}
