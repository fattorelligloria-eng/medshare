package br.com.medshare.seguranca.dto;

import java.util.List;

public record RespostaDeLogin(
        String tokenDeAcesso,
        String tokenDeRenovacao,
        long expiraEmSegundos,
        Long usuarioId,
        String nome,
        List<String> papeis
) { }
