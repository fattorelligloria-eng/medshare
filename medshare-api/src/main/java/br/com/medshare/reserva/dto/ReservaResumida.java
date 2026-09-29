package br.com.medshare.reserva.dto;

import br.com.medshare.reserva.Reserva;

import java.time.OffsetDateTime;

/**
 * O que o beneficiario ve da reserva.
 *
 * RN05 - tem o nome da farmacia e o codigo de retirada; nao tem, e nao pode
 * ter, nada sobre quem doou.
 */
public record ReservaResumida(
        String codigoRetirada,
        String medicamento,
        String apresentacao,
        String pontoDeColeta,
        String enderecoDoPonto,
        String horarioDoPonto,
        String status,
        OffsetDateTime expiraEm
) {

    public static ReservaResumida de(Reserva reserva) {
        var ponto = reserva.getDoacao().getPontoDeColeta();
        var medicamento = reserva.getDoacao().getMedicamento();
        return new ReservaResumida(
                reserva.getCodigoRetirada(),
                medicamento.getNomeComercial(),
                medicamento.getApresentacao(),
                ponto == null ? null : ponto.getNome(),
                ponto == null ? null : "%s, %s - %s".formatted(
                        ponto.getEndereco().getLogradouro(),
                        ponto.getEndereco().getNumero(),
                        ponto.getEndereco().getBairro()),
                ponto == null ? null : ponto.getHorario(),
                reserva.getStatus().name(),
                reserva.getExpiraEm());
    }
}
