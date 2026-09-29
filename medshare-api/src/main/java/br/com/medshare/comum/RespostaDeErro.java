package br.com.medshare.comum;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Formato unico de erro da API. Quem consome sempre recebe a mesma forma,
 * seja um erro de validacao de campo ou uma regra de negocio violada.
 */
public record RespostaDeErro(
        OffsetDateTime momento,
        int status,
        String erro,
        String mensagem,
        String regra,
        List<CampoInvalido> campos
) {

    public record CampoInvalido(String campo, String problema) { }

    public static RespostaDeErro de(int status, String erro, String mensagem) {
        return new RespostaDeErro(OffsetDateTime.now(), status, erro, mensagem, null, null);
    }

    public static RespostaDeErro deRegra(int status, String erro, String mensagem, String regra) {
        return new RespostaDeErro(OffsetDateTime.now(), status, erro, mensagem, regra, null);
    }

    public static RespostaDeErro deCampos(int status, String erro, List<CampoInvalido> campos) {
        return new RespostaDeErro(OffsetDateTime.now(), status, erro,
                "Há campos inválidos na requisição", null, campos);
    }
}
