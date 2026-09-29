package br.com.medshare.farmacia.dto;

import br.com.medshare.farmacia.PontoDeColeta;

public record PontoDeColetaResumido(
        Long id,
        String nome,
        String endereco,
        String bairro,
        String municipio,
        String horario,
        Double latitude,
        Double longitude
) {

    public static PontoDeColetaResumido de(PontoDeColeta ponto) {
        var endereco = ponto.getEndereco();
        return new PontoDeColetaResumido(
                ponto.getId(),
                ponto.getNome(),
                "%s, %s".formatted(endereco.getLogradouro(), endereco.getNumero()),
                endereco.getBairro(),
                endereco.getMunicipio().getNome(),
                ponto.getHorario(),
                endereco.getLatitude(),
                endereco.getLongitude());
    }
}
