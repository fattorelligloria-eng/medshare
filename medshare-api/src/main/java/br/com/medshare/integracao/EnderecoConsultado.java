package br.com.medshare.integracao;

/** Resposta normalizada da consulta de CEP (ViaCEP). */
public record EnderecoConsultado(
        String cep,
        String logradouro,
        String bairro,
        String municipio,
        String uf,
        /** Codigo IBGE do municipio, 7 digitos: e por ele que a RN09 confere a regiao. */
        String codigoIbge
) {

    public EnderecoConsultado(String cep, String logradouro, String bairro, String municipio, String uf) {
        this(cep, logradouro, bairro, municipio, uf, null);
    }
}
