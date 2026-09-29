package br.com.medshare.reserva.dto;

import br.com.medshare.reserva.Reserva;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * RN03 - o que o farmaceutico ve no balcao para conferir a retirada.
 *
 * Tem o nome do titular e o CPF parcialmente mascarado, para comparar com o
 * documento, e a receita anexada. Nada sobre quem doou (RN05).
 */
public record ConferenciaDaRetirada(
        String codigoRetirada,
        String status,
        OffsetDateTime expiraEm,
        String medicamento,
        String principioAtivo,
        String titular,
        String cpfDoTitular,
        String receitaFotoUrl,
        String receitaCrm,
        LocalDate receitaEmissao,
        LocalDate receitaValidade,
        boolean receitaValida
) {

    public static ConferenciaDaRetirada de(Reserva reserva) {
        var necessidade = reserva.getNecessidade();
        var beneficiario = necessidade.getBeneficiario();
        var medicamento = reserva.getDoacao().getMedicamento();
        var receita = necessidade.getReceita();
        return new ConferenciaDaRetirada(
                reserva.getCodigoRetirada(),
                reserva.getStatus().name(),
                reserva.getExpiraEm(),
                medicamento.getNomeComercial(),
                medicamento.getPrincipioAtivo(),
                beneficiario.getNome(),
                mascarar(beneficiario.getCpf()),
                receita == null ? null : receita.getFotoUrl(),
                receita == null ? null : "CRM-%s %s".formatted(receita.getUfCrm(), receita.getCrmMedico()),
                receita == null ? null : receita.getDataEmissao(),
                receita == null ? null : receita.getValidade(),
                receita != null && receita.estaValida());
    }

    /** "12345678901" vira "***.456.789-**": basta para conferir com o documento. */
    private static String mascarar(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return "—";
        }
        return "***.%s.%s-**".formatted(cpf.substring(3, 6), cpf.substring(6, 9));
    }
}
