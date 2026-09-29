package br.com.medshare.app.ui

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formatação de exibição.
 *
 * A API manda data como "2026-09-28" e data com hora em ISO, com fuso. Data
 * pura é só recortada; data com hora é convertida para o fuso do aparelho,
 * porque o servidor pode devolver em UTC ("...T17:00:00Z" são 14h em Brasília).
 */
object Formatos {

    private val DIA_E_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm")
    private val REAIS = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    /** "2026-09-28" vira "28/09/2026". Texto inesperado volta como veio. */
    fun data(iso: String?): String {
        if (iso == null) return "—"
        val partes = iso.take(10).split("-")
        return if (partes.size == 3) "${partes[2]}/${partes[1]}/${partes[0]}" else iso
    }

    /** "2026-09-28T17:30:00Z" vira "28/09/2026 às 14:30" num aparelho em Brasília. */
    fun dataComHora(iso: String?): String {
        if (iso == null) return "—"
        return runCatching {
            OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).format(DIA_E_HORA)
        }.getOrElse { data(iso) }
    }

    /**
     * Pelo BigDecimal do texto, e não por conta com Double: 1234.29 * 100 dá
     * 123428.99999 em ponto flutuante, e os centavos saíam errados.
     */
    fun reais(valor: Double): String =
        REAIS.format(BigDecimal(valor.toString()).setScale(2, RoundingMode.HALF_UP))

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
