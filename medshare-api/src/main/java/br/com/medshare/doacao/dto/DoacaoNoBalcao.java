package br.com.medshare.doacao.dto;

import br.com.medshare.doacao.Agendamento;
import br.com.medshare.doacao.Doacao;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * UC03 passo 2 - o que o farmaceutico ve de uma doacao no balcao: foto,
 * medicamento, lote e validade declarados, e o agendamento.
 *
 * O doador aparece so pelo nome abreviado, para o farmaceutico confirmar com
 * quem esta no balcao sem expor o nome completo na tela.
 */
public record DoacaoNoBalcao(
        String codigo,
        String medicamento,
        String principioAtivo,
        String apresentacao,
        String lote,
        LocalDate validade,
        String status,
        String fotoUrl,
        String doador,
        String codigoEntrega,
        OffsetDateTime agendadaPara,
        OffsetDateTime atualizadoEm
) {

    public static DoacaoNoBalcao de(Doacao doacao, Agendamento agendamento, String fotoAssinada) {
        return new DoacaoNoBalcao(
                doacao.getCodigo(),
                doacao.getMedicamento().getNomeComercial(),
                doacao.getMedicamento().getPrincipioAtivo(),
                doacao.getMedicamento().getApresentacao(),
                doacao.getLote(),
                doacao.getValidade(),
                doacao.getStatus().name(),
                fotoAssinada,
                doacao.getDoador().nomeAbreviado(),
                agendamento == null ? null : agendamento.getCodigoEntrega(),
                agendamento == null ? null : agendamento.getDataHora(),
                doacao.getAtualizadoEm());
    }
}
