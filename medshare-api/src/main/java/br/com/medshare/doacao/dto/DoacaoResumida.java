package br.com.medshare.doacao.dto;

import br.com.medshare.doacao.Agendamento;
import br.com.medshare.doacao.Doacao;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * O que o doador ve das proprias doacoes.
 *
 * RN05 - nao ha campo de beneficiario aqui, nem em lugar nenhum desta resposta.
 * O doador acompanha o percurso da caixa dele; quem recebeu nao e informacao
 * que lhe pertence.
 *
 * O agendamento entra aqui, e nao so no detalhe, porque a tela inicial precisa
 * dizer o que a pessoa tem que fazer a seguir sem ela ter que abrir doacao por
 * doacao para descobrir.
 */
public record DoacaoResumida(
        String codigo,
        String medicamento,
        String principioAtivo,
        String lote,
        LocalDate validade,
        String status,
        String pontoDeColeta,
        /** Quando entregar. Nulo enquanto nao houver agendamento. */
        OffsetDateTime agendadaPara,
        /** Endereco do ponto, para o botao de como chegar. Nulo sem agendamento. */
        String enderecoDoPonto,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm
) {

    public static DoacaoResumida de(Doacao doacao) {
        return de(doacao, null);
    }

    public static DoacaoResumida de(Doacao doacao, Agendamento agendamento) {
        return new DoacaoResumida(
                doacao.getCodigo(),
                doacao.getMedicamento().getNomeComercial(),
                doacao.getMedicamento().getPrincipioAtivo(),
                doacao.getLote(),
                doacao.getValidade(),
                doacao.getStatus().name(),
                doacao.getPontoDeColeta() == null ? null : doacao.getPontoDeColeta().getNome(),
                agendamento == null ? null : agendamento.getDataHora(),
                agendamento == null ? null : enderecoDe(agendamento),
                doacao.getCriadoEm(),
                doacao.getAtualizadoEm());
    }

    private static String enderecoDe(Agendamento agendamento) {
        var endereco = agendamento.getPontoDeColeta().getEndereco();
        return "%s, %s - %s".formatted(
                endereco.getLogradouro(), endereco.getNumero(), endereco.getBairro());
    }
}
