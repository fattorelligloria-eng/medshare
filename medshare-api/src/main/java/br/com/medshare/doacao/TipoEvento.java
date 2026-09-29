package br.com.medshare.doacao;

/** Vocabulario do historico (RN06). Cada passo do ciclo vira um evento. */
public enum TipoEvento {
    CADASTRO,
    PRE_VALIDACAO,
    ENVIO_PARA_CENTRAL,
    DECISAO_DA_CENTRAL,
    AGENDAMENTO,
    RECEBIMENTO,
    VALIDACAO,
    DISPONIBILIZACAO,
    RESERVA,
    EXPIRACAO_DE_RESERVA,
    CANCELAMENTO_DE_RESERVA,
    ENTREGA,
    RECUSA,
    CANCELAMENTO,
    REJEICAO,
    DESCARTE
}
