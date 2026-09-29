package br.com.medshare.reserva.dto;

import br.com.medshare.reserva.Oferta;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * UC06 passo 1 - o que o beneficiario ve de uma oferta: medicamento,
 * farmacia, endereco, horario e prazo para aceitar. Nada sobre quem doou (RN05).
 */
public record OfertaResumida(
        Long id,
        String status,
        String medicamento,
        String apresentacao,
        LocalDate validade,
        String pontoDeColeta,
        String enderecoDoPonto,
        String horarioDoPonto,
        OffsetDateTime expiraEm
) {

    public static OfertaResumida de(Oferta oferta) {
        var doacao = oferta.getDoacao();
        var ponto = doacao.getPontoDeColeta();
        var endereco = ponto.getEndereco();
        return new OfertaResumida(
                oferta.getId(),
                oferta.getStatus().name(),
                doacao.getMedicamento().getNomeComercial(),
                doacao.getMedicamento().getApresentacao(),
                doacao.getValidade(),
                ponto.getNome(),
                "%s, %s - %s, %s".formatted(endereco.getLogradouro(), endereco.getNumero(),
                        endereco.getBairro(), endereco.getMunicipio().getNome()),
                ponto.getHorario(),
                oferta.getExpiraEm());
    }
}
