package br.com.medshare.farmacia.dto;

import br.com.medshare.farmacia.PontoDeColeta;

import java.time.OffsetDateTime;

/**
 * A farmacia como quem doa a ve.
 *
 * {@code abertaAgora} e {@code vagasProximosDias} existem para a tela conseguir
 * responder "posso ir ali?" sem a pessoa ter que escolher a farmacia para
 * depois descobrir que nao da. Sem eles, a lista oferece cinco farmacias e
 * duas delas sao becos.
 *
 * {@code vagasProximosDias} e a contagem de horarios livres nas proximas duas
 * semanas, ja considerando dia de funcionamento e vagas por hora. Zero quer
 * dizer que nao adianta ir ate la.
 */
public record PontoDeColetaResumido(
        Long id,
        String nome,
        String endereco,
        String bairro,
        String municipio,
        String horario,
        Double latitude,
        Double longitude,
        boolean abertaAgora,
        Integer vagasProximosDias
) {

    public static PontoDeColetaResumido de(PontoDeColeta ponto) {
        return de(ponto, null);
    }

    public static PontoDeColetaResumido de(PontoDeColeta ponto, Integer vagas) {
        var endereco = ponto.getEndereco();
        return new PontoDeColetaResumido(
                ponto.getId(),
                ponto.getNome(),
                "%s, %s".formatted(endereco.getLogradouro(), endereco.getNumero()),
                endereco.getBairro(),
                endereco.getMunicipio().getNome(),
                ponto.getHorario(),
                endereco.getLatitude(),
                endereco.getLongitude(),
                ponto.funcionaEm(OffsetDateTime.now()),
                vagas);
    }
}
