package br.com.medshare.integracao;

/** Resposta normalizada da consulta de CEP. */
public record EnderecoConsultado(
        String cep,
        String logradouro,
        String bairro,
        String municipio,
        String uf
) { }
