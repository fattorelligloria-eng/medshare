package br.com.medshare.app.ui

/**
 * Formatação de data sem biblioteca extra.
 *
 * A API manda data como "2026-09-28" e data com hora em ISO. Como o app só
 * exibe — nunca calcula com essas datas — recortar o texto resolve, e evita
 * arrastar uma dependência de datas para dentro do APK.
 */
object Formatos {

    /** "2026-09-28" vira "28/09/2026". Texto inesperado volta como veio. */
    fun data(iso: String?): String {
        if (iso == null) return "—"
        val partes = iso.take(10).split("-")
        return if (partes.size == 3) "${partes[2]}/${partes[1]}/${partes[0]}" else iso
    }

    /** "2026-09-28T14:30:00Z" vira "28/09/2026 às 14:30". */
    fun dataComHora(iso: String?): String {
        if (iso == null) return "—"
        val dia = data(iso)
        val hora = iso.substringAfter('T', "").take(5)
        return if (hora.length == 5) "$dia às $hora" else dia
    }

    fun reais(valor: Double): String {
        val inteiro = valor.toLong()
        val centavos = ((valor - inteiro) * 100).toLong().toString().padStart(2, '0')
        val comPontos = inteiro.toString()
            .reversed().chunked(3).joinToString(".").reversed()
        return "R$ $comPontos,$centavos"
    }

    /** Como cada status aparece para o usuário. */
    fun status(chave: String): String = when (chave) {
        "CADASTRADA" -> "Cadastrada"
        "EM_ANALISE_CENTRAL" -> "Em análise"
        "PRE_VALIDADA" -> "Pré-validada"
        "AGENDADA" -> "Agendada"
        "RECEBIDA" -> "Recebida"
        "VALIDADA" -> "Validada"
        "DISPONIVEL" -> "Disponível"
        "RESERVADA" -> "Reservada"
        "ENTREGUE" -> "Entregue"
        "RECUSADA" -> "Recusada"
        "CANCELADA" -> "Cancelada"
        "REJEITADA" -> "Rejeitada"
        "DESCARTADA" -> "Descartada"
        "ATIVA" -> "Ativa"
        "CONCLUIDA" -> "Concluída"
        "EXPIRADA" -> "Expirada"
        else -> chave
    }

    /**
     * O que o usuário precisa fazer, ou esperar, em cada estado.
     * Um rótulo sozinho não diz nada a quem está acompanhando a própria doação.
     */
    fun explicacaoDoStatus(chave: String): String = when (chave) {
        "CADASTRADA" -> "Conferindo a foto da embalagem."
        "EM_ANALISE_CENTRAL" -> "Nossa equipe está conferindo a foto. Resposta em até 48 horas."
        "PRE_VALIDADA" -> "Tudo certo! Escolha a farmácia e o horário da entrega."
        "AGENDADA" -> "Leve a caixa à farmácia no horário marcado."
        "RECEBIDA" -> "A farmácia recebeu. O farmacêutico vai conferir o lacre."
        "VALIDADA" -> "Conferida pelo farmacêutico."
        "DISPONIVEL" -> "Na prateleira, esperando quem precisa."
        "RESERVADA" -> "Alguém reservou e vai retirar em breve."
        "ENTREGUE" -> "Entregue a quem precisava. Obrigado por doar."
        "RECUSADA" -> "Não pôde entrar na rede."
        "CANCELADA" -> "O agendamento foi cancelado."
        "REJEITADA" -> "Não passou na conferência do farmacêutico."
        "DESCARTADA" -> "Saiu do estoque por causa da validade."
        else -> ""
    }

    fun statusEhProblema(chave: String) =
        chave in setOf("RECUSADA", "CANCELADA", "REJEITADA", "DESCARTADA", "EXPIRADA")

    fun statusEhConclusao(chave: String) = chave in setOf("ENTREGUE", "CONCLUIDA")
}
