package br.com.medshare.app.ui

/** Os endereços internos do app, em um lugar só. */
object Destinos {
    const val LOGIN = "login"
    const val CADASTRO = "cadastro"
    const val INICIO = "inicio"
    const val NOVA_DOACAO = "nova-doacao"
    const val DETALHE_DA_DOACAO = "doacao/{codigo}"
    const val AGENDAMENTO = "agendamento/{codigo}"
    const val CADUNICO = "cadunico"
    const val NOVO_PEDIDO = "novo-pedido"
    const val RECEITA = "receita/{id}"
    const val NOTIFICACOES = "notificacoes"

    fun detalheDaDoacao(codigo: String) = "doacao/$codigo"
    fun agendamento(codigo: String) = "agendamento/$codigo"
    fun receita(id: Long) = "receita/$id"
}
