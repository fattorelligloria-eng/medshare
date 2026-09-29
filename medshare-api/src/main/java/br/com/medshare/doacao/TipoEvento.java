package br.com.medshare.doacao;

/** Vocabulario do historico (RN06). Cada passo do ciclo vira um evento. */
public enum TipoEvento {
    CADASTRO,
    PRE_VALIDACAO,
    ENVIO_PARA_CENTRAL,
    DECISAO_DA_CENTRAL,
    NOVA_FOTO_SOLICITADA,
    NOVA_FOTO_ENVIADA,
    AGENDAMENTO,
    REAGENDAMENTO,
    RECEBIMENTO,
    CORRECAO_DE_DADOS,
    VALIDACAO,
    DISPONIBILIZACAO,
    TRANSFERENCIA,
    OFERTA,
    RESERVA,
    EXPIRACAO_DE_RESERVA,
    CANCELAMENTO_DE_RESERVA,
    ENTREGA,
    RECUSA,
    CANCELAMENTO,
    REJEICAO,
    DESCARTE
}
