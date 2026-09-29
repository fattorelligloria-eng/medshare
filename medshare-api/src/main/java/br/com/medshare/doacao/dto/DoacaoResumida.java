package br.com.medshare.doacao.dto;

import br.com.medshare.doacao.Doacao;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * O que o doador ve das proprias doacoes.
 *
 * RN05 - nao ha campo de beneficiario aqui, nem em lugar nenhum desta resposta.
 * O doador acompanha o percurso da caixa dele; quem recebeu nao e informacao
 * que lhe pertence.
 */
public record DoacaoResumida(
        String codigo,
        String medicamento,
        String principioAtivo,
        String lote,
        LocalDate validade,
        String status,
        String pontoDeColeta,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm
) {

    public static DoacaoResumida de(Doacao doacao) {
        return new DoacaoResumida(
                doacao.getCodigo(),
                doacao.getMedicamento().getNomeComercial(),
                doacao.getMedicamento().getPrincipioAtivo(),
                doacao.getLote(),
                doacao.getValidade(),
                doacao.getStatus().name(),
                doacao.getPontoDeColeta() == null ? null : doacao.getPontoDeColeta().getNome(),
                doacao.getCriadoEm(),
                doacao.getAtualizadoEm());
    }
}
