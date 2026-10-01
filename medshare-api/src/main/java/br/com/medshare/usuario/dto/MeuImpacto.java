package br.com.medshare.usuario.dto;

import java.math.BigDecimal;

/**
 * O que as doacoes da pessoa viraram.
 *
 * RN05 - nenhum dado de quem recebeu. O numero de tratamentos e uma contagem de
 * caixas entregues, nao uma lista de pessoas.
 */
public record MeuImpacto(
        long caixasDoadas,
        long caixasEntregues,
        /** Soma do PMC das caixas entregues. Null quando nao houve nenhuma. */
        BigDecimal valorDosTratamentos
) { }
