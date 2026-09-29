package br.com.medshare.reserva.dto;

import br.com.medshare.necessidade.Procurador;
import br.com.medshare.reserva.Reserva;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * RN03 / UC07 passo 2 - o que o farmaceutico ve no balcao para conferir a
 * retirada.
 *
 * Tem o nome do titular e o CPF parcialmente mascarado, os procuradores
 * cadastrados (A3), a receita anexada (link com prazo) e a validade da caixa
 * (RN02). Nada sobre quem doou (RN05).
 */
public record ConferenciaDaRetirada(
        String codigoRetirada,
        String status,
        OffsetDateTime expiraEm,
        String medicamento,
        String principioAtivo,
        String apresentacao,
        LocalDate validadeDaCaixa,
        String titular,
        String cpfDoTitular,
        List<ProcuradorResumido> procuradores,
        String receitaFotoUrl,
        String receitaCrm,
        LocalDate receitaEmissao,
        LocalDate receitaValidade,
        boolean receitaValida
) {

    public record ProcuradorResumido(String nome, String cpf) { }

    public static ConferenciaDaRetirada de(Reserva reserva, String receitaAssinada,
                                           List<Procurador> procuradores) {
        var necessidade = reserva.getNecessidade();
        var beneficiario = necessidade.getBeneficiario();
        var doacao = reserva.getDoacao();
        var medicamento = doacao.getMedicamento();
        var receita = necessidade.getReceita();
        return new ConferenciaDaRetirada(
                reserva.getCodigoRetirada(),
                reserva.getStatus().name(),
                reserva.getExpiraEm(),
                medicamento.getNomeComercial(),
                medicamento.getPrincipioAtivo(),
                medicamento.getApresentacao(),
                doacao.getValidade(),
                beneficiario.getNome(),
                mascarar(beneficiario.getCpf()),
                procuradores.stream()
                        .map(p -> new ProcuradorResumido(p.getNome(), p.getCpf()))
                        .toList(),
                receitaAssinada,
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
