package br.com.medshare.doacao.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * UC03 - aprovacao no balcao. Tudo opcional: a foto da conferencia (exemplo
 * para a fase 2) e a correcao de lote/validade quando a caixa real diverge
 * do que o doador digitou (A1).
 */
public record PedidoDeValidacao(
        @Size(max = 500) String fotoUrl,
        @Size(max = 30) String lote,
        LocalDate validade
) { }
