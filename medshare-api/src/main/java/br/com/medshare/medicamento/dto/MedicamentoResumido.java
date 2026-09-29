package br.com.medshare.medicamento.dto;

import br.com.medshare.medicamento.Medicamento;

import java.math.BigDecimal;

public record MedicamentoResumido(
        Long id,
        String nomeComercial,
        String principioAtivo,
        String apresentacao,
        String laboratorio,
        BigDecimal pmc,
        boolean altoCusto
) {

    public static MedicamentoResumido de(Medicamento medicamento) {
        return new MedicamentoResumido(
                medicamento.getId(),
                medicamento.getNomeComercial(),
                medicamento.getPrincipioAtivo(),
                medicamento.getApresentacao(),
                medicamento.getLaboratorio(),
                medicamento.getPmc(),
                Boolean.TRUE.equals(medicamento.getAltoCusto()));
    }
}
